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

package com.tencent.trpc.registry.center;


import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.DisposableExtension;
import com.tencent.trpc.core.extension.InitializingExtension;
import com.tencent.trpc.core.extension.PluginConfigAware;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.core.utils.ConcurrentHashSet;
import com.tencent.trpc.registry.common.RegistryCenterConfig;
import com.tencent.trpc.registry.common.RegistryCenterData;
import com.tencent.trpc.registry.common.RegistryCenterListenerSet;
import com.tencent.trpc.registry.factory.AbstractRegistryFactory;
import com.tencent.trpc.registry.util.RegistryCenterCache;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;


/**
 * Abstract class for registry center.
 * <pre>1. Abstracts the four default operations (including two cancellation operations) of registration/subscription in
 * the registry center, and caches the data after the operation.</pre>
 * <pre>2. The cached data can be optionally persisted to local disk. When the registry center is unavailable/network
 * jitter occurs, the data can be read from the cache first to ensure availability.</pre>
 * <pre>3. When the registry center is unavailable/network jitter occurs, the cache can be invalidated, and the cache
 * will be invalidated after the cache validity period exceeds. The cache validity period can be configured; after
 * reconnection, this operation can be cancelled.</pre>
 * <pre>4. Provides the recover interface. When reconnecting to the registry center, services can be
 * re-registered/subscribed.</pre>
 * <pre>5. Provides the destroy interface. When the service stops, the framework will automatically trigger a graceful
 * exit, and automatically cancel the registration/subscription of related services.</pre>
 * <p>Interfaces that subclasses must implement:</p>
 * <pre>1. {@link #init()}: Initialization interface</pre>
 * <p>Interfaces that subclasses need to extend and implement. The following interfaces only implement basic
 * functions:</p>
 * <pre>1. {@link #register(RegisterInfo)}: Register service</pre>
 * <pre>2. {@link #unregister(RegisterInfo)}: Unregister service</pre>
 * <pre>3. {@link #subscribe(RegisterInfo, NotifyListener)}: Subscribe to service</pre>
 * <pre>4. {@link #unsubscribe(RegisterInfo, NotifyListener)}: Unsubscribe from service</pre>
 * <pre>5. {@link #notify(RegisterInfo, NotifyListener, List)}: Update the data corresponding to the subscribed
 * service</pre>
 * <p>Interfaces that subclasses can optionally extend and implement:</p>
 * <pre>1. {@link #recoverRegistered()}: Restore registered services</pre>
 * <pre>2. {@link #recoverSubscribed()}: Restore subscribed services</pre>
 * <pre>3. {@link #destroy()}: Destroy the registry center</pre>
 */
public abstract class AbstractRegistryCenter implements RegistryCenter, PluginConfigAware,
        InitializingExtension, DisposableExtension {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRegistryCenter.class);

    /**
     * Configuration for the registry center.
     */
    protected RegistryCenterConfig config;

    /**
     * Local cache, used to retrieve data from cache when the registry center is unavailable.
     */
    protected RegistryCenterCache cache;

    /**
     * Cached registered services.
     */
    private final Set<RegisterInfo> registeredRegisterInfos = new ConcurrentHashSet<>();

    /**
     * Mapping of subscribed services and their bound listeners. A service may have multiple listeners bound to it,
     * forming a one-to-many relationship.
     */
    private final Map<RegisterInfo, RegistryCenterListenerSet> subscribedRegisterInfos = new ConcurrentHashMap<>();

    /**
     * Mapping of subscribed services and their data in the registry center.
     */
    private final Map<RegisterInfo, RegistryCenterData> notifiedRegisterInfos = new ConcurrentHashMap<>();

    @Override
    public void setPluginConfig(PluginConfig pluginConfig) throws TRpcExtensionException {
        this.config = new RegistryCenterConfig(pluginConfig);
        this.cache = new RegistryCenterCache(config);
    }

    /**
     * Register a service.
     *
     * @param registerInfo The service to be registered.
     */
    @Override
    public void register(RegisterInfo registerInfo) {
        Objects.requireNonNull(registerInfo, "registerInfo can not be null");
        logger.debug("[register] registerInfo: {}", registerInfo);

        registeredRegisterInfos.add(registerInfo);
    }

    /**
     * Unregister a service.
     *
     * @param registerInfo The service to be unregistered.
     */
    @Override
    public void unregister(RegisterInfo registerInfo) {
        Objects.requireNonNull(registerInfo, "registerInfo can not be null");
        if (logger.isDebugEnabled()) {
            logger.debug("[unregister] registerInfo: {}", registerInfo);
        }
        registeredRegisterInfos.remove(registerInfo);
    }

    /**
     * Subscribe to a service.
     *
     * @param registerInfo The service to be subscribed.
     * @param notifyListener The listener for the service. When the data for the subscribed service is updated,
     *         this listener's callback method will be triggered.
     */
    @Override
    public void subscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {
        Objects.requireNonNull(registerInfo, "registerInfo can not be null");
        Objects.requireNonNull(notifyListener, "notifyListener can not be null");
        if (logger.isDebugEnabled()) {
            logger.debug("[subscribe] registerInfo: {}, notifyListener: {}", registerInfo, notifyListener);
        }

        synchronized (this) {
            RegistryCenterListenerSet registryCenterListenerSet = subscribedRegisterInfos
                    .computeIfAbsent(registerInfo, n -> new RegistryCenterListenerSet());
            registryCenterListenerSet.addNotifyListener(notifyListener);
        }
    }

    /**
     * Unsubscribe from a service and destroy its bound listener. If the service has no bound listeners, remove it from
     * the cache.
     *
     * @param registerInfo The service to be unsubscribed.
     * @param notifyListener The listener for the service. When the data for the subscribed service is updated,
     *         this listener's callback method will be triggered.
     */
    @Override
    public void unsubscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {
        Objects.requireNonNull(registerInfo, "registerInfo can not be null");
        Objects.requireNonNull(notifyListener, "notifyListener can not be null");
        if (logger.isDebugEnabled()) {
            logger.debug("[unsubscribe] registerInfo: {}, notifyListener: {}", registerInfo, notifyListener);
        }

        synchronized (this) {
            RegistryCenterListenerSet registryCenterListenerSet = subscribedRegisterInfos.get(registerInfo);
            if (registryCenterListenerSet != null && !registryCenterListenerSet.isEmpty()) {
                registryCenterListenerSet.removeNotifyListener(notifyListener);
                notifyListener.destroy();
                if (registryCenterListenerSet.isEmpty()) {
                    subscribedRegisterInfos.remove(registerInfo);
                }
            }
        }
    }

    /**
     * Interface for callbacks when the data for a subscribed service is updated.
     *
     * @param registerInfo The subscribed service.
     * @param notifyListener The listener for the service.
     * @param updatingRegisterInfos The data that needs to be updated for the subscribed service.
     */
    public void notify(RegisterInfo registerInfo, NotifyListener notifyListener,
            List<RegisterInfo> updatingRegisterInfos) {
        Objects.requireNonNull(registerInfo, "registerInfo can not be null");
        Objects.requireNonNull(notifyListener, "notifyListener can not be null");
        Objects.requireNonNull(updatingRegisterInfos, "updatingRegisterInfos can not be null");

        // 0. Filter data that matches the subscribed service.
        List<RegisterInfo> realUpdatingRegisterInfos = updatingRegisterInfos.stream()
                .filter(updatingRi -> updatingRi.getServiceName().equals(registerInfo.getServiceName()))
                .collect(Collectors.toList());

        // 1. Build a cache of registry data bound to the subscribed service
        RegistryCenterData registryCenterData = notifiedRegisterInfos
                .computeIfAbsent(registerInfo, ri -> new RegistryCenterData());
        registryCenterData.putAllRegisterInfos(realUpdatingRegisterInfos);

        // 2. Notify the listener to update the data.
        notifyListener.notify(realUpdatingRegisterInfos);

        // 3. Cache the data.
        cache.save(registerInfo, registryCenterData);
    }

    /**
     * Unregister and unsubscribe from all services. Automatically called when the framework exits.
     */
    @Override
    public void destroy() {
        if (logger.isDebugEnabled()) {
            logger.debug("[Destroy] registry center: {}", config);
        }
        destroyRegistered();
        destroySubscribed();
        AbstractRegistryFactory.removeDestroyedRegistry(this);
    }

    public Set<RegisterInfo> getRegisteredRegisterInfos() {
        return Collections.unmodifiableSet(registeredRegisterInfos);
    }

    public Map<RegisterInfo, RegistryCenterListenerSet> getSubscribedRegisterInfos() {
        return Collections.unmodifiableMap(subscribedRegisterInfos);
    }

    public Map<RegisterInfo, RegistryCenterData> getNotifiedRegisterInfos() {
        return Collections.unmodifiableMap(notifiedRegisterInfos);
    }

    public RegistryCenterConfig getRegistryCenterConfig() {
        return this.config;
    }

    /**
     * Restore registered and subscribed services.
     * This is mainly used when the registry restarts or when there is a network issue and needs to reconnect to the
     * registry.
     */
    protected void recover() {
        recoverRegistered();
        recoverSubscribed();
    }

    /**
     * Restore registered services. Subclasses may extend the implementation of the register interface.
     */
    protected void recoverRegistered() {
        Set<RegisterInfo> recoverRegistered = getRegisteredRegisterInfos();
        if (CollectionUtils.isEmpty(recoverRegistered)) {
            return;
        }

        recoverRegistered.forEach(registerInfo -> {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("[Recover] Register registerInfo: {}", registerInfo);
                }
                register(registerInfo);
            } catch (Exception e) {
                logger.warn("[Recover] Failed to register registerInfo: {}, cause: ", registerInfo, e);
            }
        });
    }


    /**
     * Restore subscribed services. Subclasses may extend the implementation of the subscribe interface.
     */
    protected void recoverSubscribed() {
        Map<RegisterInfo, RegistryCenterListenerSet> recoverSubscribed = getSubscribedRegisterInfos();
        if (MapUtils.isEmpty(recoverSubscribed)) {
            return;
        }
        recoverSubscribed.forEach((registerInfo, registryCenterListenerSet) ->
                registryCenterListenerSet.getNotifyListeners().forEach(notifyListener -> {
                    try {
                        if (logger.isDebugEnabled()) {
                            logger.debug("[Recover] Subscribe registerInfo: {}, listener: {}",
                                    registerInfo, notifyListener);
                        }
                        subscribe(registerInfo, notifyListener);
                    } catch (Exception e) {
                        logger.warn("[Recover] Failed to subscribe registerInfo: {}, cause: ", registerInfo, e);
                    }
                })
        );
    }

    /**
     * When disconnected from the registry, set an expiration time for the cache.
     */
    protected void expireCache() {
        this.cache.expireCache();
    }

    /**
     * When retrying to connect to the registry, cancel the cache expiration.
     */
    protected void cancelExpireCache() {
        this.cache.cancelExpireCache();
    }

    /**
     * Unregister all registered services.
     */
    private void destroyRegistered() {
        Set<RegisterInfo> destroyRegistered = getRegisteredRegisterInfos();
        if (CollectionUtils.isEmpty(destroyRegistered)) {
            return;
        }
        destroyRegistered.forEach(registerInfo -> {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("[Destroy] Unregister registerInfo: {}", registerInfo);
                }
                unregister(registerInfo);
            } catch (Exception e) {
                logger.warn("[Destroy] Failed to unregister registerInfo: {}, cause: ", registerInfo, e);
            }
        });
    }

    /**
     * Unsubscribe from all subscribed services.
     */
    private void destroySubscribed() {
        Map<RegisterInfo, RegistryCenterListenerSet> destroySubscribed = getSubscribedRegisterInfos();
        if (MapUtils.isEmpty(destroySubscribed)) {
            return;
        }
        destroySubscribed.forEach((registerInfo, registryCenterListenerSet) ->
                registryCenterListenerSet.getNotifyListeners().forEach(notifyListener -> {
                    try {
                        if (logger.isDebugEnabled()) {
                            logger.debug("[Destroy] Unsubscribe registerInfo: {}, listener: {}",
                                    registerInfo, notifyListener);
                        }
                        unsubscribe(registerInfo, notifyListener);
                    } catch (Exception e) {
                        logger.warn("[Destroy] Failed to unsubscribe registerInfo: {}, cause: ", registerInfo, e);
                    }
                })
        );
    }

}
