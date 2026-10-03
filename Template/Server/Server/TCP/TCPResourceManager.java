package Server.TCP;

import Server.Common.ResourceManager;
import java.util.concurrent.CountDownLatch;

/** Stage 7: standalone inventory role; only the process main thread waits for shutdown. */
public final class TCPResourceManager {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: TCPResourceManager name port"); System.exit(1);
        }
        try (TcpServer server = TcpServer.inventory(Integer.parseInt(args[1]), new ResourceManager(args[0]))) {
            Runtime.getRuntime().addShutdownHook(new Thread(server::close));
            System.out.println(args[0] + " ready on TCP " + server.port());
            new CountDownLatch(1).await();
        } catch (Exception failure) {
            System.err.println("TCP inventory startup failed: " + failure); System.exit(1);
        }
    }
}
