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

package com.tencent.trpc.registry.factory;

import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_ADDRESSED_KEY;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.registry.center.AbstractRegistryCenter;
import com.tencent.trpc.registry.center.NotifyListener;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Abstract registry factory class.
 *
 * @see RegistryFactory
 */
public abstract class AbstractRegistryFactory implements RegistryFactory {

    /**
     * Register a lock for the service acquisition process.
     */
    protected static final ReentrantLock LOCK = new ReentrantLock();

    /**
     * Registry Collection cache.
     */
    protected static final Map<String, AbstractRegistryCenter> REGISTRIES = new HashMap<>();

    /**
     * Flag indicating whether to destroy.
     */
    private static final AtomicBoolean DESTROYED = new AtomicBoolean(false);

    private static final Logger logger = LoggerFactory.getLogger(AbstractRegistryFactory.class);

    private static final AbstractRegistryCenter DEFAULT_NOP_REGISTRY = new AbstractRegistryCenter() {

        @Override
        public void init() throws TRpcExtensionException {

        }

        @Override
        public boolean isAvailable() {
            return false;
        }


        @Override
        public void register(RegisterInfo registerInfo) {

        }

        @Override
        public void unregister(RegisterInfo registerInfo) {

        }

        @Override
        public void subscribe(RegisterInfo registerInfo, NotifyListener listener) {

        }

        @Override
        public void unsubscribe(RegisterInfo registerInfo, NotifyListener listener) {

        }
    };

    /**
     * Get all registries.
     *
     * @return all registries
     */
    public static Collection<AbstractRegistryCenter> getRegistries() {
        return Collections.unmodifiableCollection(new LinkedList<>(REGISTRIES.values()));
    }

    /**
     * Get the registration information for the specified key.
     *
     * @param key the specified key
     * @return the registration information
     */
    public static AbstractRegistryCenter getRegistry(String key) {
        return REGISTRIES.get(key);
    }

    /**
     * Close all created registries.
     */
    public static void destroyAll() {
        if (!DESTROYED.compareAndSet(false, true)) {
            return;
        }

        if (logger.isInfoEnabled()) {
            logger.info("Close all registries " + getRegistries());
        }
        // 锁定注册服务关闭过程
        LOCK.lock();
        try {
            getRegistries().forEach(registry -> {
                try {
                    registry.destroy();
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                }
            });
            REGISTRIES.clear();
        } finally {
            LOCK.unlock();
        }
    }

    @Override
    public AbstractRegistryCenter connect(ProtocolConfig protocolConfig) {
        if (DESTROYED.get()) {
            logger.warn("All registry instances have been destroyed, failed to fetch any instance. "
                    + "Usually, this means no need to try to do unnecessary redundant resource clearance,"
                    + " all registries has been taken care of.");
            return DEFAULT_NOP_REGISTRY;
        }

        String connectUrlCacheKey = createRegistryCacheKey(protocolConfig);
        AbstractRegistryCenter registry = REGISTRIES.getOrDefault(connectUrlCacheKey, null);
        if (null != registry && registry.isAvailable()) {
            return registry;
        }
        // Lock the registration service access process to ensure a single instance of the registry.
        LOCK.lock();
        try {
            registry = REGISTRIES.getOrDefault(connectUrlCacheKey, null);
            if (null != registry && registry.isAvailable()) {
                return registry;
            }

            // Create a registry object.
            registry = createRegistry(protocolConfig);
            Objects.requireNonNull(registry, "Can not create registry " + protocolConfig);

            REGISTRIES.put(connectUrlCacheKey, registry);
            return registry;
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Get the connection address of the registry center client.
     *
     * @param protocolConfig the client connection configuration
     * @return the connection address of the client
     */
    private String createRegistryCacheKey(ProtocolConfig protocolConfig) {
        if (protocolConfig.getExtMap().containsKey(REGISTRY_CENTER_ADDRESSED_KEY)) {
            return String.valueOf(protocolConfig.getExtMap().get(REGISTRY_CENTER_ADDRESSED_KEY));
        }
        throw new IllegalArgumentException("register can't get addresses");
    }

    /**
     * Create a registry center service.
     *
     * @param protocolConfig the registration information
     * @return the registry center service
     */
    protected abstract AbstractRegistryCenter createRegistry(ProtocolConfig protocolConfig);

    /**
     * Remove a destroyed registry center.
     *
     * @param registryCenter the registry center to be removed
     */
    public static void removeDestroyedRegistry(AbstractRegistryCenter registryCenter) {
        LOCK.lock();
        try {
            REGISTRIES.entrySet().removeIf(entry -> entry.getValue().equals(registryCenter));
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * for unit test
     */
    public static void clearRegistryNotDestroy() {
        REGISTRIES.clear();
        DESTROYED.set(false);
    }
}
