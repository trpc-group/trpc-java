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

package com.tencent.trpc.selector.open.polaris;

import static com.tencent.trpc.polaris.common.PolarisConstant.POLARIS_CMDBCAMPUS;
import static com.tencent.trpc.polaris.common.PolarisConstant.POLARIS_CMDBREGION;
import static com.tencent.trpc.polaris.common.PolarisConstant.POLARIS_CMDBZONE;
import static com.tencent.trpc.polaris.common.PolarisConstant.POLARIS_HEALTHY;
import static com.tencent.trpc.polaris.common.PolarisConstant.POLARIS_ID;
import static com.tencent.trpc.polaris.common.PolarisConstant.POLARIS_INSTANCE;
import static com.tencent.trpc.polaris.common.PolarisConstant.POLARIS_PRIORITY;
import static com.tencent.trpc.polaris.common.PolarisConstant.POLARIS_PROTOCOL;
import static com.tencent.trpc.polaris.common.PolarisConstant.POLARIS_RESPONSE;
import static com.tencent.trpc.polaris.common.PolarisConstant.POLARIS_REVISION;
import static com.tencent.trpc.polaris.common.PolarisConstant.POLARIS_SERVICE;
import static com.tencent.trpc.polaris.common.PolarisConstant.POLARIS_VERSION;

import com.google.common.collect.Maps;
import com.tencent.polaris.api.config.global.RunMode;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.rpc.AbstractRequest;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.RequestMeta;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import com.tencent.trpc.polaris.common.PolarisConstant;
import com.tencent.trpc.selector.polaris.PolarisSelector;
import com.tencent.trpc.selector.polaris.PolarisSelectorConfig;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.mockito.Mockito;

public class DataTest {

    public static final String HOST_PREFIX = "127.0.0.";

    public static final int PORT_BASE = 1230;

    public static RpcClientContext rpcContext = mockRpcContext();

    public static RpcInvocation rpcInvocation = mockRpcInvocation();

    public static Request request = mockRequest();

    public static PluginConfig discoveryConfig = createDiscoveryConfig();

    public static PluginConfig workerPoolConfig = createWorkerPoolConfig();

    public static PluginConfig selectorConfig = createSelectorConfig();

    public static String getNamespace() {
        return "test";
    }

    public static String getService() {
        return "tencent.Greeter";
    }

    public static ServiceKey getServiceKey() {
        return new ServiceKey("java-sdk-test-service1", "com.tencent.trpc.test");
    }

    public static void init() {
        ConfigManager instance = ConfigManager.getInstance();
        instance.registerPlugin(discoveryConfig);
        instance.registerPlugin(selectorConfig);
        instance.registerPlugin(createWorkerPoolConfig());
    }

    public static ServiceId getExpService() {
        ServiceId serviceId = newServiceId();
        serviceId.setServiceName("serviceExp");
        return serviceId;
    }

    public static ServiceId newServiceId() {
        ServiceId serviceId = new ServiceId();
        serviceId.setServiceName(getService());
        serviceId.setVersion("1.0");
        serviceId.setGroup("group");
        Map<String, Object> params = new HashMap<>();
        params.put(PolarisConstant.TrpcPolarisParams.NAMESPACE_KEY.getKey(), "java-sdk-test-service1");
        serviceId.setParameters(params);
        return serviceId;
    }

    public static ServiceInstance genServiceInstance(int i) {
        Map<String, Object> map = new HashMap<>();
        map.put(POLARIS_CMDBCAMPUS, "cmdbCampus");
        map.put(POLARIS_CMDBREGION, "cmdbRegion");
        map.put(POLARIS_CMDBZONE, "cmdbZone");
        map.put(POLARIS_HEALTHY, true);
        map.put(POLARIS_REVISION, "revision");
        map.put(POLARIS_ID, "id" + i);
        map.put(POLARIS_VERSION, "version");
        map.put(POLARIS_PRIORITY, 32);
        map.put(PolarisConstant.TrpcPolarisParams.NAMESPACE_KEY.getKey(), "namespace");
        map.put(POLARIS_PROTOCOL, "grpc");
        map.put(POLARIS_SERVICE, "serviceId");
        ServiceInstance serviceInstance = new ServiceInstance(HOST_PREFIX + i, PORT_BASE + i, map);
        map.put(POLARIS_INSTANCE, getPolarisInstance(serviceInstance));
        map.put(POLARIS_RESPONSE, getPolarisResponse());
        return serviceInstance;
    }

    public static RpcInvocation mockRpcInvocation() {
        RpcInvocation rpcInvocation = Mockito.mock(RpcInvocation.class);
        Mockito.when(rpcInvocation.getRpcMethodName()).thenReturn("test");
        return rpcInvocation;
    }

    public static RpcClientContext mockRpcContext() {
        return new RpcClientContext();
    }

    public static Request mockRequest() {
        Request request = Mockito.mock(AbstractRequest.class);
        Mockito.when(request.getInvocation()).thenReturn(rpcInvocation);
        Mockito.when(request.getMeta()).thenReturn(new RequestMeta());
        return request;
    }

    public static Request mockHashValRequest() {
        Request request = new DefRequest();
        request.setInvocation(DataTest.rpcInvocation);
        RequestMeta requestMeta = new RequestMeta();
        requestMeta.setHashVal("hashVal");
        request.setMeta(requestMeta);
        return request;
    }

    public static Request mockServiceMetadataRequest() {
        Request request = Mockito.mock(AbstractRequest.class);
        Mockito.when(request.getInvocation()).thenReturn(rpcInvocation);
        Mockito.when(request.getMeta()).thenReturn(new RequestMeta());
        Map<String, Object> attachments = new HashMap<>();
        attachments.put(PolarisConstant.SELECTOR_SERVICE_NAME, "test-service-name");
        attachments.put(PolarisConstant.SELECTOR_NAMESPACE, "test-namespace");
        attachments.put(PolarisConstant.SELECTOR_ENV_NAME, "test-env-name");
        attachments.put(PolarisConstant.SELECTOR_META_PREFIX + "key1", "test-metadata-value1");
        attachments.put(PolarisConstant.SELECTOR_META_PREFIX + "key2",
                "test-metadata-value2".getBytes(StandardCharsets.UTF_8));
        Mockito.when(request.getAttachments()).thenReturn(attachments);
        return request;
    }

    private static PluginConfig createDiscoveryConfig() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put(PolarisConstant.POLARIS_RUNMODE_KEY, RunMode.ModeNoAgent.ordinal());
        extMap.put(PolarisConstant.POLARIS_ADDRESSES_KEY, "http://10.235.25.48:8080");
        extMap.put(PolarisConstant.POLARIS_ENABLE_TRANS_META, "true");
        return new PluginConfig("polaris", PolarisSelector.class, extMap);
    }


    private static PluginConfig createWorkerPoolConfig() {
        return WorkerPoolManager.newThreadWorkerPoolConfig("selector_pool1", 4, Boolean.FALSE);
    }

    public static PluginConfig createSelectorConfig() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put(PolarisSelectorConfig.WORKER_POOL, workerPoolConfig.getName());
        extMap.putAll(discoveryConfig.getProperties());

        PluginConfig namingConfig = new PluginConfig("selector1", PolarisSelector.class, extMap);
        return namingConfig;
    }

    public static Instance getPolarisInstance(ServiceInstance serviceInstance) {
        Instance instance = Mockito.mock(Instance.class);
        Map<String, String> strMap = Maps.newHashMap(
                Maps.transformValues(serviceInstance.getParameters(), Object::toString));
        Mockito.when(instance.getMetadata()).thenReturn(strMap);
        Mockito.when(instance.getHost()).thenReturn(serviceInstance.getHost());
        Mockito.when(instance.getPort()).thenReturn(serviceInstance.getPort());
        Mockito.when(instance.getId()).thenReturn("instanceId" + serviceInstance.getPort());
        Mockito.when(instance.getNamespace()).thenReturn("namespace");
        Mockito.when(instance.getService()).thenReturn("service");
        Mockito.when(instance.getWeight()).thenReturn(100);
        Mockito.when(instance.getId()).thenReturn("" + serviceInstance.getPort());
        return instance;
    }

    private static InstancesResponse getPolarisResponse() {
        return Mockito.mock(InstancesResponse.class);
    }
}
