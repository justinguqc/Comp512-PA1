package Server.Interface;

import java.rmi.RemoteException;
import Server.Common.ReservationReceipt;

/** Stage 2: internal backend contract; the client's IResourceManager stays unchanged. */
public interface IInventoryManager extends IResourceManager {
    /** Atomically acquire quantity and its price, or return null if unavailable/cancelled.
     * Replaying an active booking ID returns its receipt without consuming more inventory.
     * IDs cannot be reused with different arguments; all records live only until restart.
     */
    ReservationReceipt reserveInventory(String key, int quantity, String bookingId) throws RemoteException;

    /** Idempotently cancel a booking. An unknown ID gets a cancellation tombstone so a
     * delayed reserve cannot execute afterwards. Replaying a confirmed release returns true.
     */
    boolean releaseInventory(String bookingId) throws RemoteException;
}
