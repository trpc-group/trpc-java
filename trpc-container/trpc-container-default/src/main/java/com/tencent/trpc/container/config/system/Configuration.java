/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 Tencent.
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.container.config.system;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Configuration interface.
 *
 * <p>Abstract the configuration reading operation, system variables, environment variables implement this interface
 * separately, and override the get property method.</p>
 *
 * <p>By recombining the relationships of multiple configurations, clients can flexibly customize the multi-level
 * configuration reading capability.</p>
 */
public interface Configuration {

    Map<Class<?>, Function<String, ?>> CONVERTERS = createConvertersMap();

    static Map<Class<?>, Function<String, ?>> createConvertersMap() {
        Map<Class<?>, Function<String, ?>> map = new HashMap<>();
        map.put(Integer.class, (Function<String, Integer>) Integer::valueOf);
        map.put(Long.class, (Function<String, Long>) Long::valueOf);
        map.put(Byte.class, (Function<String, Byte>) Byte::valueOf);
        map.put(Short.class, (Function<String, Short>) Short::valueOf);
        map.put(Float.class, (Function<String, Float>) Float::valueOf);
        map.put(Double.class, (Function<String, Double>) Double::valueOf);
        return Collections.unmodifiableMap(map);
    }

    static Boolean toBooleanObject(boolean bool) {
        return bool ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Actual method to get the property (template method).
     *
     * @param key specified property key
     * @return object
     */
    Object getInternalProperty(String key);

    /**
     * Specify a key and get the corresponding value (no default value, can be null).
     *
     * @param key specified property key
     * @return returns the property value
     */
    default String getString(String key) {
        return convert(String.class, key, null);
    }

    /**
     * Specify a key and get the corresponding value.
     *
     * @param key specified property key
     * @param defaultValue if the corresponding value cannot be obtained, return this default value
     * @return string
     */
    default String getString(String key, String defaultValue) {
        return convert(String.class, key, defaultValue);
    }

    default int getInt(String key) {
        Integer i = this.getInteger(key, null);
        if (i != null) {
            return i;
        } else {
            throw new NoSuchElementException('\'' + key + "' doesn't map to an existing object");
        }
    }

    default int getInt(String key, int defaultValue) {
        Integer i = this.getInteger(key, null);
        return i == null ? defaultValue : i;
    }

    default Integer getInteger(String key, Integer defaultValue) {
        try {
            return convert(Integer.class, key, defaultValue);
        } catch (NumberFormatException e) {
            throw new IllegalStateException('\'' + key + "' doesn't map to a Integer object", e);
        }
    }

    default boolean getBoolean(String key) {
        Boolean b = this.getBoolean(key, null);
        if (b != null) {
            return b;
        } else {
            throw new NoSuchElementException('\'' + key + "' doesn't map to an existing object");
        }
    }

    default boolean getBoolean(String key, boolean defaultValue) {
        return this.getBoolean(key, toBooleanObject(defaultValue));
    }

    default Boolean getBoolean(String key, Boolean defaultValue) {
        try {
            return convert(Boolean.class, key, defaultValue);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Try to get " + '\'' + key + "' failed, maybe because this key doesn't map to a Boolean object", e);
        }
    }

    default byte getByte(String key) {
        Byte b = this.getByte(key, null);
        if (b != null) {
            return b;
        } else {
            throw new NoSuchElementException('\'' + key + "' doesn't map to an existing object");
        }
    }

    default byte getByte(String key, byte defaultValue) {
        return this.getByte(key, Byte.valueOf(defaultValue));
    }

    default Byte getByte(String key, Byte defaultValue) {
        try {
            return convert(Byte.class, key, defaultValue);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Try to get " + '\'' + key + "' failed, maybe because this key doesn't map to a Byte object", e);
        }
    }

    default short getShort(String key) {
        Short b = this.getShort(key, null);
        if (b != null) {
            return b;
        } else {
            throw new NoSuchElementException('\'' + key + "' doesn't map to an existing object");
        }
    }

    default short getShort(String key, short defaultValue) {
        return this.getShort(key, Short.valueOf(defaultValue));
    }

    default Short getShort(String key, Short defaultValue) {
        try {
            return convert(Short.class, key, defaultValue);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Try to get " + '\'' + key + "' failed, maybe because this key doesn't map to a Short object", e);
        }
    }

    default float getFloat(String key) {
        Float b = this.getFloat(key, null);
        if (b != null) {
            return b;
        } else {
            throw new NoSuchElementException('\'' + key + "' doesn't map to an existing object");
        }
    }

    default float getFloat(String key, float defaultValue) {
        return this.getFloat(key, Float.valueOf(defaultValue));
    }

    default Float getFloat(String key, Float defaultValue) {
        try {
            return convert(Float.class, key, defaultValue);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Try to get " + '\'' + key + "' failed, maybe because this key doesn't map to a Float object", e);
        }
    }

    default double getDouble(String key) {
        Double b = this.getDouble(key, null);
        if (b != null) {
            return b;
        } else {
            throw new NoSuchElementException('\'' + key + "' doesn't map to an existing object");
        }
    }

    default double getDouble(String key, double defaultValue) {
        return this.getDouble(key, Double.valueOf(defaultValue));
    }

    default Double getDouble(String key, Double defaultValue) {
        try {
            return convert(Double.class, key, defaultValue);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Try to get " + '\'' + key + "' failed, maybe because this key doesn't map to a Double object", e);
        }
    }

    default Object getProperty(String key) {
        return getProperty(key, null);
    }

    default Object getProperty(String key, Object defaultValue) {
        Object value = getInternalProperty(key);
        return value != null ? value : defaultValue;
    }

    default <T> T convert(Class<T> clazz, String key, T defaultValue) {
        String value = (String) getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }

        Object obj = doConvert(clazz, value);

        return clazz.cast(obj);
    }

    /**
     * Convert to the corresponding type.
     *
     * @param clazz type class
     * @param value original value
     * @return converted value
     */
    default <T> Object doConvert(Class<T> clazz, String value) {
        Object obj = null;
        if (Boolean.class.equals(clazz) || Boolean.TYPE.equals(clazz)) {
            obj = Boolean.valueOf(value);
        } else if (Number.class.isAssignableFrom(clazz) || clazz.isPrimitive()) {
            obj = doConvertNumber(clazz, value);
        } else if (clazz.isEnum()) {
            obj = Enum.valueOf(clazz.asSubclass(Enum.class), value);
        }
        return obj;
    }

    /**
     * Parse number type.
     *
     * @param clazz type class
     * @param value original value
     * @return T
     */
    default <T> Object doConvertNumber(Class<T> clazz, String value) {
        Function<String, ?> converter = CONVERTERS.get(clazz);
        if (converter != null) {
            return converter.apply(value);
        }
        return null;
    }

}
