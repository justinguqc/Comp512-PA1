package Server.Common;

import Server.Interface.IInventoryManager;
import Server.Interface.IResourceManager;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Stage 3: transport-independent routing and exclusive customer ownership. */
public class Middleware implements IResourceManager {
    private final String name;
    private final IInventoryManager flights;
    private final IInventoryManager cars;
    private final IInventoryManager rooms;
    private final Map<Integer, Account> accounts = new ConcurrentHashMap<>();
    private final AtomicInteger nextCustomerId = new AtomicInteger(1);

    private static final class Account {
        boolean exists;
        boolean deleting;
        final List<Booking> bookings = new ArrayList<>();
        final List<Booking> pendingRelease = new ArrayList<>();
    }

    private static final class Booking {
        final IInventoryManager manager;
        final String key;
        final String location;
        final int quantity;
        final String id = UUID.randomUUID().toString();
        ReservationReceipt receipt;

        Booking(IInventoryManager manager, String key, String location) {
            this(manager, key, location, 1);
        }

        Booking(IInventoryManager manager, String key, String location, int quantity) {
            this.manager = manager;
            this.key = key;
            this.location = location;
            this.quantity = quantity;
        }
    }

    public Middleware(String name, IInventoryManager flights, IInventoryManager cars, IInventoryManager rooms) {
        this.name = Objects.requireNonNull(name);
        this.flights = Objects.requireNonNull(flights);
        this.cars = Objects.requireNonNull(cars);
        this.rooms = Objects.requireNonNull(rooms);
    }

    // Account monitors are stable even after deletion/recreation of a customer ID.
    // Only that customer's operations wait across RMI; other customers and CRUD proceed.
    public boolean newCustomer(int id) {
        Account account = accounts.computeIfAbsent(id, ignored -> new Account());
        synchronized (account) {
            if (account.exists) return false;
            account.exists = true;
            return true;
        }
    }

    public int newCustomer() {
        while (true) {
            int id = nextCustomerId.getAndUpdate(value -> value == Integer.MAX_VALUE ? 1 : value + 1);
            if (newCustomer(id)) return id;
        }
    }

    public String queryCustomerInfo(int id) throws RemoteException {
        Account account = accounts.get(id);
        if (account == null) return "";
        synchronized (account) {
            if (!account.exists) return "";
            if (account.deleting) throw new RemoteException("Customer deletion incomplete; retry deleteCustomer");
            Customer customer = new Customer(id);
            for (Booking booking : account.bookings) {
                for (int i = 0; i < booking.receipt.getQuantity(); i++)
                    customer.reserve(booking.key, booking.location, booking.receipt.getPrice());
            }
            return customer.getBill();
        }
    }

    private boolean reserve(int id, Booking booking) throws RemoteException {
        return acquire(id, Collections.singletonList(booking));
    }

    // Stage 4: track the token before sending so a lost reply can still be cancelled.
    private boolean acquire(int id, List<Booking> trip) throws RemoteException {
        Account account = accounts.get(id);
        if (account == null) return false;
        synchronized (account) {
            if (!account.exists) return false;
            if (account.deleting) throw new RemoteException("Customer deletion incomplete; retry deleteCustomer");
            releasePending(account);
            List<Booking> attempted = new ArrayList<>();
            boolean available = true;
            try {
                for (Booking booking : trip) {
                    attempted.add(booking);
                    booking.receipt = booking.manager.reserveInventory(booking.key, booking.quantity, booking.id);
                    if (booking.receipt == null) { available = false; break; }
                }
            } catch (RemoteException failure) {
                account.pendingRelease.addAll(attempted);
                try { releasePending(account); }
                catch (RemoteException compensation) { failure.addSuppressed(compensation); }
                throw new RemoteException("Reservation failed; incomplete cleanup is retained for retry", failure);
            }
            if (!available) {
                account.pendingRelease.addAll(attempted);
                releasePending(account);
                return false;
            }
            account.bookings.addAll(trip);
            return true;
        }
    }

    private void releasePending(Account account) throws RemoteException {
        RemoteException failure = null;
        Iterator<Booking> pending = account.pendingRelease.iterator();
        while (pending.hasNext()) {
            Booking booking = pending.next();
            try {
                if (!booking.manager.releaseInventory(booking.id))
                    throw new RemoteException("Unable to cancel reservation " + booking.id);
                pending.remove();
            } catch (RemoteException error) {
                if (failure == null) failure = error;
                else failure.addSuppressed(error);
            }
        }
        if (failure != null)
            throw new RemoteException("Incomplete reservation cleanup; retry this customer operation after backend recovery", failure);
    }

    public boolean reserveFlight(int id, int flight) throws RemoteException {
        return reserve(id, new Booking(flights, Flight.getKey(flight), String.valueOf(flight)));
    }
    public boolean reserveCar(int id, String location) throws RemoteException {
        return reserve(id, new Booking(cars, Car.getKey(location), location));
    }
    public boolean reserveRoom(int id, String location) throws RemoteException {
        return reserve(id, new Booking(rooms, Room.getKey(location), location));
    }
    public boolean deleteCustomer(int id) throws RemoteException {
        Account account = accounts.get(id);
        if (account == null) return false;
        synchronized (account) {
            if (!account.exists) return false;
            account.deleting = true;
            releasePending(account);
            Iterator<Booking> bookings = account.bookings.iterator();
            while (bookings.hasNext()) {
                Booking booking = bookings.next();
                if (!booking.manager.releaseInventory(booking.id))
                    throw new RemoteException("Unable to release reservation " + booking.id);
                // Remove only confirmed releases; an interrupted deletion can safely resume.
                bookings.remove();
            }
            account.exists = false;
            account.deleting = false;
            return true;
        }
    }
    public boolean bundle(int id, Vector<String> numbers, String location, boolean car, boolean room) throws RemoteException {
        if (numbers == null || numbers.isEmpty()) return false;
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        try {
            for (String number : numbers) counts.merge(Integer.parseInt(number), 1, Integer::sum);
        } catch (NumberFormatException invalid) { return false; }
        List<Booking> trip = new ArrayList<>();
        for (Map.Entry<Integer, Integer> flight : counts.entrySet())
            trip.add(new Booking(flights, Flight.getKey(flight.getKey()), String.valueOf(flight.getKey()), flight.getValue()));
        if (car) trip.add(new Booking(cars, Car.getKey(location), location));
        if (room) trip.add(new Booking(rooms, Room.getKey(location), location));
        // The same acquisition/compensation path handles individual bookings and bundles.
        return acquire(id, trip);
    }

    // Stage 3: routing preserves every public client signature and return value.
    public boolean addFlight(int number, int count, int price) throws RemoteException { return flights.addFlight(number, count, price); }
    public boolean addCars(String location, int count, int price) throws RemoteException { return cars.addCars(location, count, price); }
    public boolean addRooms(String location, int count, int price) throws RemoteException { return rooms.addRooms(location, count, price); }
    public boolean deleteFlight(int number) throws RemoteException { return flights.deleteFlight(number); }
    public boolean deleteCars(String location) throws RemoteException { return cars.deleteCars(location); }
    public boolean deleteRooms(String location) throws RemoteException { return rooms.deleteRooms(location); }
    public int queryFlight(int number) throws RemoteException { return flights.queryFlight(number); }
    public int queryCars(String location) throws RemoteException { return cars.queryCars(location); }
    public int queryRooms(String location) throws RemoteException { return rooms.queryRooms(location); }
    public int queryFlightPrice(int number) throws RemoteException { return flights.queryFlightPrice(number); }
    public int queryCarsPrice(String location) throws RemoteException { return cars.queryCarsPrice(location); }
    public int queryRoomsPrice(String location) throws RemoteException { return rooms.queryRoomsPrice(location); }
    public String getName() { return name; }
}
