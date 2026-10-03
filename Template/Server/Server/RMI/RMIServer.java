package Server.RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/** Stage 3: common RMI lifecycle for inventory and middleware endpoints. */
public final class RMIServer implements AutoCloseable {
    private final Remote service;
    private final Registry registry;
    private final String binding;
    private final boolean ownsRegistry;

    private RMIServer(Remote service, Registry registry, String binding, boolean ownsRegistry) {
        this.service = service;
        this.registry = registry;
        this.binding = binding;
        this.ownsRegistry = ownsRegistry;
    }

    public static RMIServer bind(Remote service, String name, int registryPort, String prefix, int objectPort)
            throws RemoteException {
        if (registryPort < 1 || registryPort > 65535 || objectPort < 0 || objectPort > 65535)
            throw new IllegalArgumentException("Invalid RMI port");
        Registry registry;
        boolean owned;
        try {
            registry = LocateRegistry.createRegistry(registryPort);
            owned = true;
        } catch (RemoteException busy) {
            registry = LocateRegistry.getRegistry(registryPort);
            registry.list();
            owned = false;
        }
        try {
            Remote stub = UnicastRemoteObject.exportObject(service, objectPort);
            registry.rebind(prefix + name, stub);
            return new RMIServer(service, registry, prefix + name, owned);
        } catch (RemoteException failure) {
            try { UnicastRemoteObject.unexportObject(service, true); } catch (Exception ignored) { }
            if (owned) try { UnicastRemoteObject.unexportObject(registry, true); } catch (Exception ignored) { }
            throw failure;
        }
    }

    public void installShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close, "rmi-shutdown-" + binding));
    }

    public void close() {
        try { registry.unbind(binding); } catch (Exception ignored) { }
        try { UnicastRemoteObject.unexportObject(service, true); } catch (Exception ignored) { }
        if (ownsRegistry) try { UnicastRemoteObject.unexportObject(registry, true); } catch (Exception ignored) { }
    }
}
