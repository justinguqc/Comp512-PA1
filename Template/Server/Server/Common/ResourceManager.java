// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import Server.Interface.*;

import java.util.*;
import java.rmi.RemoteException;
import java.io.*;

public class ResourceManager implements IInventoryManager
{
	protected String m_name = "";
	protected RMHashMap m_data = new RMHashMap();
	private final Map<String, InventoryBooking> inventoryBookings = new HashMap<>();

	private static final class InventoryBooking {
		final String key;
		final int quantity;
		final ReservationReceipt receipt;
		boolean active;
		InventoryBooking(String key, int quantity, ReservationReceipt receipt) {
			this.key = key;
			this.quantity = quantity;
			this.receipt = receipt;
			this.active = receipt != null;
		}
	}

	// Stage 2: the service monitor covers complete state transitions, including CRUD.
	public synchronized ReservationReceipt reserveInventory(String key, int quantity, String bookingId) throws RemoteException
	{
		if (quantity <= 0 || bookingId == null || bookingId.isEmpty())
			throw new RemoteException("Invalid inventory reservation");
		InventoryBooking existing = inventoryBookings.get(bookingId);
		if (existing != null) {
			// A cancellation tombstone also rejects a delayed reserve arriving after release.
			if (existing.key == null) return null;
			if (!existing.key.equals(key) || existing.quantity != quantity)
				throw new RemoteException("Booking ID reused for different inventory");
			return existing.active ? existing.receipt : null;
		}
		RMItem value = readData(key);
		ReservableItem item = value instanceof ReservableItem ? (ReservableItem)value : null;
		if (item == null || item.getCount() < quantity) {
			inventoryBookings.put(bookingId, new InventoryBooking(key, quantity, null));
			return null;
		}
		item.setCount(item.getCount() - quantity);
		item.setReserved(item.getReserved() + quantity);
		writeData(key, item);
		ReservationReceipt receipt = new ReservationReceipt(key, quantity, item.getPrice());
		inventoryBookings.put(bookingId, new InventoryBooking(key, quantity, receipt));
		return receipt;
	}

	// Stage 2: token-based release is idempotent, including after an uncertain reserve reply.
	public synchronized boolean releaseInventory(String bookingId) throws RemoteException
	{
		if (bookingId == null || bookingId.isEmpty()) throw new RemoteException("Invalid booking ID");
		InventoryBooking booking = inventoryBookings.get(bookingId);
		if (booking == null) {
			inventoryBookings.put(bookingId, new InventoryBooking(null, 0, null));
			return true;
		}
		if (!booking.active) return true;
		ReservableItem item = (ReservableItem)readData(booking.key);
		if (item == null || item.getReserved() < booking.quantity)
			throw new RemoteException("Inventory reservation invariant violated");
		item.setCount(item.getCount() + booking.quantity);
		item.setReserved(item.getReserved() - booking.quantity);
		writeData(booking.key, item);
		booking.active = false;
		return true;
	}

	public ResourceManager(String p_name)
	{
		m_name = p_name;
	}

	// Reads a data item
	protected synchronized RMItem readData(String key)
	{
		synchronized(m_data) {
			RMItem item = m_data.get(key);
			if (item != null) {
				return (RMItem)item.clone();
			}
			return null;
		}
	}

	// Writes a data item
	protected synchronized void writeData(String key, RMItem value)
	{
		synchronized(m_data) {
			m_data.put(key, value);
		}
	}

	// Remove the item out of storage
	protected synchronized void removeData(String key)
	{
		synchronized(m_data) {
			m_data.remove(key);
		}
	}

	// Deletes the encar item
	protected synchronized boolean deleteItem(String key)
	{
		Trace.info("RM::deleteItem(" + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(key);
		// Check if there is such an item in the storage
		if (curObj == null)
		{
			Trace.warn("RM::deleteItem(" + key + ") failed--item doesn't exist");
			return false;
		}
		else
		{
			if (curObj.getReserved() == 0)
			{
				removeData(curObj.getKey());
				Trace.info("RM::deleteItem(" + key + ") item deleted");
				return true;
			}
			else
			{
				Trace.info("RM::deleteItem(" + key + ") item can't be deleted because some customers have reserved it");
				return false;
			}
		}
	}

	// Query the number of available seats/rooms/cars
	protected synchronized int queryNum(String key)
	{
		Trace.info("RM::queryNum(" + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(key);
		int value = 0;  
		if (curObj != null)
		{
			value = curObj.getCount();
		}
		Trace.info("RM::queryNum(" + key + ") returns count=" + value);
		return value;
	}    

	// Query the price of an item
	protected synchronized int queryPrice(String key)
	{
		Trace.info("RM::queryPrice(" + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(key);
		int value = 0; 
		if (curObj != null)
		{
			value = curObj.getPrice();
		}
		Trace.info("RM::queryPrice(" + key + ") returns cost=$" + value);
		return value;        
	}

	// Reserve an item
	protected synchronized boolean reserveItem(int customerID, String key, String location)
	{
		Trace.info("RM::reserveItem(customer=" + customerID + ", " + key + ", " + location + ") called" );        
		// Read customer object if it exists (and read lock it)
		Customer customer = (Customer)readData(Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("RM::reserveItem(" + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
			return false;
		} 

		// Check if the item is available
		ReservableItem item = (ReservableItem)readData(key);
		if (item == null)
		{
			Trace.warn("RM::reserveItem(" + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
			return false;
		}
		else if (item.getCount() == 0)
		{
			Trace.warn("RM::reserveItem(" + customerID + ", " + key + ", " + location + ") failed--No more items");
			return false;
		}
		else
		{            
			customer.reserve(key, location, item.getPrice());        
			writeData(customer.getKey(), customer);

			// Decrease the number of available items in the storage
			item.setCount(item.getCount() - 1);
			item.setReserved(item.getReserved() + 1);
			writeData(item.getKey(), item);

			Trace.info("RM::reserveItem(" + customerID + ", " + key + ", " + location + ") succeeded");
			return true;
		}        
	}

	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public synchronized boolean addFlight(int flightNum, int flightSeats, int flightPrice) throws RemoteException
	{
		if (flightSeats < 0) return false;
		Trace.info("RM::addFlight(" + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
		Flight curObj = (Flight)readData(Flight.getKey(flightNum));
		if (curObj == null)
		{
			// Doesn't exist yet, add it
			Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
			writeData(newObj.getKey(), newObj);
			Trace.info("RM::addFlight() created new flight " + flightNum + ", seats=" + flightSeats + ", price=$" + flightPrice);
		}
		else
		{
			// Add seats to existing flight and update the price if greater than zero
			curObj.setCount(curObj.getCount() + flightSeats);
			if (flightPrice > 0)
			{
				curObj.setPrice(flightPrice);
			}
			writeData(curObj.getKey(), curObj);
			Trace.info("RM::addFlight() modified existing flight " + flightNum + ", seats=" + curObj.getCount() + ", price=$" + flightPrice);
		}
		return true;
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current price
	public synchronized boolean addCars(String location, int count, int price) throws RemoteException
	{
		if (count < 0) return false;
		Trace.info("RM::addCars(" + location + ", " + count + ", $" + price + ") called");
		Car curObj = (Car)readData(Car.getKey(location));
		if (curObj == null)
		{
			// Car location doesn't exist yet, add it
			Car newObj = new Car(location, count, price);
			writeData(newObj.getKey(), newObj);
			Trace.info("RM::addCars() created new location " + location + ", count=" + count + ", price=$" + price);
		}
		else
		{
			// Add count to existing car location and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0)
			{
				curObj.setPrice(price);
			}
			writeData(curObj.getKey(), curObj);
			Trace.info("RM::addCars() modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
		}
		return true;
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public synchronized boolean addRooms(String location, int count, int price) throws RemoteException
	{
		if (count < 0) return false;
		Trace.info("RM::addRooms(" + location + ", " + count + ", $" + price + ") called");
		Room curObj = (Room)readData(Room.getKey(location));
		if (curObj == null)
		{
			// Room location doesn't exist yet, add it
			Room newObj = new Room(location, count, price);
			writeData(newObj.getKey(), newObj);
			Trace.info("RM::addRooms() created new room location " + location + ", count=" + count + ", price=$" + price);
		} else {
			// Add count to existing object and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0)
			{
				curObj.setPrice(price);
			}
			writeData(curObj.getKey(), curObj);
			Trace.info("RM::addRooms() modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
		}
		return true;
	}

	// Deletes flight
	public synchronized boolean deleteFlight(int flightNum) throws RemoteException
	{
		return deleteItem(Flight.getKey(flightNum));
	}

	// Delete cars at a location
	public synchronized boolean deleteCars(String location) throws RemoteException
	{
		return deleteItem(Car.getKey(location));
	}

	// Delete rooms at a location
	public synchronized boolean deleteRooms(String location) throws RemoteException
	{
		return deleteItem(Room.getKey(location));
	}

	// Returns the number of empty seats in this flight
	public synchronized int queryFlight(int flightNum) throws RemoteException
	{
		return queryNum(Flight.getKey(flightNum));
	}

	// Returns the number of cars available at a location
	public synchronized int queryCars(String location) throws RemoteException
	{
		return queryNum(Car.getKey(location));
	}

	// Returns the amount of rooms available at a location
	public synchronized int queryRooms(String location) throws RemoteException
	{
		return queryNum(Room.getKey(location));
	}

	// Returns price of a seat in this flight
	public synchronized int queryFlightPrice(int flightNum) throws RemoteException
	{
		return queryPrice(Flight.getKey(flightNum));
	}

	// Returns price of cars at this location
	public synchronized int queryCarsPrice(String location) throws RemoteException
	{
		return queryPrice(Car.getKey(location));
	}

	// Returns room price at this location
	public synchronized int queryRoomsPrice(String location) throws RemoteException
	{
		return queryPrice(Room.getKey(location));
	}

	public synchronized String queryCustomerInfo(int customerID) throws RemoteException
	{
		Trace.info("RM::queryCustomerInfo(" + customerID + ") called");
		Customer customer = (Customer)readData(Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("RM::queryCustomerInfo(" + customerID + ") failed--customer doesn't exist");
			// NOTE: don't change this--WC counts on this value indicating a customer does not exist...
			return "";
		}
		else
		{
			Trace.info("RM::queryCustomerInfo(" + customerID + ")");
			System.out.println(customer.getBill());
			return customer.getBill();
		}
	}

	public synchronized int newCustomer() throws RemoteException
	{
        	Trace.info("RM::newCustomer() called");
		// Generate a globally unique ID for the new customer; if it generates duplicates for you, then adjust
		int cid = Integer.parseInt(String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
			String.valueOf(Math.round(Math.random() * 100 + 1)));
		Customer customer = new Customer(cid);
		writeData(customer.getKey(), customer);
		Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
		return cid;
	}

	public synchronized boolean newCustomer(int customerID) throws RemoteException
	{
		Trace.info("RM::newCustomer(" + customerID + ") called");
		Customer customer = (Customer)readData(Customer.getKey(customerID));
		if (customer == null)
		{
			customer = new Customer(customerID);
			writeData(customer.getKey(), customer);
			Trace.info("RM::newCustomer(" + customerID + ") created a new customer");
			return true;
		}
		else
		{
			Trace.info("INFO: RM::newCustomer(" + customerID + ") failed--customer already exists");
			return false;
		}
	}

	public synchronized boolean deleteCustomer(int customerID) throws RemoteException
	{
		Trace.info("RM::deleteCustomer(" + customerID + ") called");
		Customer customer = (Customer)readData(Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("RM::deleteCustomer(" + customerID + ") failed--customer doesn't exist");
			return false;
		}
		else
		{            
			// Increase the reserved numbers of all reservable items which the customer reserved. 
 			RMHashMap reservations = customer.getReservations();
			for (String reservedKey : reservations.keySet())
			{        
				ReservedItem reserveditem = customer.getReservedItem(reservedKey);
				Trace.info("RM::deleteCustomer(" + customerID + ") has reserved " + reserveditem.getKey() + " " +  reserveditem.getCount() +  " times");
				ReservableItem item  = (ReservableItem)readData(reserveditem.getKey());
				Trace.info("RM::deleteCustomer(" + customerID + ") has reserved " + reserveditem.getKey() + " which is reserved " +  item.getReserved() +  " times and is still available " + item.getCount() + " times");
				item.setReserved(item.getReserved() - reserveditem.getCount());
				item.setCount(item.getCount() + reserveditem.getCount());
				writeData(item.getKey(), item);
			}

			// Remove the customer from the storage
			removeData(customer.getKey());
			Trace.info("RM::deleteCustomer(" + customerID + ") succeeded");
			return true;
		}
	}

	// Adds flight reservation to this customer
	public synchronized boolean reserveFlight(int customerID, int flightNum) throws RemoteException
	{
		return reserveItem(customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
	}

	// Adds car reservation to this customer
	public synchronized boolean reserveCar(int customerID, String location) throws RemoteException
	{
		return reserveItem(customerID, Car.getKey(location), location);
	}

	// Adds room reservation to this customer
    public synchronized boolean reserveRoom(int customerID, String location) throws RemoteException
	{
		return reserveItem(customerID, Room.getKey(location), location);
	}

	// Reserve bundle 
	public synchronized boolean bundle(int customerId, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException
	{
		return false;
	}

	public synchronized String getName() throws RemoteException
	{
		return m_name;
	}
}
 
