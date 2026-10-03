package Tests;

import Server.Common.ResourceManager;
import Server.Common.ReservationReceipt;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/** Stage 2: inventory operations never require a backend customer record. */
public final class InventoryTest {
    public static void main(String[] args) throws Exception {
        ResourceManager rm = new ResourceManager("Flights");
        rm.addFlight(512, 1, 120);
        ReservationReceipt receipt = rm.reserveInventory("flight-512", 1, "booking-1");
        StarterTest.check(receipt != null && receipt.getPrice() == 120, "reserve returns booked price");
        StarterTest.check(rm.queryFlight(512) == 0, "reserve consumes inventory without a customer");
        StarterTest.check(rm.reserveInventory("flight-512", 1, "booking-1") != null,
                "replaying a booking returns its original result");
        StarterTest.check(rm.releaseInventory("booking-1"), "release restores reservation");
        StarterTest.check(rm.releaseInventory("booking-1"), "replaying release is safe");
        StarterTest.check(rm.queryFlight(512) == 1, "release restores exactly one seat");
        rm.releaseInventory("cancelled-before-arrival");
        StarterTest.check(rm.reserveInventory("flight-512", 1, "cancelled-before-arrival") == null,
                "late reserve cannot undo a cancellation");
        ExecutorService pool = Executors.newFixedThreadPool(12);
        CountDownLatch ready = new CountDownLatch(12);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int i = 0; i < 12; i++) {
                final String id = "contender-" + i;
                results.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return rm.reserveInventory("flight-512", 1, id) != null;
                }));
            }
            StarterTest.check(ready.await(5, TimeUnit.SECONDS), "contenders ready");
            start.countDown();
            int successes = 0;
            for (Future<Boolean> result : results) if (result.get(5, TimeUnit.SECONDS)) successes++;
            StarterTest.check(successes == 1 && rm.queryFlight(512) == 0,
                    "only one concurrent caller acquires the last seat");
        } finally { start.countDown(); pool.shutdownNow(); }
        StarterTest.check(!rm.addFlight(512, -1, 120), "negative additions cannot corrupt availability");
        System.out.println("PASS InventoryTest");
    }
}
