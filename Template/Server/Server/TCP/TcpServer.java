package Server.TCP;

import Server.Common.*;
import java.io.*;
import java.net.*;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Stage 6: independent accept/read/write loops; middleware dispatch never waits for replies. */
public final class TcpServer implements AutoCloseable {
    private final ServerSocket listener;
    private final AsyncService service;
    private final boolean inventory;
    private final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private final Semaphore permits = new Semaphore(64);
    private final Thread acceptor;
    private ExecutorService workers;

    public TcpServer(int port, AsyncService service, boolean inventory) throws IOException {
        this.service = service; this.inventory = inventory;
        listener = new ServerSocket(port);
        acceptor = Services.daemonFactory("tcp-accept").newThread(this::accept); acceptor.start();
    }
    public static TcpServer inventory(int port, ResourceManager manager) throws IOException {
        ThreadPoolExecutor workers = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(256), Services.daemonFactory("tcp-inventory"));
        try {
            TcpServer server = new TcpServer(port, (method, args) -> {
                try { return CompletableFuture.supplyAsync(() -> Services.call(manager, method, args), workers); }
                catch (RejectedExecutionException busy) { return CompletableFuture.failedFuture(busy); }
            }, true);
            server.workers = workers; return server;
        } catch (IOException failure) { workers.shutdownNow(); throw failure; }
    }
    public int port() { return listener.getLocalPort(); }
    private void accept() {
        try {
            while (!listener.isClosed()) {
                Socket socket = listener.accept();
                if (!permits.tryAcquire()) { socket.close(); continue; }
                try {
                    synchronized (this) {
                        if (listener.isClosed()) { socket.close(); permits.release(); continue; }
                        socket.setTcpNoDelay(true); Session session = new Session(socket); sessions.add(session); session.start();
                    }
                }
                catch (Exception failure) { socket.close(); permits.release(); }
            }
        } catch (IOException error) { if (!listener.isClosed()) close(); }
    }
    private final class Session {
        final Socket socket;
        final BlockingQueue<Protocol.Response> outgoing = new ArrayBlockingQueue<>(1024);
        final AtomicInteger inflight = new AtomicInteger();
        final Thread reader;
        final Thread writer;
        boolean closed;
        Session(Socket socket) {
            this.socket = socket;
            reader = Services.daemonFactory("tcp-session-read").newThread(this::read);
            writer = Services.daemonFactory("tcp-session-write").newThread(this::write);
        }
        void start() { reader.start(); writer.start(); }
        void read() {
            try {
                while (true) {
                    Object frame = Protocol.read(socket.getInputStream());
                    if (!(frame instanceof Protocol.Request request)) throw new IOException("Expected request");
                    if (inflight.incrementAndGet() > 1024) throw new IOException("Too many pending requests");
                    CompletionStage<Object> reply;
                    try { reply = service.invoke(Operations.resolve(request.operation(), request.arguments(), inventory), request.arguments()); }
                    catch (Exception invalid) { reply = CompletableFuture.failedFuture(invalid); }
                    reply.whenComplete((value, error) -> {
                        inflight.decrementAndGet();
                        String message = error == null ? null : Services.cause(error).toString();
                        if (!outgoing.offer(new Protocol.Response(request.id(), value, message))) close();
                    });
                }
            } catch (Exception error) { close(); }
        }
        void write() {
            try { while (true) Protocol.write(socket.getOutputStream(), outgoing.take()); }
            catch (Exception error) { close(); }
        }
        synchronized void close() {
            if (closed) return;
            closed = true;
            try { socket.close(); } catch (IOException ignored) { }
            reader.interrupt(); writer.interrupt(); outgoing.clear(); sessions.remove(this); permits.release();
        }
    }
    public synchronized void close() {
        try { listener.close(); } catch (IOException ignored) { }
        for (Session session : sessions) session.close();
        if (workers != null) workers.shutdownNow();
        acceptor.interrupt();
    }
}
