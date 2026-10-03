package Tests;

import Server.Common.Middleware;
import Server.Common.ResourceManager;
import java.util.Arrays;
import java.util.Vector;

/** Stage 4: a trip publishes its customer ledger only when every component is acquired. */
public final class BundleTest {
    public static void main(String[] args) throws Exception {
        Middleware m = new Middleware("Middleware", new ResourceManager("Flights"),
                new ResourceManager("Cars"), new ResourceManager("Rooms"));
        m.newCustomer(7);
        m.addFlight(512, 3, 100);
        m.addFlight(513, 2, 150);
        m.addCars("Montreal", 1, 30);
        m.addRooms("Montreal", 1, 80);
        StarterTest.check(m.bundle(7, flights("512", "512", "513"), "Montreal", true, true),
                "bundle books repeated flights, car, and room");
        StarterTest.check(m.queryFlight(512) == 1 && m.queryFlight(513) == 1 && m.queryCars("Montreal") == 0
                && m.queryRooms("Montreal") == 0 && m.queryCustomerInfo(7).contains("Total cost: $460"),
                "bundle accounts for every quantity and booked price");
        String bill = m.queryCustomerInfo(7);
        StarterTest.check(!m.bundle(7, flights("512", "513"), "Montreal", true, false),
                "sold-out car rejects the trip");
        StarterTest.check(m.queryFlight(512) == 1 && m.queryFlight(513) == 1
                && m.queryCustomerInfo(7).equals(bill), "failed trip preserves inventory and earlier reservations");
        StarterTest.check(!m.bundle(7, flights("512", "bad-number"), "Montreal", false, false)
                && !m.bundle(7, flights(), "Montreal", false, false)
                && !m.bundle(999, flights("512"), "Montreal", false, false),
                "invalid trip or missing customer is rejected before mutation");
        StarterTest.check(!m.bundle(7, flights("512", "512"), "Montreal", false, false)
                && m.queryFlight(512) == 1, "repeated flights require their full quantity");
        StarterTest.check(m.bundle(7, flights("512"), "Montreal", false, false), "flights-only bundle");
        StarterTest.check(m.deleteCustomer(7) && m.queryFlight(512) == 3 && m.queryFlight(513) == 2
                && m.queryCars("Montreal") == 1 && m.queryRooms("Montreal") == 1,
                "deleting bundled customer returns every quantity");
        System.out.println("PASS BundleTest");
    }

    static Vector<String> flights(String... numbers) { return new Vector<>(Arrays.asList(numbers)); }
}
