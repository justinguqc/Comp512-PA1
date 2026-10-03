package Server.TCP;

import Server.Common.AsyncMiddleware;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/** Stage 7: TCP on both links, with the shared asynchronous customer service. */
public final class TCPMiddleware {
    private static TcpChannel connect(String endpoint, long timeout) throws Exception {
        int separator = endpoint.lastIndexOf(':');
        if (separator <= 0) throw new IllegalArgumentException("Expected backend host:port: " + endpoint);
        return new TcpChannel(endpoint.substring(0, separator), Integer.parseInt(endpoint.substring(separator + 1)), timeout);
    }
    public static void main(String[] args) {
        if (args.length < 4 || args.length > 5) {
            System.err.println("Usage: TCPMiddleware port flightsHost:port carsHost:port roomsHost:port [timeoutMillis]");
            System.exit(1);
        }
        List<TcpChannel> channels = new ArrayList<>();
        try {
            long timeout = args.length == 5 ? Long.parseLong(args[4]) : 30000;
            for (int i = 1; i <= 3; i++) channels.add(connect(args[i], timeout));
            AsyncMiddleware service = new AsyncMiddleware("Middleware", channels.get(0), channels.get(1), channels.get(2));
            try (TcpServer server = new TcpServer(Integer.parseInt(args[0]), service, false)) {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    server.close(); channels.forEach(TcpChannel::close);
                }));
                System.out.println("Middleware ready on TCP " + server.port());
                new CountDownLatch(1).await();
            }
        } catch (Exception failure) {
            System.err.println("TCP middleware startup failed: " + failure);
            channels.forEach(TcpChannel::close); System.exit(1);
        } finally { channels.forEach(TcpChannel::close); }
    }
}
