package Server.Interface;

import java.rmi.RemoteException;
import Server.Common.ReservationReceipt;

/** Stage 2: internal backend contract; the client's IResourceManager stays unchanged. */
public interface IInventoryManager extends IResourceManager {
    ReservationReceipt reserveInventory(String key, int quantity, String bookingId) throws RemoteException;
    boolean releaseInventory(String bookingId) throws RemoteException;
}
