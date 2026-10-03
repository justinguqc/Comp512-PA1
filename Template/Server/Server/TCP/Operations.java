package Server.TCP;

import Server.Interface.IInventoryManager;
import Server.Interface.IResourceManager;
import java.lang.reflect.*;
import java.util.*;

/** Stage 5: explicit allowlist from the public interfaces, not arbitrary remote reflection. */
public final class Operations {
    private static final Map<String, Method> METHODS = new HashMap<>();
    static { for (Method method : IInventoryManager.class.getMethods()) METHODS.put(key(method), method); }
    private Operations() { }
    public static String key(Method method) {
        return method.getName() + "(" + String.join(",", Arrays.stream(method.getParameterTypes()).map(Class::getName).toList()) + ")";
    }
    public static Method method(String name, Class<?>... types) {
        try { return IInventoryManager.class.getMethod(name, types); }
        catch (NoSuchMethodException error) { throw new IllegalArgumentException(error); }
    }
    public static Method resolve(String key, Object[] arguments, boolean inventory) {
        Method method = METHODS.get(key);
        if (method == null || (!inventory && method.getDeclaringClass() != IResourceManager.class))
            throw new IllegalArgumentException("Unknown or disallowed operation: " + key);
        Class<?>[] types = method.getParameterTypes();
        if (types.length != arguments.length) throw new IllegalArgumentException("Invalid argument count");
        for (int i = 0; i < types.length; i++) {
            Class<?> expected = types[i] == int.class ? Integer.class : types[i] == boolean.class ? Boolean.class : types[i];
            if (!expected.isInstance(arguments[i])) throw new IllegalArgumentException("Invalid argument type");
        }
        return method;
    }
}
