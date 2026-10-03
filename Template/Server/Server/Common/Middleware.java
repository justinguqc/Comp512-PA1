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
        final List<Booking> bookings = new ArrayList<>();
    }

    private static final class Booking {
        final IInventoryManager manager;
        final String key;
        final String location;
        final String id = UUID.randomUUID().toString();
        ReservationReceipt receipt;

        Booking(IInventoryManager manager, String key, String location) {
            this.manager = manager;
            this.key = key;
            this.location = location;
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

    public String queryCustomerInfo(int id) {
        Account account = accounts.get(id);
        if (account == null) return "";
        synchronized (account) {
            if (!account.exists) return "";
            Customer customer = new Customer(id);
            for (Booking booking : account.bookings) {
                for (int i = 0; i < booking.receipt.getQuantity(); i++)
                    customer.reserve(booking.key, booking.location, booking.receipt.getPrice());
            }
            return customer.getBill();
        }
    }

    private boolean reserve(int id, Booking booking) throws RemoteException {
        Account account = accounts.get(id);
        if (account == null) return false;
        synchronized (account) {
            if (!account.exists) return false;
            booking.receipt = booking.manager.reserveInventory(booking.key, 1, booking.id);
            if (booking.receipt == null) return false;
            account.bookings.add(booking);
            return true;
        }
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
            Iterator<Booking> bookings = account.bookings.iterator();
            while (bookings.hasNext()) {
                Booking booking = bookings.next();
                if (!booking.manager.releaseInventory(booking.id))
                    throw new RemoteException("Unable to release reservation " + booking.id);
                // Remove only confirmed releases; an interrupted deletion can safely resume.
                bookings.remove();
            }
            account.exists = false;
            return true;
        }
    }
    public boolean bundle(int id, Vector<String> numbers, String location, boolean car, boolean room) throws RemoteException {
        return false;
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
