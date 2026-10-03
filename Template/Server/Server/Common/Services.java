package Server.Common;

import java.lang.reflect.*;
import java.rmi.RemoteException;
import java.util.concurrent.*;

/** Stage 6: synchronous facades adapt the shared async service only at RMI/client boundaries. */
public final class Services {
    private static final ExecutorService RMI = Executors.newFixedThreadPool(16, daemonFactory("rmi-adapter"));
    private Services() { }
    public static ThreadFactory daemonFactory(String name) {
        return task -> { Thread thread = new Thread(task, name); thread.setDaemon(true); return thread; };
    }
    public static Object call(Object service, Method method, Object[] arguments) {
        try { return method.invoke(service, arguments); }
        catch (InvocationTargetException error) { throw new CompletionException(error.getCause()); }
        catch (Exception error) { throw new CompletionException(error); }
    }
    public static AsyncService remote(Object service) {
        return (method, arguments) -> CompletableFuture.supplyAsync(() -> call(service, method, arguments), RMI);
    }
    public static Throwable cause(Throwable error) {
        while ((error instanceof CompletionException || error instanceof ExecutionException) && error.getCause() != null)
            error = error.getCause();
        return error;
    }
    public static <T> T blocking(Class<T> type, AsyncService service) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                switch (method.getName()) {
                    case "toString": return "Service proxy: " + type.getSimpleName();
                    case "hashCode": return System.identityHashCode(proxy);
                    case "equals": return proxy == args[0];
                    default: throw new IllegalArgumentException();
                }
            }
            try { return service.invoke(method, args == null ? new Object[0] : args).toCompletableFuture().get(); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new RemoteException("Interrupted", error); }
            catch (ExecutionException error) { throw new RemoteException("Service request failed", cause(error)); }
        }));
    }
}
