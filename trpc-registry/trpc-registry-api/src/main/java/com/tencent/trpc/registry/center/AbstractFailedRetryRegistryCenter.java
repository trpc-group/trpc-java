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

import static com.tencent.trpc.registry.common.Constants.TICKS_PER_WHEEL;

import com.tencent.trpc.core.common.NamedThreadFactory;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.timer.HashedWheelTimer;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.registry.common.ConfigConstants;
import com.tencent.trpc.registry.common.RegistryCenterListenerSet;
import com.tencent.trpc.registry.task.AbstractRetryTask;
import com.tencent.trpc.registry.task.RetryNotifyTask;
import com.tencent.trpc.registry.task.RetryRegisterTask;
import com.tencent.trpc.registry.task.RetrySubscribeTask;
import com.tencent.trpc.registry.task.RetryUnregisterTask;
import com.tencent.trpc.registry.task.RetryUnsubscribeTask;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

/**
 * An abstract class for registry center that encapsulates the operation of failure retry.
 * <p>Based on {@link AbstractRegistryCenter}, this class encapsulates the logic of failure retry.
 * Subclasses can directly inherit and implement the corresponding methods.
 * The underlying uses the HashedWheelTimer time wheel algorithm to schedule retry tasks efficiently using threads, but
 * the accuracy is not very high.
 * The default retry interval is {@link ConfigConstants#DEFAULT_REGISTRY_CENTER_RETRY_PERIOD_MS}.
 * Here, local optimization is done. Considering that a large number of retries of multiple services in a short period
 * of time may put pressure on the registry center,
 * a random number is added, and the actual retry time is: retry interval + random.nextInt(retry interval) ms.
 * Later, it can be considered to continuously double the retry time when multiple failures reach the threshold within a
 * certain period of time.</p>
 * <p>Interfaces that subclasses must implement:</p>
 * <pre>1. {@link #init()}: Initialization interface</pre>
 * <pre>2. {@link #doRegister(RegisterInfo)}: Interface for registering services</pre>
 * <pre>3. {@link #doUnregister(RegisterInfo)}: Interface for unregistering services</pre>
 * <pre>4. {@link #subscribe(RegisterInfo, NotifyListener)}: Interface for subscribing to services</pre>
 * <pre>5. {@link #unsubscribe(RegisterInfo, NotifyListener)}: Interface for unsubscribing from services</pre>
 */
public abstract class AbstractFailedRetryRegistryCenter extends AbstractRegistryCenter {

    private static final Logger logger = LoggerFactory.getLogger(AbstractFailedRetryRegistryCenter.class);

    /**
     * Cache for retry tasks.
     */
    private final ConcurrentMap<RegisterInfoListenerHolder, AbstractRetryTask> failedTasks = new ConcurrentHashMap<>();

    /**
     * Task scheduler based on time wheel.
     */
    private HashedWheelTimer retryTimer;

    /**
     * Random interval for task retry interval.
     */
    private Random random = new Random();

    @Override
    public void setPluginConfig(PluginConfig pluginConfig) throws TRpcExtensionException {
        super.setPluginConfig(pluginConfig);
        this.retryTimer = new HashedWheelTimer(
                new NamedThreadFactory("TrpcFailedRegistryRetryTimer", true),
                this.config.getRetryPeriod(), TimeUnit.MILLISECONDS, TICKS_PER_WHEEL);
    }

    /**
     * Register the service and remove the retry task from the failure retry task.
     *
     * @param registerInfo The service to be registered.
     */
    @Override
    public void register(RegisterInfo registerInfo) {
        super.register(registerInfo);

        removeFailedRegisteredTask(registerInfo);
        removeFailedUnregisteredTask(registerInfo);

        try {
            doRegister(registerInfo);
        } catch (Exception e) {
            logger.warn("Failed to register registerInfo: {}, and waiting to retry. Cause: ", registerInfo, e);
            addFailedRegisteredTask(registerInfo);
        }
    }

    /**
     * Unregister the service and remove the retry task from the failure retry task.
     *
     * @param registerInfo The service to be unregistered.
     */
    @Override
    public void unregister(RegisterInfo registerInfo) {
        super.unregister(registerInfo);

        removeFailedRegisteredTask(registerInfo);
        removeFailedUnregisteredTask(registerInfo);

        try {
            doUnregister(registerInfo);
        } catch (Exception e) {
            logger.warn("Failed to unregister registerInfo: {}, and waiting to retry. Cause: ", registerInfo, e);
            addFailedUnregisteredTask(registerInfo);
        }
    }

    /**
     * Subscribe to the service and remove it from the failure retry task.
     * If the operation fails here, it will try to read the subscribed data from the cached data.
     *
     * @param registerInfo The service to be subscribed.
     * @param notifyListener The listener for the service operation.
     */
    @Override
    public void subscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {
        super.subscribe(registerInfo, notifyListener);

        removeFailedSubscribedTask(registerInfo, notifyListener);
        removeFailedUnsubscribedTask(registerInfo, notifyListener);
        removeFailedNotifyTask(registerInfo, notifyListener);

        try {
            doSubscribe(registerInfo, notifyListener);
        } catch (Exception e) {
            // Read cached data.
            List<RegisterInfo> registerInfos = cache.getRegisterInfos(registerInfo.getServiceName());
            if (!registerInfos.isEmpty()) {
                notify(registerInfo, notifyListener, registerInfos);
                logger.warn("Failed to subscribe registerInfo: {}, notifyListener: {}, and using cache: {}, Cause: ",
                        registerInfo, notifyListener, registerInfos, e);
            } else {
                logger.warn("Failed to subscribe registerInfo: {}, notifyListener: {}, and waiting to retry. Cause: ",
                        registerInfo, notifyListener, e);
            }
            addFailedSubscribedTask(registerInfo, notifyListener);
        }
    }

    /**
     * Unsubscribe from the service and remove it from the failure retry task.
     *
     * @param registerInfo The service to be unsubscribed.
     * @param notifyListener The listener for the service operation.
     */
    @Override
    public void unsubscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {
        super.unsubscribe(registerInfo, notifyListener);

        removeFailedSubscribedTask(registerInfo, notifyListener);
        removeFailedUnsubscribedTask(registerInfo, notifyListener);
        removeFailedNotifyTask(registerInfo, notifyListener);

        try {
            doUnsubscribe(registerInfo, notifyListener);
        } catch (Exception e) {
            logger.warn("Failed to unsubscribe registerInfo: {}, notifyListener: {}, and waiting to retry. Cause: ",
                    registerInfo, notifyListener, e);
            addFailedUnsubscribedTask(registerInfo, notifyListener);
        }
    }

    /**
     * Callback interface for service subscription when data changes.
     *
     * @param registerInfo The subscribed service.
     * @param notifyListener The listener for the service.
     * @param updatingRegisterInfos The data updated by the subscribed service.
     */
    @Override
    public void notify(RegisterInfo registerInfo, NotifyListener notifyListener,
            List<RegisterInfo> updatingRegisterInfos) {
        Objects.requireNonNull(registerInfo, "registerInfo can not be null");
        Objects.requireNonNull(notifyListener, "notifyListener can not be null");
        Objects.requireNonNull(updatingRegisterInfos, "updatingRegisterInfos can not be null");

        try {
            doNotify(registerInfo, notifyListener, updatingRegisterInfos);
        } catch (Exception e) {
            logger.warn("Failed to notify registerInfo: {}, notifyListener: {}, and waiting to retry. Cause: ",
                    registerInfo, notifyListener, e);
            addFailedNotifiedTask(registerInfo, notifyListener, updatingRegisterInfos);
        }
    }


    /**
     * Add a retry task for registering the service.
     *
     * @param registerInfo The service to be operated on.
     */
    public void addFailedRegisteredTask(RegisterInfo registerInfo) {
        this.addFailedTask(new RegisterInfoListenerHolder(registerInfo, null),
                new RetryRegisterTask(this, registerInfo));
    }

    /**
     * Remove the retry task for registering the service.
     *
     * @param registerInfo The service to be operated on.
     */
    public void removeFailedRegisteredTask(RegisterInfo registerInfo) {
        this.removeFailedTask(new RegisterInfoListenerHolder(registerInfo, null));
    }

    /**
     * Add a retry task for unregistering the service.
     *
     * @param registerInfo The service to be operated on.
     */
    public void addFailedUnregisteredTask(RegisterInfo registerInfo) {
        this.addFailedTask(new RegisterInfoListenerHolder(registerInfo, null),
                new RetryUnregisterTask(this, registerInfo));
    }

    /**
     * Remove the retry task for unregistering the service.
     *
     * @param registerInfo The service to be operated on.
     */
    public void removeFailedUnregisteredTask(RegisterInfo registerInfo) {
        this.removeFailedTask(new RegisterInfoListenerHolder(registerInfo, null));
    }

    /**
     * Add a retry task for subscribing to the service.
     *
     * @param registerInfo The service to be operated on.
     * @param notifyListener The listener for the service operation.
     */
    public void addFailedSubscribedTask(RegisterInfo registerInfo, NotifyListener notifyListener) {
        this.addFailedTask(new RegisterInfoListenerHolder(registerInfo, notifyListener),
                new RetrySubscribeTask(this, registerInfo, notifyListener));
    }

    /**
     * Remove the retry task for subscribing to the service.
     *
     * @param registerInfo The service to be operated on.
     * @param notifyListener The listener for the service operation.
     */
    public void removeFailedSubscribedTask(RegisterInfo registerInfo, NotifyListener notifyListener) {
        this.removeFailedTask(new RegisterInfoListenerHolder(registerInfo, notifyListener));
    }

    /**
     * Add a retry task for unsubscribing from the service.
     *
     * @param registerInfo The service to be operated on.
     * @param notifyListener The listener for the service operation.
     */
    public void addFailedUnsubscribedTask(RegisterInfo registerInfo, NotifyListener notifyListener) {
        this.addFailedTask(new RegisterInfoListenerHolder(registerInfo, notifyListener),
                new RetryUnsubscribeTask(this, registerInfo, notifyListener));
    }

    /**
     * Remove the retry task for unsubscribing from the service.
     *
     * @param registerInfo The service to be operated on.
     * @param notifyListener The listener for the service operation.
     */
    public void removeFailedUnsubscribedTask(RegisterInfo registerInfo, NotifyListener notifyListener) {
        this.removeFailedTask(new RegisterInfoListenerHolder(registerInfo, notifyListener));
    }

    /**
     * Add a retry task for notifying the callback of a subscribed service.
     *
     * @param registerInfo The subscribed service.
     * @param notifyListener The listener for the service operation.
     * @param updatingRegisterInfos The data that needs to be updated for the subscribed service.
     */
    public void addFailedNotifiedTask(RegisterInfo registerInfo, NotifyListener notifyListener,
            List<RegisterInfo> updatingRegisterInfos) {
        RegisterInfoListenerHolder holder = new RegisterInfoListenerHolder(registerInfo, notifyListener);
        RetryNotifyTask newTask = new RetryNotifyTask(this, registerInfo, notifyListener);
        newTask.addRegisterInfoToRetry(updatingRegisterInfos);
        RetryNotifyTask oldTask = (RetryNotifyTask) failedTasks.putIfAbsent(holder, newTask);
        if (oldTask == null) {
            retryTimer.newTimeout(newTask, this.config.getRetryPeriod(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Remove the retry task for notifying the callback of a subscribed service.
     *
     * @param registerInfo The service to be operated on.
     * @param notifyListener The listener for the service operation.
     */
    public void removeFailedNotifyTask(RegisterInfo registerInfo, NotifyListener notifyListener) {
        this.removeFailedTask(new RegisterInfoListenerHolder(registerInfo, notifyListener));
    }

    /**
     * Unsubscribe and unregister all services. Automatically called when the framework exits.
     */
    @Override
    public void destroy() {
        super.destroy();
        retryTimer.stop();
    }

    /**
     * Get the actual retry time for a retry task. Add or subtract a random number to avoid putting too much pressure on
     * the registry center when multiple services retry frequently in a short period of time.
     *
     * @return The retry time for the retry task.
     */
    public int getRealRetryPeriod() {
        return this.config.getRetryPeriod() + this.random.nextInt(this.config.getRetryPeriod());
    }

    public ConcurrentMap<RegisterInfoListenerHolder, AbstractRetryTask> getFailedTasks() {
        return failedTasks;
    }

    /**
     * Service registration operation that needs to be implemented by subclasses.
     *
     * @param registerInfo The service to be registered.
     */
    public abstract void doRegister(RegisterInfo registerInfo);

    /**
     * Service unregistration operation that needs to be implemented by subclasses.
     *
     * @param registerInfo The service to be unregistered.
     */
    public abstract void doUnregister(RegisterInfo registerInfo);

    /**
     * Service subscription operation that needs to be implemented by subclasses.
     *
     * @param registerInfo The service to be subscribed.
     * @param notifyListener The listener for the service.
     */
    public abstract void doSubscribe(RegisterInfo registerInfo, NotifyListener notifyListener);

    /**
     * Service unsubscription operation that needs to be implemented by subclasses.
     *
     * @param registerInfo The service to be unsubscribed.
     * @param notifyListener The listener for the service.
     */
    public abstract void doUnsubscribe(RegisterInfo registerInfo, NotifyListener notifyListener);

    /**
     * Callback interface for service subscription when data changes. Generally, it does not need to be overridden.
     *
     * @param registerInfo The subscribed service.
     * @param notifyListener The listener for the service.
     * @param updatingRegisterInfos The updated data for the subscribed service.
     */
    public void doNotify(RegisterInfo registerInfo, NotifyListener notifyListener,
            List<RegisterInfo> updatingRegisterInfos) {
        super.notify(registerInfo, notifyListener, updatingRegisterInfos);
    }

    /**
     * Restore registered services.
     */
    @Override
    protected void recoverRegistered() {
        Set<RegisterInfo> recoverRegistered = getRegisteredRegisterInfos();
        if (CollectionUtils.isEmpty(recoverRegistered)) {
            return;
        }
        recoverRegistered.forEach(registerInfo -> {
            logger.debug("[Recover] Register registerInfo: {}", registerInfo);
            addFailedRegisteredTask(registerInfo);
        });
    }

    /**
     * Restore subscribed services.
     */
    @Override
    protected void recoverSubscribed() {
        Map<RegisterInfo, RegistryCenterListenerSet> recoverSubscribed = getSubscribedRegisterInfos();
        if (MapUtils.isEmpty(recoverSubscribed)) {
            return;
        }
        recoverSubscribed.forEach((registerInfo, registryCenterListenerSet) ->
                registryCenterListenerSet.getNotifyListeners().forEach(notifyListener -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("[Recover] Subscribe registerInfo: {}, listener: {}",
                                registerInfo, notifyListener);
                    }
                    addFailedSubscribedTask(registerInfo, notifyListener);
                })
        );
    }

    /**
     * Add a retry task for use when subscribing/unsubscribing. When the task does not exist in the map, create a new
     * task and start it.
     *
     * @param registerInfoListenerHolder The service and its listener for the operation.
     * @param newTask The retry task.
     */
    private void addFailedTask(RegisterInfoListenerHolder registerInfoListenerHolder, AbstractRetryTask newTask) {
        AbstractRetryTask oldTask = failedTasks.get(registerInfoListenerHolder);
        if (oldTask != null) {
            return;
        }
        oldTask = failedTasks.putIfAbsent(registerInfoListenerHolder, newTask);
        if (oldTask == null) {
            retryTimer.newTimeout(newTask, this.getRealRetryPeriod(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Remove a retry task for use when registering/unsubscribing.
     *
     * @param registerInfoListenerHolder The service and its listener for the operation.
     */
    private void removeFailedTask(RegisterInfoListenerHolder registerInfoListenerHolder) {
        AbstractRetryTask abstractRetryTask = failedTasks.remove(registerInfoListenerHolder);
        if (abstractRetryTask != null) {
            abstractRetryTask.cancel();
        }
    }

    /**
     * Binding holder for the service and its listener for subscription, only for binding relationship management. It
     * has no practical use.
     */
    static class RegisterInfoListenerHolder {

        private final RegisterInfo registerInfo;

        private final NotifyListener notifyListener;

        RegisterInfoListenerHolder(RegisterInfo url, NotifyListener notifyListener) {
            Objects.requireNonNull(url, "url of RegisterInfo is null");

            this.registerInfo = url;
            this.notifyListener = notifyListener;
        }

        public RegisterInfo getRegisterInfo() {
            return registerInfo;
        }

        public NotifyListener getNotifyListener() {
            return notifyListener;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = result * prime + registerInfo.hashCode();
            result = result * prime + ((null == notifyListener) ? 0 : notifyListener.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RegisterInfoListenerHolder) {
                RegisterInfoListenerHolder h = (RegisterInfoListenerHolder) obj;
                if (this.notifyListener == null && h.notifyListener == null) {
                    return this.registerInfo.equals(h.registerInfo);
                }
                if (this.notifyListener != null && h.notifyListener != null) {
                    return this.registerInfo.equals(h.registerInfo) && this.notifyListener
                            .equals(h.notifyListener);
                }
            }
            return false;
        }
    }

}
