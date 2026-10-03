package Server.Common;

import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;

/** Stage 5: one invocation boundary for all signatures and both transports. */
@FunctionalInterface
public interface AsyncService {
    CompletionStage<Object> invoke(Method method, Object[] arguments);
}
