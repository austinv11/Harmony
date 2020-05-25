package harmony.command.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ProcessorUtils {

    public static int methodHash(String name, String returnType, String[] paramTypes) {
        return Objects.hash(name, returnType, Arrays.hashCode(paramTypes));
    }

    public static int methodHash(Method m) {
        return methodHash(m.getName(), m.getReturnType().getName(),
                Arrays.stream(m.getParameterTypes()).map(Class::getName).collect(Collectors.toList())
                        .toArray(new String[m.getParameterCount()]));
    }
}
