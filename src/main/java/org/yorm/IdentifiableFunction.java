package org.yorm;

import org.yorm.exception.YormException;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A {@link Serializable} version of {@link Function}. While we do not actually serialize lambdas, the serialized
 * form provides information about the invocation. Like the method name, and the type
 * @param <T>
 * @param <R>
 */
public interface IdentifiableFunction<T, R> extends Serializable {
    R apply(T t);

    default String getCallingFunctionName() {
        return IdentifiableFunction.getCallingFunctionName(this);
    }

    class PrivateData
    {
        static PrivateData INSTANCE = new PrivateData();

        final Map<Class<?>, MethodHandle> writeReplaceMethodHandles = new HashMap<>();

        private PrivateData() {
        }
    }

    private static String getCallingFunctionName(IdentifiableFunction<?, ?> identifiableFunction) {
        try {
            MethodHandle writeReplaceMethodHandle =
                    PrivateData.INSTANCE.writeReplaceMethodHandles.computeIfAbsent(identifiableFunction.getClass(), clazz -> {
                        try {
                            return MethodHandles.privateLookupIn(clazz, MethodHandles.lookup()).findVirtual(
                                    identifiableFunction.getClass(),
                                    "writeReplace",
                                    MethodType.methodType(Object.class)
                            );
                        } catch (NoSuchMethodException | IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                );
            SerializedLambda serializedLambda = (SerializedLambda) writeReplaceMethodHandle.invoke(identifiableFunction);

            return serializedLambda.getImplMethodName();
        } catch (Throwable t) {
            throw new RuntimeException("Could not identify function name.", t);
        }
    }
}
