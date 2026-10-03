package Tests;

import Server.Common.Middleware;
import Server.Common.ResourceManager;
import Server.Interface.IInventoryManager;
import java.lang.reflect.*;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.atomic.AtomicBoolean;

/** Stage 4: inject lost replies at a real RMI boundary, after the backend has mutated. */
public final class RmiFailureTest {
    public static void main(String[] args) throws Exception {
        ResourceManager backend = new ResourceManager("Flights");
        AtomicBoolean loseReserve = new AtomicBoolean(true);
        AtomicBoolean loseRelease = new AtomicBoolean(false);
        IInventoryManager endpoint = (IInventoryManager) Proxy.newProxyInstance(
                IInventoryManager.class.getClassLoader(), new Class<?>[]{IInventoryManager.class}, (proxy, method, values) -> {
                    Object result;
                    try { result = method.invoke(backend, values); }
                    catch (InvocationTargetException error) { throw error.getCause(); }
                    if (method.getName().equals("reserveInventory") && loseReserve.getAndSet(false))
                        throw new RemoteException("Injected lost reserve reply");
                    if (method.getName().equals("releaseInventory") && loseRelease.getAndSet(false))
                        throw new RemoteException("Injected lost release reply");
                    return result;
                });
        IInventoryManager stub = (IInventoryManager) UnicastRemoteObject.exportObject(endpoint, 0);
        try {
            Middleware m = new Middleware("Middleware", stub, new ResourceManager("Cars"), new ResourceManager("Rooms"));
            m.addFlight(512, 1, 100);
            m.newCustomer(7);
            expectRemoteFailure(() -> m.reserveFlight(7, 512));
            StarterTest.check(m.queryFlight(512) == 1 && m.queryCustomerInfo(7).contains("Total cost: $0"),
                    "lost reserve reply is compensated without publishing a customer booking");
            StarterTest.check(m.reserveFlight(7, 512), "customer can book after compensation");
            loseRelease.set(true);
            expectRemoteFailure(() -> m.deleteCustomer(7));
            expectRemoteFailure(() -> m.reserveFlight(7, 512));
            StarterTest.check(m.deleteCustomer(7) && m.queryFlight(512) == 1,
                    "resumed deletion cannot double-release after a lost cancellation reply");
        } finally { UnicastRemoteObject.unexportObject((Remote) endpoint, true); }
        System.out.println("PASS RmiFailureTest");
    }

    interface RemoteCall { boolean run() throws RemoteException; }
    static void expectRemoteFailure(RemoteCall operation) throws RemoteException {
        try { operation.run(); }
        catch (RemoteException expected) { return; }
        throw new AssertionError("Expected a reported remote failure");
    }
}
