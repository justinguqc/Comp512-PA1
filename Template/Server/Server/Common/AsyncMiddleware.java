package Server.Common;

import Server.Interface.IInventoryManager;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.management.ManagementFactory;
import java.util.function.Supplier;

/** Stage 6: shared customer/routing logic. Remote waits are continuations, never get/join.
 * Each customer has an ordered future chain; no worker is occupied waiting for inventory. */
public final class AsyncMiddleware implements AsyncService {
    private static final String PROCESS = ManagementFactory.getRuntimeMXBean().getName() + "-" + ManagementFactory.getRuntimeMXBean().getStartTime();
    private static final AtomicLong BOOKING_IDS = new AtomicLong();
    private final String name;
    private final AsyncService flights, cars, rooms;
    private final Map<Integer, Account> accounts = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private static final class Account {
        volatile boolean exists;
        boolean deleting;
        int queued;
        CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);
        final List<Booking> bookings = new ArrayList<>();
        final List<Booking> pending = new ArrayList<>();
    }
    private static final class Booking {
        final AsyncService manager;
        final String key, location, id = PROCESS + "-" + BOOKING_IDS.incrementAndGet();
        final int quantity;
        ReservationReceipt receipt;
        Booking(AsyncService manager, String key, String location, int quantity) {
            this.manager = manager; this.key = key; this.location = location; this.quantity = quantity;
        }
    }
    public AsyncMiddleware(String name, AsyncService flights, AsyncService cars, AsyncService rooms) {
        this.name = Objects.requireNonNull(name); this.flights = Objects.requireNonNull(flights);
        this.cars = Objects.requireNonNull(cars); this.rooms = Objects.requireNonNull(rooms);
    }
    private static CompletionStage<Object> result(Object value) { return CompletableFuture.completedFuture(value); }
    private static CompletionStage<Object> failure(String message) { return CompletableFuture.failedFuture(new RemoteException(message)); }
    private CompletionStage<Object> queue(Account account, Supplier<CompletionStage<Object>> task) {
        synchronized (account) {
            if (account.queued >= 1024) return failure("Too many queued customer requests");
            account.queued++;
            CompletableFuture<Object> reply = account.tail.handle((v, e) -> null).thenCompose(ignored -> task.get()).toCompletableFuture();
            account.tail = reply.handle((v, e) -> { synchronized (account) { account.queued--; } return null; });
            return reply;
        }
    }
    public CompletionStage<Object> invoke(Method method, Object[] args) {
        String operation = method.getName();
        try {
            if (operation.equals("getName")) return result(name);
            if (operation.equals("newCustomer") && args.length == 0) return generatedCustomer();
            if (operation.equals("newCustomer")) {
                Account account = accounts.computeIfAbsent((Integer)args[0], ignored -> new Account());
                return queue(account, () -> { if (account.exists) return result(false); account.exists = true; return result(true); });
            }
            if (operation.equals("queryCustomerInfo") || operation.equals("deleteCustomer")
                    || operation.startsWith("reserve") || operation.equals("bundle")) {
                int id = (Integer)args[0]; Account account = accounts.get(id);
                if (account == null) return result(operation.equals("queryCustomerInfo") ? "" : false);
                List<Booking> trip = operation.startsWith("reserve") || operation.equals("bundle") ? trip(operation, args) : null;
                return queue(account, () -> {
                    if (!account.exists) return result(operation.equals("queryCustomerInfo") ? "" : false);
                    if (operation.equals("deleteCustomer")) return delete(account);
                    if (account.deleting) return failure("Customer deletion incomplete; retry deleteCustomer");
                    if (operation.equals("queryCustomerInfo")) return result(bill(id, account));
                    if (trip == null) return result(false);
                    return acquire(account, trip);
                });
            }
            AsyncService manager = operation.contains("Flight") ? flights : operation.contains("Cars") ? cars
                    : operation.contains("Rooms") ? rooms : null;
            if (manager == null) return failure("Unsupported middleware operation");
            return manager.invoke(method, args);
        } catch (Exception error) { return CompletableFuture.failedFuture(error); }
    }
    private CompletionStage<Object> generatedCustomer() {
        int id;
        do { id = nextId.getAndUpdate(value -> value == Integer.MAX_VALUE ? 1 : value + 1); }
        while (accounts.containsKey(id) && accounts.get(id).exists);
        final int selected = id;
        return invoke(method("newCustomer", int.class), new Object[]{id}).thenCompose(created ->
                Boolean.TRUE.equals(created) ? result(selected) : generatedCustomer());
    }
    private String bill(int id, Account account) {
        Customer customer = new Customer(id);
        for (Booking booking : account.bookings)
            for (int i = 0; i < booking.quantity; i++) customer.reserve(booking.key, booking.location, booking.receipt.getPrice());
        return customer.getBill();
    }
    private List<Booking> trip(String operation, Object[] args) {
        List<Booking> trip = new ArrayList<>();
        if (operation.equals("reserveFlight")) trip.add(new Booking(flights, Flight.getKey((Integer)args[1]), args[1].toString(), 1));
        else if (operation.equals("reserveCar")) trip.add(new Booking(cars, Car.getKey((String)args[1]), (String)args[1], 1));
        else if (operation.equals("reserveRoom")) trip.add(new Booking(rooms, Room.getKey((String)args[1]), (String)args[1], 1));
        else if (operation.equals("bundle")) {
            if (!(args[1] instanceof Vector<?> numbers) || numbers.isEmpty()) return null;
            Map<Integer, Integer> counts = new LinkedHashMap<>();
            try { for (Object number : numbers) counts.merge(Integer.parseInt((String)number), 1, Integer::sum); }
            catch (RuntimeException invalid) { return null; }
            counts.forEach((number, quantity) -> trip.add(new Booking(flights, Flight.getKey(number), number.toString(), quantity)));
            String location = (String)args[2];
            if ((Boolean)args[3]) trip.add(new Booking(cars, Car.getKey(location), location, 1));
            if ((Boolean)args[4]) trip.add(new Booking(rooms, Room.getKey(location), location, 1));
        } else return null;
        return trip;
    }
    private CompletionStage<Object> acquire(Account account, List<Booking> trip) {
        return cleanup(account).thenCompose(ignored -> {
            List<Booking> attempted = new ArrayList<>();
            CompletionStage<Boolean> chain = CompletableFuture.completedFuture(true);
            for (Booking booking : trip) chain = chain.thenCompose(available -> {
                if (!available) return CompletableFuture.completedFuture(false);
                attempted.add(booking);
                return call(booking.manager, "reserveInventory", new Class<?>[]{String.class, int.class, String.class},
                        booking.key, booking.quantity, booking.id).thenApply(receipt -> {
                    booking.receipt = (ReservationReceipt)receipt; return receipt != null;
                });
            });
            return chain.handle((available, error) -> {
                if (error == null && Boolean.TRUE.equals(available)) {
                    account.bookings.addAll(trip); return result(true);
                }
                account.pending.addAll(attempted);
                return cleanup(account).handle((v, cleanupError) -> {
                    if (error != null) {
                        Throwable cause = Services.cause(error);
                        if (cleanupError != null) cause.addSuppressed(Services.cause(cleanupError));
                        throw new CompletionException(new RemoteException("Reservation failed; cleanup attempted and unconfirmed releases retained", cause));
                    }
                    if (cleanupError != null) throw new CompletionException(Services.cause(cleanupError));
                    return (Object)false;
                });
            }).thenCompose(stage -> stage);
        });
    }
    private CompletionStage<Void> cleanup(Account account) {
        CompletionStage<Void> chain = CompletableFuture.completedFuture(null);
        List<Throwable> failures = new ArrayList<>();
        for (Booking booking : new ArrayList<>(account.pending)) chain = chain.thenCompose(ignored ->
                release(booking).handle((v, error) -> {
                    if (error == null) account.pending.remove(booking); else failures.add(Services.cause(error));
                    return (Void)null;
                }));
        return chain.thenApply(ignored -> {
            if (!failures.isEmpty()) {
                RemoteException error = new RemoteException("Incomplete cleanup; retry after backend recovery", failures.get(0));
                for (int i = 1; i < failures.size(); i++) error.addSuppressed(failures.get(i));
                throw new CompletionException(error);
            }
            return null;
        });
    }
    private CompletionStage<Object> delete(Account account) {
        account.deleting = true;
        CompletionStage<Void> chain = cleanup(account);
        for (Booking booking : new ArrayList<>(account.bookings)) chain = chain.thenCompose(ignored ->
                release(booking).thenAccept(value -> account.bookings.remove(booking)));
        return chain.thenApply(ignored -> { account.exists = false; account.deleting = false; return (Object)true; });
    }
    private CompletionStage<Object> release(Booking booking) {
        return call(booking.manager, "releaseInventory", new Class<?>[]{String.class}, booking.id).thenApply(value -> {
            if (!Boolean.TRUE.equals(value)) throw new CompletionException(new RemoteException("Unable to release reservation"));
            return value;
        });
    }
    private static Method method(String name, Class<?>... types) {
        try { return IInventoryManager.class.getMethod(name, types); }
        catch (NoSuchMethodException error) { throw new IllegalArgumentException(error); }
    }
    private static CompletionStage<Object> call(AsyncService service, String name, Class<?>[] types, Object... args) {
        try { return service.invoke(method(name, types), args); }
        catch (Exception error) { return CompletableFuture.failedFuture(error); }
    }
}
