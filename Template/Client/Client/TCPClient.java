package Client;

import Server.Common.Services;
import Server.Interface.IResourceManager;
import Server.TCP.TcpChannel;
import java.io.IOException;

/** Stage 7: the console blocks through the general service proxy, one command at a time. */
public final class TCPClient extends Client implements AutoCloseable {
    private final String host;
    private final int port;
    private final long timeout;
    private TcpChannel channel;

    public TCPClient(String host, int port, long timeout) {
        this.host = host; this.port = port; this.timeout = timeout;
    }
    public void connectServer() {
        close();
        try {
            channel = new TcpChannel(host, port, timeout);
            // Every interface method shares invocation, framing, correlation, and error handling.
            m_resourceManager = Services.blocking(IResourceManager.class, channel);
            System.out.println("Connected to TCP middleware " + host + ":" + port);
        } catch (IOException failure) { throw new IllegalStateException("TCP connection failed", failure); }
    }
    public void close() { if (channel != null) channel.close(); }
    public static void main(String[] args) {
        if (args.length > 3) {
            System.err.println("Usage: TCPClient [host [port [timeoutMillis]]]"); System.exit(1);
        }
        try (TCPClient client = new TCPClient(args.length > 0 ? args[0] : "localhost",
                args.length > 1 ? Integer.parseInt(args[1]) : 4004,
                args.length > 2 ? Long.parseLong(args[2]) : 30000)) {
            Runtime.getRuntime().addShutdownHook(new Thread(client::close));
            client.connectServer(); client.start();
        } catch (Exception failure) {
            System.err.println("TCP client failed: " + failure); System.exit(1);
        }
    }
}
