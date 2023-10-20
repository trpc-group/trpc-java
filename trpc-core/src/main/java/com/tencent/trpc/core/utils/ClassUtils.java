/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 THL A29 Limited, a Tencent company. 
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.core.utils;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;

public class ClassUtils {

    private static final Logger logger = LoggerFactory.getLogger(ClassUtils.class);

    public static boolean isByteArray(Object obj) {
        return ((obj instanceof byte[]) || (obj instanceof Byte[]));
    }

    public static boolean isByteArray(Class<?> type) {
        return type == byte[].class || type == Byte[].class;
    }

    public static byte[] cast2ByteArray(Object obj) {
        PreconditionUtils.checkArgument(isByteArray(obj), "%s is not instanceof byte array", obj);
        if ((obj instanceof byte[])) {
            return (byte[]) obj;
        } else {
            return ArrayUtils.toPrimitive((Byte[]) obj);
        }
    }

    public static <T> T newInstance(Class<?> clazz) {
        try {
            return (T) clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(
                    "instance Class: " + clazz.getName() + " with ex: " + e.getMessage(), e);
        }
    }

    public static List<Class> getAllInterfaces(final Class cls) {
        if (cls == null) {
            return Collections.emptyList();
        }
        final LinkedHashSet<Class<?>> interfacesFound = new LinkedHashSet<>();
        getAllInterfaces(cls, interfacesFound);
        return new ArrayList<>(interfacesFound);
    }

    /**
     * Get the interfaces for the specified class.
     */
    private static void getAllInterfaces(Class<?> cls, final HashSet<Class<?>> interfacesFound) {
        while (cls != null) {
            final Class<?>[] interfaces = cls.getInterfaces();
            for (final Class<?> i : interfaces) {
                if (interfacesFound.add(i)) {
                    getAllInterfaces(i, interfacesFound);
                }
            }
            cls = cls.getSuperclass();
        }
    }

    /**
     * Get the specified method of the specified class, returns null when the method does not exist.
     *
     * @param clazz the class to get the method from
     * @param name method name
     * @param parameterTypes parameter types
     * @return the corresponding method if it exists, otherwise returns null
     */
    public static Method getDeclaredMethod(Class<?> clazz, String name,
            Class<?>... parameterTypes) {
        if (clazz == null) {
            return null;
        }

        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            logger.error("the class:{} no such method:{}, parameterTypes:{}", clazz, name,
                    parameterTypes);
        }
        return null;
    }

    /**
     * Get the value of a field.
     *
     * @param target target object instance
     * @param field field of the object's class
     * @return value of target object instance
     */
    public static Optional<Object> getValue(Object target, Field field) {
        boolean accessible = field.isAccessible();
        Object result = null;
        try {
            if (!accessible) {
                field.setAccessible(true);
            }
            result = field.get(target);
        } catch (IllegalAccessException e) {
            logger.error("get value from obj: {}, field: {} error: ", target, field.getName(), e);
        } finally {
            field.setAccessible(accessible);
        }
        return Optional.ofNullable(result);
    }

    /**
     * Get the static value of a field.
     *
     * @param field field of the class
     * @return static value
     */
    public static Optional<Object> getStaticValue(Field field) {
        return getValue(null, field);
    }

    /**
     * Get constant values of target clazz. Field must be public static final.
     *
     * @param clazz target clazz
     * @return constant values
     */
    public static List<Object> getConstantValues(Class<?> clazz) {
        return Stream.of(clazz.getFields())
                // getFields must return public, skip filter Modifier.isPublic
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .filter(f -> Modifier.isFinal(f.getModifiers()))
                .map(ClassUtils::getStaticValue)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

}
