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

package com.tencent.trpc.registry.discovery;

import static com.tencent.trpc.registry.common.Constants.REGISTRY_CENTER_SERVICE_TYPE_KEY;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.discovery.AbstractDiscovery;
import com.tencent.trpc.core.utils.ConcurrentHashSet;
import com.tencent.trpc.core.utils.FutureUtils;
import com.tencent.trpc.core.utils.NetUtils;
import com.tencent.trpc.core.utils.StringUtils;
import com.tencent.trpc.registry.center.AbstractRegistryCenter;
import com.tencent.trpc.registry.center.NotifyListener;
import com.tencent.trpc.registry.center.RegistryCenter;
import com.tencent.trpc.registry.common.RegistryCenterEnum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

/**
 * Service registration and discovery listener.
 * <p>Encapsulates the discovery operation of the registry center and implements the {@link NotifyListener} notification
 * listener.</p>
 * <pre>After the client subscribes to the service, it will be notified to update the available service list when the
 * data changes, and it needs to be used with the selector module.</pre>
 * <pre>Each subscription operation will bind RegistryDiscovery and register itself to the consumers node of the
 * registry center, and then subscribe to the providers node. When the data under this node changes, it will be notified
 * to update the available service list of RegistryDiscovery.</pre>
 * <pre>When the service is destroyed, the available service list will be cleared.</pre>
 */
public class RegistryDiscovery extends AbstractDiscovery implements NotifyListener {

    /**
     * The subscribed service that is bound.
     */
    private final RegisterInfo registerInfo;

    /**
     * The list of available services.
     */
    private Set<ServiceInstance> availableServiceInstances = new ConcurrentHashSet<>();

    /**
     * The registry center.
     */
    private RegistryCenter registryCenter;

    /**
     * Register the subscriber itself to the consumers node and subscribe to the service.
     *
     * @param serviceId The service information to subscribe to.
     * @param registryCenter The registry center.
     */
    public RegistryDiscovery(ServiceId serviceId, RegistryCenter registryCenter) {
        Objects.requireNonNull(registryCenter);

        String host = ConfigManager.getInstance().getServerConfig().getLocalIp();
        if (StringUtils.isEmpty(host)) {
            host = NetUtils.LOCAL_HOST;
        }

        this.registerInfo = new RegisterInfo("trpc", host,
                0, serviceId.getGroup(), serviceId.getVersion(), serviceId.getServiceName());
        this.registryCenter = registryCenter;
        this.registerSelfToConsumers();
        this.subscribe();
    }

    /**
     * If the REGISTRY_CENTER_REGISTER_CONSUMER_KEY registration consumer function is enabled, register the consumer
     * itself to the consumers node.
     */
    protected void registerSelfToConsumers() {
        if (!(this.registryCenter instanceof AbstractRegistryCenter)
                || !((AbstractRegistryCenter) this.registryCenter).getRegistryCenterConfig().isRegisterConsumer()) {
            return;
        }
        RegisterInfo cloneRegisterInfo = registerInfo.clone();
        Map<String, Object> params = cloneRegisterInfo.getParameters();
        String serviceType = cloneRegisterInfo.getParameter(REGISTRY_CENTER_SERVICE_TYPE_KEY);
        if (StringUtils.isEmpty(serviceType)) {
            serviceType = RegistryCenterEnum.CONSUMERS.getType();
        } else if (!serviceType.contains(RegistryCenterEnum.CONSUMERS.getType())) {
            serviceType = String.format("%s,%s", serviceType, RegistryCenterEnum.CONSUMERS.getType());
        }
        params.put(REGISTRY_CENTER_SERVICE_TYPE_KEY, serviceType);
        registryCenter.register(cloneRegisterInfo);
    }

    /**
     * The callback interface when the subscribed service data changes. Currently, only the
     * {@link RegistryCenterEnum#PROVIDERS} type data is processed.
     *
     * @param registerInfos The subscribed data. When the data is empty, the availableServiceInstances will be
     *         cleared. It needs to be processed according to different data types {@link RegistryCenterEnum}.
     */
    @Override
    public void notify(List<RegisterInfo> registerInfos) {
        Set<ServiceInstance> tempServiceInstances = new ConcurrentHashSet<>();
        registerInfos.forEach(registerInfo -> {
            ServiceInstance serviceInstance = new ServiceInstance(registerInfo.getHost(),
                    registerInfo.getPort(), true);
            tempServiceInstances.add(serviceInstance);
        });
        this.availableServiceInstances = tempServiceInstances;
    }


    /**
     * Query all service providers for a service.
     *
     * @param serviceId The service to search for.
     * @return All providers for the serviceId service.
     */
    @Override
    public List<ServiceInstance> list(ServiceId serviceId) {
        if (!isValidServiceId(serviceId)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(availableServiceInstances);
    }

    /**
     * Asynchronously query all service providers for a service. Since the service providers are already cached, this
     * method only does simple wrapping and has no actual asynchronous operations.
     *
     * @param serviceId The service to search for.
     * @return All providers for the serviceId service.
     */
    @Override
    public CompletionStage<List<ServiceInstance>> asyncList(ServiceId serviceId,
            Executor executor) {
        CompletionStage<List<ServiceInstance>> future = FutureUtils.newFuture();
        future.toCompletableFuture().complete(list(serviceId));
        return future;
    }

    /**
     * Determine if it is the correct service.
     *
     * @param serviceId The service to operate on.
     */
    private boolean isValidServiceId(ServiceId serviceId) {
        return serviceId != null && serviceId.getServiceName()
                .equals(this.registerInfo.getServiceName());
    }

    /**
     * Get all available service providers in availableServiceInstances.
     */
    public Set<ServiceInstance> getServiceInstances() {
        return Collections.unmodifiableSet(availableServiceInstances);
    }


    /**
     * Clear all service providers in serviceInstances.
     */
    @Override
    public void destroy() throws TRpcExtensionException {
        unSubscribe();
        availableServiceInstances.clear();
    }

    /**
     * Subscribe to a service.
     */
    private void subscribe() {
        registryCenter.subscribe(registerInfo, this);
    }

    /**
     * Unsubscribe from a service.
     */
    private void unSubscribe() {
        registryCenter.unsubscribe(registerInfo, this);
    }
}
