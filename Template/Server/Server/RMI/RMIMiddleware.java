package Server.RMI;

import Server.Common.Middleware;
import Server.Interface.IInventoryManager;
import java.rmi.registry.LocateRegistry;

/** Stage 3: RMI-specific startup only; customer and reservation logic lives in Common. */
public final class RMIMiddleware extends Middleware {
    public RMIMiddleware(String name, IInventoryManager flights, IInventoryManager cars, IInventoryManager rooms) {
        super(name, flights, cars, rooms);
    }

    private static IInventoryManager lookup(String endpoint, String name, String prefix) throws Exception {
        String[] parts = endpoint.split(":", -1);
        if (parts.length > 2 || parts[0].isEmpty())
            throw new IllegalArgumentException("Expected backend hostname[:registry-port]");
        int port = parts.length == 2 ? Integer.parseInt(parts[1]) : 1099;
        return (IInventoryManager) LocateRegistry.getRegistry(parts[0], port).lookup(prefix + name);
    }

    public static RMIMiddleware connect(String name, String flights, String cars, String rooms, String prefix)
            throws Exception {
        return new RMIMiddleware(name, lookup(flights, "Flights", prefix), lookup(cars, "Cars", prefix),
                lookup(rooms, "Rooms", prefix));
    }

    public static void main(String[] args) {
        if (args.length < 3 || args.length > 6) {
            System.err.println("Usage: RMIMiddleware Flights-host[:port] Cars-host[:port] Rooms-host[:port] [registry-port [prefix [object-port]]]");
            System.exit(1);
        }
        try {
            int port = args.length > 3 ? Integer.parseInt(args[3]) : 1099;
            String prefix = args.length > 4 ? args[4] : "group_xx_";
            int objectPort = args.length > 5 ? Integer.parseInt(args[5]) : 0;
            RMIMiddleware middleware = connect("Middleware", args[0], args[1], args[2], prefix);
            RMIServer host = RMIServer.bind(middleware, "Middleware", port, prefix, objectPort);
            host.installShutdownHook();
            System.out.println("Middleware ready on registry " + port + ", bound to " + prefix + "Middleware");
        } catch (Exception failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }
}
