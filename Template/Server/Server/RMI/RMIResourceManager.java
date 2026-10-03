// Adapted from Kevin T. Manley, CSE 593.
package Server.RMI;

import Server.Common.ResourceManager;

/** Stage 3: configurable RMI inventory host sharing the common inventory implementation. */
public class RMIResourceManager extends ResourceManager {
    public RMIResourceManager(String name) { super(name); }

    public static void main(String[] args) {
        if (args.length > 4) {
            System.err.println("Usage: RMIResourceManager [name [registry-port [prefix [object-port]]]]");
            System.exit(1);
        }
        try {
            String name = args.length > 0 ? args[0] : "Server";
            int port = args.length > 1 ? Integer.parseInt(args[1]) : 1099;
            String prefix = args.length > 2 ? args[2] : "group_xx_";
            int objectPort = args.length > 3 ? Integer.parseInt(args[3]) : 0;
            RMIServer host = RMIServer.bind(new RMIResourceManager(name), name, port, prefix, objectPort);
            host.installShutdownHook();
            System.out.println(name + " ready on registry " + port + ", bound to " + prefix + name);
        } catch (Exception failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }
}
