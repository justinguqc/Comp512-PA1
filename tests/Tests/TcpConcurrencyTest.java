package Tests;

import Server.Common.*;
import Server.Interface.IResourceManager;
import Server.TCP.*;
import java.util.*;
import java.util.concurrent.*;

/** More outstanding waits than the RMI adapter's worker count; TCP uses no such workers. */
public final class TcpConcurrencyTest {
    public static void main(String[] args) throws Exception {
        ResourceManager state = new ResourceManager("Flights");
        CompletableFuture<Void> gate = new CompletableFuture<>();
        CountDownLatch arrived = new CountDownLatch(40);
        AsyncService delayed = (method, values) -> {
            if (method.getName().equals("reserveInventory")) {
                arrived.countDown(); return gate.thenApply(ignored -> Services.call(state, method, values));
            }
            return CompletableFuture.completedFuture(Services.call(state, method, values));
        };
        try (TcpServer flights = new TcpServer(0, delayed, true);
             TcpServer cars = TcpServer.inventory(0, new ResourceManager("Cars"));
             TcpServer rooms = TcpServer.inventory(0, new ResourceManager("Rooms"));
             TcpChannel f = new TcpChannel("localhost", flights.port(), 15000);
             TcpChannel c = new TcpChannel("localhost", cars.port(), 15000);
             TcpChannel r = new TcpChannel("localhost", rooms.port(), 15000);
             TcpServer middleware = new TcpServer(0, new AsyncMiddleware("Middleware", f, c, r), false);
             TcpChannel channel = new TcpChannel("localhost", middleware.port(), 15000);
             TcpChannel other = new TcpChannel("localhost", middleware.port(), 15000)) {
            IResourceManager client = Services.blocking(IResourceManager.class, channel);
            client.addFlight(512, 52, 10); client.addCars("Montreal", 1, 30);
            List<CompletableFuture<Object>> slow = new ArrayList<>();
            for (int i = 1; i <= 40; i++) {
                client.newCustomer(i);
                slow.add(channel.invoke(Operations.method("reserveFlight", int.class, int.class), new Object[]{i, 512}).toCompletableFuture());
            }
            StarterTest.check(arrived.await(5, TimeUnit.SECONDS), "all 40 remote waits reached backend without worker starvation");
            StarterTest.check(other.invoke(Operations.method("queryCars", String.class), new Object[]{"Montreal"}).toCompletableFuture().get(5, TimeUnit.SECONDS).equals(1),
                    "another client's Cars query finishes while Flights replies remain held");
            StarterTest.check(channel.invoke(Operations.method("queryCars", String.class), new Object[]{"Montreal"}).toCompletableFuture().get(5, TimeUnit.SECONDS).equals(1)
                    && slow.stream().noneMatch(CompletableFuture::isDone), "same connection receives out-of-order replies by ID");
            gate.complete(null);
            for (var booking : slow) StarterTest.check(booking.get(5, TimeUnit.SECONDS).equals(true), "delayed reply reaches its caller");
            client.newCustomer(100);
            List<CompletableFuture<Object>> sameCustomer = new ArrayList<>();
            for (int i = 0; i < 12; i++) sameCustomer.add(channel.invoke(Operations.method("reserveFlight", int.class, int.class), new Object[]{100, 512}).toCompletableFuture());
            for (var booking : sameCustomer) StarterTest.check(booking.get(5, TimeUnit.SECONDS).equals(true), "ordered customer booking");
            StarterTest.check(client.queryFlight(512) == 0 && client.queryCustomerInfo(100).contains("Total cost: $120"), "customer queue loses no ledger entries");
            StarterTest.check(client.deleteCustomer(100) && client.queryFlight(512) == 12, "queued bookings are all released");
        } finally { gate.complete(null); }
        System.out.println("PASS TcpConcurrencyTest");
    }
}
