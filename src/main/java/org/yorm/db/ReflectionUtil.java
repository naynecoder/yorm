package org.yorm.db;

import org.yorm.IdentifiableFunction;
import org.yorm.exception.YormException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.util.HashMap;
import java.util.Map;

public class ReflectionUtil {
    private static ReflectionUtil instance = null;

    private final Map<Class<?>, MethodHandle> writeReplaceMethodHandles = new HashMap<>();

    private ReflectionUtil() {

    }

    // Get instance method for the singleton
    public static ReflectionUtil getInstance() {
        if (instance == null) {
            instance = new ReflectionUtil();
        }
        return instance;
    }

    public <T extends Record, U> String getFunctionName(IdentifiableFunction<T, U> getter) throws YormException {
        try {
            return getter.getCallingFunctionName();
        } catch (Throwable e) {
            throw new YormException("Couldn't look up the calleing function's name.", e);
        }
    }
}