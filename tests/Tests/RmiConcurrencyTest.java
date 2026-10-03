package Tests;

import Server.Common.Middleware;
import Server.Common.ResourceManager;
import Server.Interface.IInventoryManager;
import Server.Interface.IResourceManager;
import java.lang.reflect.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;

/** RMI concurrency: independent customers progress; same-customer updates are not lost. */
public final class RmiConcurrencyTest {
    public static void main(String[] args) throws Exception {
        ResourceManager flights = new ResourceManager("Flights");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        IInventoryManager endpoint = (IInventoryManager) Proxy.newProxyInstance(
                IInventoryManager.class.getClassLoader(), new Class<?>[]{IInventoryManager.class}, (proxy, method, values) -> {
                    if (method.getName().equals("reserveInventory")) {
                        entered.countDown();
                        if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Delay was not released");
                    }
                    try { return method.invoke(flights, values); }
                    catch (InvocationTargetException error) { throw error.getCause(); }
                });
        IInventoryManager remoteFlights = (IInventoryManager) UnicastRemoteObject.exportObject(endpoint, 0);
        Middleware middleware = new Middleware("Middleware", remoteFlights, new ResourceManager("Cars"), new ResourceManager("Rooms"));
        IResourceManager client = (IResourceManager) UnicastRemoteObject.exportObject(middleware, 0);
        ExecutorService pool = Executors.newFixedThreadPool(12);
        try {
            client.addFlight(512, 13, 10);
            client.addCars("Montreal", 1, 30);
            client.newCustomer(1);
            client.newCustomer(2);
            Future<Boolean> slow = pool.submit(() -> client.reserveFlight(1, 512));
            StarterTest.check(entered.await(5, TimeUnit.SECONDS), "slow backend request reached RMI boundary");
            Future<Boolean> fast = pool.submit(() -> client.reserveCar(2, "Montreal"));
            StarterTest.check(fast.get(5, TimeUnit.SECONDS) && !slow.isDone(),
                    "unrelated customer completes while Flights response is delayed");
            release.countDown();
            StarterTest.check(slow.get(5, TimeUnit.SECONDS), "delayed booking completes");

            client.newCustomer(7);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Boolean>> reservations = new ArrayList<>();
            for (int i = 0; i < 12; i++) reservations.add(pool.submit(() -> {
                start.await();
                return client.reserveFlight(7, 512);
            }));
            start.countDown();
            for (Future<Boolean> booking : reservations) StarterTest.check(booking.get(10, TimeUnit.SECONDS), "concurrent booking succeeds");
            StarterTest.check(client.queryFlight(512) == 0 && client.queryCustomerInfo(7).contains("12 flight-512 $10")
                    && client.queryCustomerInfo(7).contains("Total cost: $120"), "no lost updates for one customer");
            StarterTest.check(client.deleteCustomer(7) && client.queryFlight(512) == 12, "delete returns concurrent reservations");
            Set<Integer> ids = new HashSet<>();
            List<Future<Integer>> customers = new ArrayList<>();
            for (int i = 0; i < 100; i++) customers.add(pool.submit(() -> client.newCustomer()));
            for (Future<Integer> result : customers) ids.add(result.get(10, TimeUnit.SECONDS));
            StarterTest.check(ids.size() == 100 && !ids.contains(1) && !ids.contains(2), "generated IDs remain unique concurrently");
        } finally {
            release.countDown();
            pool.shutdownNow();
            UnicastRemoteObject.unexportObject(middleware, true);
            UnicastRemoteObject.unexportObject(endpoint, true);
        }
        System.out.println("PASS RmiConcurrencyTest");
    }
}
