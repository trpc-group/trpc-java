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

package org.slf4j;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.tencent.trpc.core.utils.StringUtils;
import java.util.Deque;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.spi.MDCAdapter;

/**
 * TrpcMDCAdapter
 *
 * <p>trpc internal cross-thread context tracking.</p>
 *
 * <p>Due to the implementation of the MDC class, the MDC.mdcAdapter does not provide read and write interfaces, so
 * the package is org.slf4j.</p>
 *
 * @link https://github.com/alibaba/transmittable-thread-local
 */
public class TrpcMDCAdapter implements MDCAdapter {

    private static final int WRITE_OPERATION = 1;

    private static final int READ_OPERATION = 2;

    private static final TrpcMDCAdapter mtcMDCAdapter;

    private final ThreadLocal<Map<String, String>> copyOnInheritThreadLocal = new TransmittableThreadLocal<>();

    /**
     * Keeps track of the last operation performed
     */
    private final ThreadLocal<Integer> lastOperation = new ThreadLocal<>();

    static {
        mtcMDCAdapter = new TrpcMDCAdapter();
        try {
            // In SLF4J 2.0, MDC.mdcAdapter is no longer directly accessible
            // We need to use reflection to set it
            Field field = MDC.class.getDeclaredField("mdcAdapter");
            field.setAccessible(true);
            field.set(null, mtcMDCAdapter);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TrpcMDCAdapter", e);
        }
    }

    public static MDCAdapter init() {
        return mtcMDCAdapter;
    }

    /**
     * Put a context value (the <code>val</code> parameter) as identified with the
     * <code>key</code> parameter into the current thread's context map. Note that
     * contrary to log4j, the <code>val</code> parameter can be null.
     * <p/>
     * <p/>
     * If the current thread does not have a context map it is created as a side
     * effect of this call.
     *
     * @throws NullPointerException in case the "key" parameter is null
     */
    @Override
    public void put(String key, String val) {
        Objects.requireNonNull(key, "key cannot be null");
        Map<String, String> oldMap = copyOnInheritThreadLocal.get();
        Integer lastOp = getAndSetLastOperation();
        if (wasLastOpReadOrNull(lastOp) || oldMap == null) {
            Map<String, String> newMap = duplicateAndInsertNewMap(oldMap);
            newMap.put(key, val);
        } else {
            oldMap.put(key, val);
        }
    }

    /**
     * Remove the context identified by the <code>key</code> parameter.
     * <p/>
     */
    @Override
    public void remove(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        Map<String, String> oldMap = copyOnInheritThreadLocal.get();
        if (oldMap == null) {
            return;
        }
        Integer lastOp = getAndSetLastOperation();
        if (wasLastOpReadOrNull(lastOp)) {
            Map<String, String> newMap = duplicateAndInsertNewMap(oldMap);
            newMap.remove(key);
        } else {
            oldMap.remove(key);
        }
    }


    /**
     * Clear all entries in the MDC.
     */
    @Override
    public void clear() {
        lastOperation.set(WRITE_OPERATION);
        copyOnInheritThreadLocal.remove();
    }

    /**
     * Get the context identified by the <code>key</code> parameter.
     * <p/>
     */
    @Override
    public String get(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return Optional.ofNullable(getPropertyMap())
                .map(propertyMap -> propertyMap.get(key)).orElse(null);
    }

    /**
     * Get the current thread's MDC as a map. This method is intended to be used
     * internally.
     */
    public Map<String, String> getPropertyMap() {
        lastOperation.set(READ_OPERATION);
        return copyOnInheritThreadLocal.get();
    }

    /**
     * Return a copy of the current thread's context map. Returned value may be
     * null.
     */
    @Override
    public Map<String, String> getCopyOfContextMap() {
        lastOperation.set(READ_OPERATION);
        return Optional.ofNullable(copyOnInheritThreadLocal.get())
                .map(HashMap::new).orElse(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setContextMap(Map<String, String> contextMap) {
        lastOperation.set(WRITE_OPERATION);
        Map<String, String> newMap = new ConcurrentHashMap<>(contextMap);
        // the newMap replaces the old one for serialisation's sake
        copyOnInheritThreadLocal.set(newMap);
    }

    private Integer getAndSetLastOperation() {
        Integer lastOp = lastOperation.get();
        lastOperation.set(TrpcMDCAdapter.WRITE_OPERATION);
        return lastOp;
    }

    private static boolean wasLastOpReadOrNull(Integer lastOp) {
        return lastOp == null || lastOp == READ_OPERATION;
    }

    /**
     * Remove duplicate Map parameters and lock the oldMap to avoid being modified by other threads.
     *
     * @param oldMap {@code Map<String, String>}
     * @return Map {@code Map<String, String>}
     */
    private Map<String, String> duplicateAndInsertNewMap(Map<String, String> oldMap) {
        Map<String, String> newMap = new ConcurrentHashMap<>();
        if (oldMap != null) {
            // we don't want the parent thread modifying oldMap while we are iterating over it
            synchronized (oldMap) {
                newMap.putAll(oldMap);
            }
        }
        copyOnInheritThreadLocal.set(newMap);
        return newMap;
    }

    /**
     * Push a context value as identified with the key parameter into the current thread's context map.
     * This is a SLF4J 2.0 new method for supporting MDC stack operations.
     *
     * @param key   the key
     * @param value the value to push
     * @throws NullPointerException in case the "key" parameter is null
     */
    @Override
    public void pushByKey(String key, String value) {
        Objects.requireNonNull(key, "key cannot be null");
        Map<String, String> oldMap = copyOnInheritThreadLocal.get();
        Integer lastOp = getAndSetLastOperation();
        if (wasLastOpReadOrNull(lastOp) || oldMap == null) {
            Map<String, String> newMap = duplicateAndInsertNewMap(oldMap);
            newMap.put(key, value);
        } else {
            oldMap.put(key, value);
        }
    }

    /**
     * Pop the context identified by key parameter from the current thread's context map.
     * This is a SLF4J 2.0 new method for supporting MDC stack operations.
     *
     * @param key the key
     * @return the value removed, or null if there was no value for the key
     * @throws NullPointerException in case the "key" parameter is null
     */
    @Override
    public String popByKey(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        Map<String, String> oldMap = copyOnInheritThreadLocal.get();
        if (oldMap == null) {
            return null;
        }
        Integer lastOp = getAndSetLastOperation();
        if (wasLastOpReadOrNull(lastOp)) {
            Map<String, String> newMap = duplicateAndInsertNewMap(oldMap);
            return newMap.remove(key);
        } else {
            return oldMap.remove(key);
        }
    }

    /**
     * Clear all the entries in the deque identified by the key parameter.
     * This is a SLF4J 2.0.7+ new method for clearing MDC stacks.
     *
     * @param key the key
     * @throws NullPointerException in case the "key" parameter is null
     */
    @Override
    public void clearDequeByKey(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        remove(key);
    }

    /**
     * Get a copy of the deque identified by the key parameter.
     * This is a SLF4J 2.0 new method for supporting MDC stack operations.
     * Since our implementation uses a simple Map, we return null to indicate no deque.
     *
     * @param key the key
     * @return null (this implementation does not support deques)
     */
    @Override
    public Deque<String> getCopyOfDequeByKey(String key) {
        // This implementation uses a simple Map structure, not Deque
        // Return null to indicate no deque exists for the key
        return null;
    }

}