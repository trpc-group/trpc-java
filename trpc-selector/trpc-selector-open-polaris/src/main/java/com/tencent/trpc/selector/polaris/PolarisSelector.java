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

package com.tencent.trpc.selector.polaris;

import com.google.common.collect.Lists;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.loadbalance.LoadBalancer;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.Criteria;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.GetOneInstanceRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.APIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataProvider;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.impl.MetadataContainerImpl;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.polaris.metadata.core.manager.MetadataContextHolder;
import com.tencent.polaris.plugins.loadbalancer.random.WeightedRandomBalance;
import com.tencent.polaris.threadlocal.cross.ExecutorWrapper;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.configcenter.ConfigurationManager;
import com.tencent.trpc.core.configcenter.spi.ConfigurationLoader;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.DisposableExtension;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.extension.InitializingExtension;
import com.tencent.trpc.core.extension.PluginConfigAware;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.spi.Selector;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.core.utils.StringUtils;
import com.tencent.trpc.polaris.common.PolarisConstant;
import com.tencent.trpc.polaris.common.PolarisFutureUtil;
import com.tencent.trpc.polaris.common.PolarisTrans;
import com.tencent.trpc.proto.http.common.HttpConstants;
import com.tencent.trpc.selector.polaris.common.PolarisCommon;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Base open source polaris selector
 */
@Extension(PolarisSelector.NAME)
public class PolarisSelector implements Selector, PluginConfigAware, InitializingExtension, DisposableExtension {

    public static final String NAME = "polaris";

    private static final Logger logger = LoggerFactory.getLogger(PolarisSelector.class);

    private static final LoadBalancer loadBalancer = new WeightedRandomBalance();
    private PolarisSelectorConfig selectorConfig;

    private ConsumerAPI polarisAPI;

    private PluginConfig pluginConfig;

    public PolarisSelector() {

    }

    /**
     * In most cases, it is not necessary to have multiple instances of Polaris in one process
     *
     * Obtain the Polaris instance in TRPC
     *
     * @return Polaris instances
     */
    public ConsumerAPI getPolarisAPI() {
        return polarisAPI;
    }

    @Override
    public void setPluginConfig(PluginConfig pluginConfig) throws TRpcExtensionException {
        this.pluginConfig = pluginConfig;
        this.selectorConfig = PolarisSelectorConfig.parse(pluginConfig);
    }

    @Override
    public void init() throws TRpcExtensionException {
        Objects.requireNonNull(pluginConfig, "pluginConfig");
        try {
            Configuration polarisConfig = genPolarisConfig(selectorConfig);
            SDKContext sdkContext = APIFactory.initContextByConfig(polarisConfig);
            sdkContext.init();
            polarisAPI = APIFactory.createConsumerAPIByContext(sdkContext);
        } catch (PolarisException e) {
            throw TRpcException.trans(e);
        }
    }


    private Configuration genPolarisConfig(PolarisSelectorConfig selectorConfig) {
        Map<String, Object> extMap = selectorConfig.getExtMap();
        ConfigurationImpl configuration;

        if (extMap == null) {
            configuration = new ConfigurationImpl();
            configuration.setDefault();
        } else {
            configuration = PolarisCommon.genPolarisConfiguration(extMap);
        }
        return configuration;
    }

    @Override
    public void warmup(ServiceId serviceId) {
        GetInstancesRequest req = createSelectAllReq(serviceId, false);
        try {
            PolarisFutureUtil.toCompletableFuture(polarisAPI.asyncGetInstances(req),
                            selectorConfig.getWorkerPool().toExecutor())
                    .thenApply(res -> {
                        logger.debug("[selector] warmup polaris asyncSelectAll return success:{}",
                                res);
                        if (res == null || res.getInstances() == null
                                || res.getInstances().length == 0) {
                            return Collections.emptyList();
                        }
                        return Arrays.stream(res.getInstances())
                                .map(s -> PolarisTrans.toServiceInstance(res, s))
                                .collect(Collectors.toList());
                    }).toCompletableFuture().join();
        } catch (Exception e) {
            throw TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_ROUTER_ERR, "call polaris error", e);
        }
    }

    @Override
    public CompletionStage<ServiceInstance> asyncSelectOne(ServiceId serviceId, Request request) {
        GetOneInstanceRequest req = new GetOneInstanceRequest();
        String namespace = serviceId.getParameter(PolarisConstant.TrpcPolarisParams.NAMESPACE_KEY.getKey());
        if (namespace != null) {
            req.setNamespace(namespace);
        }
        req.setService(serviceId.getServiceName());
        req.setCriteria(getCriteria(request));
        req.setMetadata(PolarisTrans.trans2PolarisMetadata(serviceId.getParameters()));
        // ServiceInfo assignment, used to support request matching rules for rule routing
        req.setServiceInfo(PolarisTrans.getPolarisServiceInfo(pluginConfig.getProperties(), serviceId, request));
        req.setExternalParameterSupplier(key -> {
            String configCenter = ConfigManager.getInstance().getServerConfig()
                    .getConfigCenter();
            if (StringUtils.isEmpty(configCenter)) {
                return Optional.empty();
            }
            ConfigurationLoader loader = ConfigurationManager.getConfigurationLoader(configCenter);
            return Optional.ofNullable(loader.getValue(key, ConfigManager.getInstance().getServerConfig().getApp()));
        });
        logger.debug("[asyncSelectOne] GetOneInstanceRequest:{}", req);

        try {
            MetadataContext metadataContext = buildCalleeMetadataManager(request);
            MetadataContextHolder.set(metadataContext);
            Executor executor = new ExecutorWrapper<>(selectorConfig.getWorkerPool().toExecutor(), () -> metadataContext, s -> {
            });
            CompletableFuture<InstancesResponse> future = CompletableFuture.supplyAsync(() -> polarisAPI.getOneInstance(req), executor);
            return future.thenCompose(res -> {
                if (res != null && res.getInstances() != null && res.getInstances().length > 0) {
                    logger.debug("[selector] selector asyncSelectOne ServiceId:{} return success:{}",
                            serviceId, res.getInstances()[0]);
                    return CompletableFuture.completedFuture(
                            PolarisTrans.toServiceInstance(res, res.getInstances()[0]));
                }
                // Execute fallback logic: Identify all nodes (including unhealthy ones) and perform load balancing
                return selectOneFallback(serviceId, request);
            });
        } catch (Exception e) {
            throw TRpcException
                    .newFrameException(ErrorCode.TRPC_CLIENT_ROUTER_ERR, "call polaris error",
                            e);
        } finally {
            MetadataContextHolder.remove();
        }
    }

    @Override
    public CompletionStage<List<ServiceInstance>> asyncSelectAll(ServiceId serviceId,
            Request request) {
        GetInstancesRequest req = createSelectAllReq(serviceId, request, false);
        logger.debug("[asyncSelectAll] GetInstancesRequest:{}", req);
        try {
            return PolarisFutureUtil.toCompletableFuture(polarisAPI.asyncGetInstances(req),
                            selectorConfig.getWorkerPool().toExecutor())
                    .thenApply(res -> {
                        logger.debug("[selector] polaris asyncSelectAll return success:{}", res);
                        if (res == null || res.getInstances() == null
                                || res.getInstances().length == 0) {
                            return Collections.emptyList();
                        }
                        return Arrays.stream(res.getInstances())
                                .map(s -> PolarisTrans.toServiceInstance(res, s))
                                .collect(Collectors.toList());
                    });
        } catch (Exception e) {
            throw TRpcException
                    .newFrameException(ErrorCode.TRPC_CLIENT_ROUTER_ERR, "call polaris error",
                            e);
        }
    }

    @Override
    public void report(ServiceInstance serviceInstance, int code, long costMs)
            throws TRpcException {
        PolarisCommon.report(polarisAPI, serviceInstance, code, costMs);
    }

    /**
     * When Polaris obtains a single node and returns null,
     * it is possible that the polaris registration center's heartbeat has failed,
     * while the service node is normal. At this time, all nodes (including those with unhealthy heartbeat) are pulled,
     * and then load balancing is performed to return
     */
    private CompletableFuture<ServiceInstance> selectOneFallback(ServiceId serviceId,
            Request request) {
        logger.debug("[selector] selectOneFallback call for ServiceId:{}", serviceId);
        GetInstancesRequest req = createSelectAllReq(serviceId, request, true);
        try {
            return PolarisFutureUtil.toCompletableFuture(polarisAPI.asyncGetInstances(req),
                            selectorConfig.getWorkerPool().toExecutor())
                    .thenApply(res -> {
                        if (res == null || res.getInstances() == null
                                || res.getInstances().length == 0) {
                            return null;
                        }
                        return doLoadBalance(res, request);
                    });
        } catch (Exception e) {
            throw TRpcException
                    .newFrameException(ErrorCode.TRPC_CLIENT_ROUTER_ERR, "call polaris error", e);
        }
    }

    private Criteria createCriteria(Request request) {
        if (!loadBalancer.getName().equals(LoadBalanceConfig.LOAD_BALANCE_RING_HASH)) {
            return null;
        }
        String hashVal = request.getMeta().getHashVal();
        if (hashVal == null) {
            throw TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_ROUTER_ERR,
                    "hashVal must not be null when use load balance:" + selectorConfig
                            .getLoadbalance());
        }

        Criteria criteria = new Criteria();
        criteria.setHashKey(hashVal);
        return criteria;
    }

    private GetInstancesRequest createSelectAllReq(ServiceId serviceId, Request request, boolean includeUnhealthy) {
        GetInstancesRequest req = new GetInstancesRequest();
        String namespace = serviceId.getParameter(PolarisConstant.TrpcPolarisParams.NAMESPACE_KEY.getKey());
        if (namespace != null) {
            req.setNamespace(namespace);
        }
        req.setService(serviceId.getServiceName());
        req.setMetadata(PolarisTrans.trans2PolarisMetadata(serviceId.getParameters()));
        req.setIncludeUnhealthy(includeUnhealthy);
        req.setIncludeCircuitBreak(false);
        // ServiceInfo assignment, used to support request matching rules for rule routing
        req.setServiceInfo(PolarisTrans.getPolarisServiceInfo(pluginConfig.getProperties(), serviceId, request));
        return req;
    }

    private GetInstancesRequest createSelectAllReq(ServiceId serviceId, boolean includeUnhealthy) {
        return createSelectAllReq(serviceId, null, includeUnhealthy);
    }

    private ServiceInstance doLoadBalance(InstancesResponse response, Request request) {
        try {
            Instance[] instances = response.getInstances();

            ServiceInstances serviceInstances =
                    PolarisCommon.createServiceInstances(Lists.newArrayList(instances));
            Instance instance = loadBalancer
                    .chooseInstance(createCriteria(request), serviceInstances);

            return PolarisTrans.toServiceInstance(response, instance);
        } catch (PolarisException e) {
            throw TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_ROUTER_ERR,
                    "polaris selector fallback do load balance error", e);
        }
    }

    /**
     * 1.If hashVal is set, consistent hashing is used
     * 2.If set to consistent hash, but no hashVal error is reported
     * 3.Hash algorithm for setting
     */
    private Criteria getCriteria(Request request) {
        String hashVal = request.getMeta().getHashVal();
        if (hashVal != null) {
            Criteria criteria = new Criteria();
            criteria.setHashKey(hashVal);
            return criteria;
        }
        return null;
    }

    @Override
    public void destroy() throws TRpcExtensionException {
        if (null != polarisAPI) {
            polarisAPI.destroy();
        }
    }

    private static MetadataContext buildCalleeMetadataManager(Request request) {
        MetadataContext manager = RpcContextUtils.getValueMapValue(request.getContext(), PolarisConstant.RPC_CONTEXT_POALRIS_METADATA);
        if (Objects.isNull(manager)) {
            manager = new MetadataContext(MetadataContext.DEFAULT_TRANSITIVE_PREFIX, true);
            RpcContextUtils.putValueMapValue(request.getContext(), PolarisConstant.RPC_CONTEXT_POALRIS_METADATA, manager);
        }
        MetadataContainerImpl calleeContainer = manager.getMetadataContainer(MetadataType.MESSAGE, false);
        calleeContainer.setMetadataProvider(new MetadataProvider() {
            @Override
            public String getRawMetadataStringValue(String key) {
                key = key.toLowerCase();
                switch (key) {
                    case MessageMetadataContainer.LABEL_KEY_CALLER_IP:
                        return request.getMeta().getRemoteAddress().getHostName();
                    case MessageMetadataContainer.LABEL_KEY_METHOD:
                        return request.getMeta().getCallInfo().getCalleeMethod();
                    case MessageMetadataContainer.LABEL_KEY_PATH:
                        return request.getInvocation().getRpcMethodInfo().getServiceInterface().getCanonicalName();
                }
                return null;
            }

            @Override
            public String getRawMetadataMapValue(String key, String mapKey) {
                key = key.toLowerCase();
                if (key.equals(MessageMetadataContainer.LABEL_MAP_KEY_HEADER)) {
                    Map<String, Object> attach = request.getContext().getReqAttachMap();
                    if (attach.containsKey("headers")) {
                        // processing scg scenes
                        Object springHeaders = attach.get("headers");
                        try {
                            Method method = springHeaders.getClass().getMethod("getFirst");
                            Object val = method.invoke(springHeaders, mapKey);
                            return val == null ? null : String.valueOf(val);
                        } catch (Exception e) {
                            logger.error("[selector] get raw metadata from tRPC context fail, key:{}", mapKey, e);
                            return null;
                        }
                    }
                    if (attach.containsKey(HttpConstants.TRPC_ATTACH_SERVLET_REQUEST)) {
                        // processing tRPC http protocol
                        HttpServletRequest servletRequest = (HttpServletRequest) attach.get(HttpConstants.TRPC_ATTACH_SERVLET_REQUEST);
                        return servletRequest.getHeader(mapKey);
                    }

                    Object val = attach.get(mapKey);
                    return val == null ? null : String.valueOf(val);
                }
                return null;
            }
        });
        RpcContextUtils.putValueMapValue(request.getContext(), PolarisConstant.RPC_CONTEXT_POALRIS_METADATA, manager);
        return manager;
    }
}
