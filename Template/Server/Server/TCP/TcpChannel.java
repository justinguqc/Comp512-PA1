package Server.TCP;

import Server.Common.AsyncService;
import Server.Common.Services;
import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.rmi.RemoteException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Stage 6: multiplexed async connection. invoke queues a frame and returns immediately. */
public final class TcpChannel implements AsyncService, AutoCloseable {
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(Services.daemonFactory("tcp-timeouts"));
    private final Socket socket = new Socket();
    private final long timeout;
    private final AtomicLong ids = new AtomicLong();
    private final ConcurrentMap<Long, CompletableFuture<Object>> pending = new ConcurrentHashMap<>();
    private final BlockingQueue<Protocol.Request> outgoing = new ArrayBlockingQueue<>(1024);
    private final Thread reader;
    private final Thread writer;
    private boolean closed;

    public TcpChannel(String host, int port, long timeoutMillis) throws IOException {
        if (timeoutMillis < 1 || timeoutMillis > Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid timeout");
        timeout = timeoutMillis;
        try { socket.connect(new InetSocketAddress(host, port), (int)timeoutMillis); socket.setTcpNoDelay(true); }
        catch (IOException error) { socket.close(); throw error; }
        reader = Services.daemonFactory("tcp-replies").newThread(this::read);
        writer = Services.daemonFactory("tcp-requests").newThread(this::write);
        reader.start(); writer.start();
    }

    public synchronized CompletionStage<Object> invoke(Method method, Object[] arguments) {
        if (closed) return CompletableFuture.failedFuture(new RemoteException("TCP channel closed"));
        if (pending.size() >= 1024) return CompletableFuture.failedFuture(new RemoteException("TCP channel overloaded"));
        long id = ids.incrementAndGet(); CompletableFuture<Object> future = new CompletableFuture<>();
        pending.put(id, future);
        if (!outgoing.offer(new Protocol.Request(id, Operations.key(method), arguments.clone()))) {
            pending.remove(id); future.completeExceptionally(new RemoteException("TCP writer overloaded")); return future;
        }
        ScheduledFuture<?> deadline = TIMER.schedule(() -> fail(new SocketTimeoutException("TCP reply timed out; mutation outcome may be uncertain")), timeout, TimeUnit.MILLISECONDS);
        future.whenComplete((value, error) -> deadline.cancel(false));
        return future;
    }
    private void read() {
        try {
            while (true) {
                Object frame = Protocol.read(socket.getInputStream());
                if (!(frame instanceof Protocol.Response response)) throw new IOException("Expected response");
                CompletableFuture<Object> future = pending.remove(response.id());
                if (future == null) throw new IOException("Unexpected response ID");
                if (response.error() == null) future.complete(response.value());
                else future.completeExceptionally(new RemoteException(response.error()));
            }
        } catch (Exception error) { fail(error); }
    }
    private void write() {
        try { while (true) Protocol.write(socket.getOutputStream(), outgoing.take()); }
        catch (Exception error) { fail(error); }
    }
    private synchronized void fail(Throwable error) {
        if (closed) return;
        closed = true;
        try { socket.close(); } catch (IOException ignored) { }
        reader.interrupt(); writer.interrupt(); outgoing.clear();
        pending.forEach((id, future) -> future.completeExceptionally(new RemoteException("TCP connection failed", error)));
        pending.clear();
    }
    public void close() { fail(new IOException("TCP channel closed")); }
}
