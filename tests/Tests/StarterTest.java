package Tests;

import Server.Common.ResourceManager;

/** Stage 1: assertions observe the same public service contract used by the console. */
public final class StarterTest {
    public static void main(String[] args) throws Exception {
        ResourceManager rm = new ResourceManager("Resources");
        rm.addFlight(512, 2, 100);
        rm.addFlight(512, 1, 0);
        check(rm.queryFlight(512) == 3, "duplicate additions increase availability");
        check(rm.queryFlightPrice(512) == 100, "zero update preserves price");
        rm.newCustomer(7);
        check(rm.reserveFlight(7, 512), "customer can reserve a flight");
        check(!rm.deleteFlight(512), "reserved flight cannot be deleted");
        check(rm.queryCustomerInfo(7).contains("Total cost: $100"), "bill includes total");
        check(Client.Client.toBoolean("1") && Client.Client.toBoolean("Y"),
                "documented bundle flags enable bookings");
        check(!Client.Client.toBoolean("0") && !Client.Client.toBoolean("N"),
                "documented negative flags disable bookings");
        System.out.println("PASS StarterTest");
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
