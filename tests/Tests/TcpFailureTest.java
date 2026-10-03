package Tests;

import Server.Common.*;
import Server.Interface.IResourceManager;
import Server.TCP.*;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TcpFailureTest {
    public static void main(String[] args) throws Exception {
        AtomicBoolean loseReply = new AtomicBoolean(true), failRelease = new AtomicBoolean(true);
        ResourceManager state = new ResourceManager("Flights");
        AsyncService faulty = (method, args1) -> {
            if (method.getName().equals("releaseInventory") && failRelease.getAndSet(false))
                return CompletableFuture.failedFuture(new IOException("Cancellation unavailable"));
            Object value = Services.call(state, method, args1);
            if (method.getName().equals("reserveInventory") && loseReply.getAndSet(false))
                return CompletableFuture.failedFuture(new IOException("Reserve reply lost after mutation"));
            return CompletableFuture.completedFuture(value);
        };
        try (TcpServer flights = new TcpServer(0, faulty, true);
             TcpServer cars = TcpServer.inventory(0, new ResourceManager("Cars"));
             TcpServer rooms = TcpServer.inventory(0, new ResourceManager("Rooms"));
             TcpChannel f = new TcpChannel("localhost", flights.port(), 5000);
             TcpChannel c = new TcpChannel("localhost", cars.port(), 5000);
             TcpChannel r = new TcpChannel("localhost", rooms.port(), 5000);
             TcpServer middleware = new TcpServer(0, new AsyncMiddleware("Middleware", f, c, r), false);
             TcpChannel channel = new TcpChannel("localhost", middleware.port(), 5000)) {
            IResourceManager client = Services.blocking(IResourceManager.class, channel);
            client.addFlight(512, 1, 100); client.newCustomer(7);
            RmiFailureTest.expectRemoteFailure(() -> client.reserveFlight(7, 512));
            StarterTest.check(client.queryFlight(512) == 0, "unconfirmed cleanup remains held");
            StarterTest.check(client.reserveFlight(7, 512) && client.deleteCustomer(7) && client.queryFlight(512) == 1,
                    "TCP errors retain compensation and allow safe retry");
            try (Socket raw = new Socket("localhost", middleware.port())) {
                raw.setSoTimeout(5000);
                Protocol.write(raw.getOutputStream(), new Protocol.Request(1, "arbitraryMethod()", new Object[0]));
                StarterTest.check(((Protocol.Response)Protocol.read(raw.getInputStream())).error() != null, "unknown operation returns explicit error");
                Protocol.write(raw.getOutputStream(), new Protocol.Request(2, "reserveInventory(java.lang.String,int,java.lang.String)", new Object[]{"flight-512", 1, "bypass"}));
                StarterTest.check(((Protocol.Response)Protocol.read(raw.getInputStream())).error() != null, "middleware rejects backend-only operations");
            }
        }
        CompletableFuture<Object> held = new CompletableFuture<>();
        try (TcpServer server = new TcpServer(0, (method, args1) -> held, false);
             TcpChannel channel = new TcpChannel("localhost", server.port(), 200)) {
            try { channel.invoke(Operations.method("getName"), new Object[0]).toCompletableFuture().get(3, TimeUnit.SECONDS);
                throw new AssertionError("Missing response did not time out");
            } catch (ExecutionException expected) { }
        }
        try (TcpServer server = new TcpServer(0, (method, args1) -> held, false);
             TcpChannel channel = new TcpChannel("localhost", server.port(), 5000)) {
            var pending = channel.invoke(Operations.method("getName"), new Object[0]).toCompletableFuture();
            server.close();
            try { pending.get(3, TimeUnit.SECONDS); throw new AssertionError("Disconnected call remained pending"); }
            catch (ExecutionException expected) { }
        }
        held.complete("late");
        System.out.println("PASS TcpFailureTest");
    }
}
