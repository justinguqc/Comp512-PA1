package Tests;

import Server.Common.*;
import Server.Interface.IResourceManager;
import Server.TCP.*;

public final class TcpMiddlewareTest {
    public static void main(String[] args) throws Exception {
        ResourceManager flightState = new ResourceManager("Flights");
        try (TcpServer flights = TcpServer.inventory(0, flightState);
             TcpServer cars = TcpServer.inventory(0, new ResourceManager("Cars"));
             TcpServer rooms = TcpServer.inventory(0, new ResourceManager("Rooms"));
             TcpChannel f = new TcpChannel("localhost", flights.port(), 5000);
             TcpChannel c = new TcpChannel("localhost", cars.port(), 5000);
             TcpChannel r = new TcpChannel("localhost", rooms.port(), 5000);
             TcpServer middleware = new TcpServer(0, new AsyncMiddleware("Middleware", f, c, r), false);
             TcpChannel channel = new TcpChannel("localhost", middleware.port(), 5000)) {
            IResourceManager client = Services.blocking(IResourceManager.class, channel);
            client.addFlight(512, 3, 100); client.addCars("Montreal", 1, 30); client.addRooms("Montreal", 1, 80);
            client.newCustomer(7);
            StarterTest.check(client.bundle(7, BundleTest.flights("512", "512"), "Montreal", true, true)
                    && client.queryCustomerInfo(7).contains("Total cost: $310"), "TCP bundle uses both socket layers");
            StarterTest.check(!client.bundle(7, BundleTest.flights("512"), "Montreal", true, false)
                    && client.queryFlight(512) == 1, "TCP sold-out bundle is compensated");
            StarterTest.check(flightState.queryCustomerInfo(7).isEmpty(), "TCP backend has no customer copy");
            StarterTest.check(client.deleteCustomer(7) && client.queryFlight(512) == 3,
                    "TCP deletion restores every quantity");
        }
        System.out.println("PASS TcpMiddlewareTest");
    }
}
