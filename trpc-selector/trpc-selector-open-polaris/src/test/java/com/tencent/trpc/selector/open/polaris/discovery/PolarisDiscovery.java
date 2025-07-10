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

package com.tencent.trpc.selector.open.polaris.discovery;

import static com.tencent.polaris.api.exception.ErrorCode.PLUGIN_ERROR;
import static com.tencent.trpc.polaris.common.PolarisConstant.NAMESPACE_DIFF_ALLOWED;
import static com.tencent.trpc.polaris.common.PolarisConstant.NAMESPACE_DIFF_ALLOWED_DEFAULT;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesFuture;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.factory.api.APIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.DisposableExtension;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.extension.InitializingExtension;
import com.tencent.trpc.core.extension.PluginConfigAware;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.discovery.AbstractDiscovery;
import com.tencent.trpc.polaris.common.PolarisConstant;
import com.tencent.trpc.polaris.common.PolarisFutureUtil;
import com.tencent.trpc.polaris.common.PolarisTrans;
import com.tencent.trpc.selector.polaris.common.PolarisCommon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * Implementation of Service Discovery Based on Polaris
 */
@Extension(PolarisDiscovery.NAME)
public class PolarisDiscovery extends AbstractDiscovery implements PluginConfigAware, InitializingExtension,
        DisposableExtension {

    public static final String NAME = "polaris";
    private final Logger logger = LoggerFactory.getLogger(PolarisDiscovery.class);
    private boolean namespaceDiffAllowed;

    /**
     * Polaris Service Discovery api
     */
    private ConsumerAPI consumerApi;

    private PluginConfig config;


    public PolarisDiscovery() {

    }

    /**
     * Injecting plugin configuration will be called when the plugin is load
     */
    @Override
    public void setPluginConfig(PluginConfig pluginConfig) throws TRpcExtensionException {
        this.config = pluginConfig;
    }

    /**
     * Plugin initialization will be called during the container startup process and during plugin initialization
     */
    @Override
    public void init() throws TRpcExtensionException {
        Objects.requireNonNull(config, "discovery config can not be null");
        Map<String, Object> extMap = config.getProperties();

        try {
            this.namespaceDiffAllowed = MapUtils.getBooleanValue(extMap, NAMESPACE_DIFF_ALLOWED,
                    NAMESPACE_DIFF_ALLOWED_DEFAULT);

            ConfigurationImpl configuration = PolarisCommon.genPolarisConfiguration(extMap);
            //Without configuring service routing, do not configure routing.
            // Polaris SDK has modified the semantics of consumerApi. getInstances after 3.0.9 to default with routing
            logger.info("[PolarisDiscovery init] extMap:{}", extMap);
            updateRouterChain(configuration, extMap);
            consumerApi = APIFactory.createConsumerAPIByConfig(configuration);
        } catch (PolarisException e) {
            logger.error("[discovery init] failed to create consumerApi.");
            throw new IllegalStateException("failed to create consumerApi with config:" + extMap, e);
        }
    }

    private void updateRouterChain(ConfigurationImpl configuration, Map<String, Object> extMap) {
        Map<String, Object> consumers = (Map<String, Object>) MapUtils.getMap(extMap,
                PolarisConstant.POLARIS_CONSUMER_KEY, Maps.newHashMap());
        Map<String, Object> serviceRouterConfig = (Map<String, Object>) MapUtils.getMap(consumers,
                "serviceRouter", Maps.newHashMap());
        if (serviceRouterConfig == null) {
            configuration.getConsumer().getServiceRouter()
                    .setChain(Arrays.asList(ServiceRouterConfig.DEFAULT_ROUTER_ISOLATED));
            configuration.getConsumer().getServiceRouter()
                    .setAfterChain(Arrays.asList(ServiceRouterConfig.DEFAULT_ROUTER_ISOLATED));
        }
    }


    @Override
    public List<ServiceInstance> list(ServiceId serviceId) {
        // Verify if the namespaces are consistent
        checkNamespace(serviceId);
        // Building Polaris Service Discovery Request Object GetInstancesRequest
        GetInstancesRequest request = buildGetInstanceRequest(serviceId);
        try {
            InstancesResponse response = consumerApi.getInstances(request);
            if (response == null) {
                logger.error("getInstances return null by {} ", serviceId);
                return Lists.newArrayList();
            }
            Instance[] instances = response.getInstances();
            if (instances != null && instances.length > 0) {
                return transfer2ServiceInstance(response);
            }
            return Lists.newArrayList();
        } catch (Throwable e) {
            logger.error("[list] failed to list instances from :{} by {} ", config, serviceId);
            throw new PolarisException(PLUGIN_ERROR,
                    "failed to list instances from :" + config + " by " + serviceId + " ,cause:" + e.getMessage(), e);
        }
    }


    @Override
    public CompletionStage<List<ServiceInstance>> asyncList(ServiceId serviceId, Executor executor) {
        checkNamespace(serviceId);
        // Building Polaris Service Discovery Request Object GetInstancesRequest
        GetInstancesRequest request = buildGetInstanceRequest(serviceId);
        try {
            InstancesFuture instancesFuture = consumerApi.asyncGetInstances(request);
            return PolarisFutureUtil.toCompletableFuture(instancesFuture, executor).thenApply(res -> {
                if (res == null || res.getInstances() == null || res.getInstances().length == 0) {
                    return Lists.newArrayList();
                } else {
                    return transfer2ServiceInstance(res);
                }
            });
        } catch (Throwable e) {
            logger.error("[asyncList] failed to list instances from :{} by {} ", config, serviceId);
            throw new PolarisException(PLUGIN_ERROR,
                    "failed to list instances from :" + config + " by " + serviceId + " ,cause:" + e.getMessage(), e);
        }
    }

    /**
     * Verify whether cross environment calls are allowed
     */
    private void checkNamespace(ServiceId serviceId) {
        // callee namespace
        String namespace = serviceId.getParameter(PolarisConstant.TrpcPolarisParams.NAMESPACE_KEY.getKey());
        // calling namespace
        String invokerNamespace = ConfigManager.getInstance().getGlobalConfig().getNamespace();
        if (namespace == null) {
            throw new IllegalStateException("[polaris discovery] namespace can not be null.");
        }
        if (!namespace.equals(invokerNamespace) && !namespaceDiffAllowed) {
            throw new IllegalStateException("[polaris discovery] namespace can not be different.");
        }
    }

    /**
     * Convert the Polaris service discovery response to a TRPC service instance
     */
    private List<ServiceInstance> transfer2ServiceInstance(InstancesResponse response) {
        List<ServiceInstance> serviceInstances = new ArrayList<>();
        Instance[] instances = response.getInstances();
        for (Instance instance : instances) {
            ServiceInstance serviceInstance = PolarisTrans.toServiceInstance(response, instance);
            serviceInstances.add(serviceInstance);
        }
        return serviceInstances;
    }

    /**
     * Building Polaris Service Discovery Request Object GetInstancesRequest
     */
    private GetInstancesRequest buildGetInstanceRequest(ServiceId serviceId) {
        GetInstancesRequest getInstancesRequest = new GetInstancesRequest();
        getInstancesRequest.setService(serviceId.getServiceName());
        getInstancesRequest.setNamespace(serviceId
                .getParameter(PolarisConstant.TrpcPolarisParams.NAMESPACE_KEY.getKey()));
        // Does it contain unhealthy nodes
        boolean includeUnhealthy = serviceId
                .getParameter(PolarisConstant.TrpcPolarisParams.INCLUDE_UNHEALTHY.getKey(), false);
        getInstancesRequest.setIncludeUnhealthy(includeUnhealthy);
        // Does it include circuitBreaker nodes
        boolean includeCircuitBreak = serviceId
                .getParameter(PolarisConstant.TrpcPolarisParams.INCLUDE_CIRCUITBREAK.getKey(), false);
        getInstancesRequest.setIncludeCircuitBreak(includeCircuitBreak);

        if (NumberUtils.isDigits(serviceId
                .getParameter(PolarisConstant.TrpcPolarisParams.TIMEOUT_PARAM_KEY.getKey()))) {
            getInstancesRequest.setTimeoutMs(
                    NumberUtils.createLong(serviceId
                            .getParameter(PolarisConstant.TrpcPolarisParams.TIMEOUT_PARAM_KEY.getKey())));
        }
        getInstancesRequest.setMetadata(PolarisTrans.trans2PolarisMetadata(serviceId.getParameters()));

        logger.debug("[buildGetInstanceRequest] {}", getInstancesRequest);
        return getInstancesRequest;
    }

    @Override
    public void destroy() throws TRpcExtensionException {
        if (null != consumerApi) {
            consumerApi.destroy();
        }
    }
}
