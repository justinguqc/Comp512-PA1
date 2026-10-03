package Tests;

import Server.Common.ResourceManager;
import Server.Interface.IResourceManager;
import Server.RMI.RMIMiddleware;
import Server.RMI.RMIServer;
import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;

/** Stage 3: lookups and all service calls use RMI stubs and loopback network endpoints. */
public final class RmiIntegrationTest {
    public static void main(String[] args) throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        String prefix = "test_";
        try (RMIServer f = RMIServer.bind(new ResourceManager("Flights"), "Flights", port, prefix, 0);
             RMIServer c = RMIServer.bind(new ResourceManager("Cars"), "Cars", port, prefix, 0);
             RMIServer r = RMIServer.bind(new ResourceManager("Rooms"), "Rooms", port, prefix, 0)) {
            String endpoint = "localhost:" + port;
            RMIMiddleware service = RMIMiddleware.connect("Middleware", endpoint, endpoint, endpoint, prefix);
            try (RMIServer m = RMIServer.bind(service, "Middleware", port, prefix, 0)) {
                IResourceManager client = (IResourceManager) LocateRegistry.getRegistry("localhost", port)
                        .lookup(prefix + "Middleware");
                client.addFlight(512, 1, 100);
                client.addCars("Montreal", 1, 30);
                client.addRooms("Montreal", 1, 80);
                int customer = client.newCustomer();
                StarterTest.check(client.reserveFlight(customer, 512) && client.reserveCar(customer, "Montreal")
                        && client.reserveRoom(customer, "Montreal"), "RMI reservations cross both layers");
                StarterTest.check(client.queryCustomerInfo(customer).contains("Total cost: $210"), "RMI bill");
                StarterTest.check(client.deleteCustomer(customer) && client.queryFlight(512) == 1,
                        "RMI deletion restores inventory");
            }
        }
        System.out.println("PASS RmiIntegrationTest");
    }
}
