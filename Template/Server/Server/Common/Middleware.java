package Server.Common;

import Server.Interface.IInventoryManager;
import Server.Interface.IResourceManager;
import java.rmi.RemoteException;
import java.util.Vector;

/** Stage 6: RMI synchronous facade over the same async business service used by TCP.
 * The accepted pre-TCP RMI solution is preserved at Git tag rmi-complete. */
public class Middleware implements IResourceManager {
    private final IResourceManager service;
    public Middleware(String name, IInventoryManager flights, IInventoryManager cars, IInventoryManager rooms) {
        service = Services.blocking(IResourceManager.class, new AsyncMiddleware(name,
                Services.remote(flights), Services.remote(cars), Services.remote(rooms)));
    }
    @Override
    public boolean addFlight(int flightNum, int flightSeats, int flightPrice) throws RemoteException {
        return service.addFlight(flightNum, flightSeats, flightPrice);
    }
    @Override
    public boolean addCars(String location, int numCars, int price) throws RemoteException {
        return service.addCars(location, numCars, price);
    }
    @Override
    public boolean addRooms(String location, int numRooms, int price) throws RemoteException {
        return service.addRooms(location, numRooms, price);
    }
    @Override
    public int newCustomer() throws RemoteException {
        return service.newCustomer();
    }
    @Override
    public boolean newCustomer(int cid) throws RemoteException {
        return service.newCustomer(cid);
    }
    @Override
    public boolean deleteFlight(int flightNum) throws RemoteException {
        return service.deleteFlight(flightNum);
    }
    @Override
    public boolean deleteCars(String location) throws RemoteException {
        return service.deleteCars(location);
    }
    @Override
    public boolean deleteRooms(String location) throws RemoteException {
        return service.deleteRooms(location);
    }
    @Override
    public boolean deleteCustomer(int customerID) throws RemoteException {
        return service.deleteCustomer(customerID);
    }
    @Override
    public int queryFlight(int flightNumber) throws RemoteException {
        return service.queryFlight(flightNumber);
    }
    @Override
    public int queryCars(String location) throws RemoteException {
        return service.queryCars(location);
    }
    @Override
    public int queryRooms(String location) throws RemoteException {
        return service.queryRooms(location);
    }
    @Override
    public String queryCustomerInfo(int customerID) throws RemoteException {
        return service.queryCustomerInfo(customerID);
    }
    @Override
    public int queryFlightPrice(int flightNumber) throws RemoteException {
        return service.queryFlightPrice(flightNumber);
    }
    @Override
    public int queryCarsPrice(String location) throws RemoteException {
        return service.queryCarsPrice(location);
    }
    @Override
    public int queryRoomsPrice(String location) throws RemoteException {
        return service.queryRoomsPrice(location);
    }
    @Override
    public boolean reserveFlight(int customerID, int flightNumber) throws RemoteException {
        return service.reserveFlight(customerID, flightNumber);
    }
    @Override
    public boolean reserveCar(int customerID, String location) throws RemoteException {
        return service.reserveCar(customerID, location);
    }
    @Override
    public boolean reserveRoom(int customerID, String location) throws RemoteException {
        return service.reserveRoom(customerID, location);
    }
    @Override
    public boolean bundle(int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException {
        return service.bundle(customerID, flightNumbers, location, car, room);
    }
    @Override
    public String getName() throws RemoteException {
        return service.getName();
    }
}
