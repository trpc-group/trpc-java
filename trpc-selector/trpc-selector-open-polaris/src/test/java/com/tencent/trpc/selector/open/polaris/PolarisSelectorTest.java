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

package com.tencent.trpc.selector.open.polaris;

import static org.mockito.Mockito.when;

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.Supplier;
import com.tencent.polaris.api.plugin.weight.WeightType;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.GetOneInstanceRequest;
import com.tencent.polaris.api.rpc.InstancesFuture;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.APIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.plugins.loadbalancer.random.WeightedRandomBalance;
import com.tencent.polaris.plugins.loadbalancer.ringhash.ConsistentHashLoadBalance;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.polaris.common.PolarisConstant;
import com.tencent.trpc.polaris.common.PolarisTrans;
import com.tencent.trpc.selector.polaris.PolarisSelector;
import com.tencent.trpc.selector.polaris.common.PolarisCommon;
import com.tencent.trpc.selector.polaris.common.pojo.PolarisServiceInstances;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest({APIFactory.class})
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*"})
public class PolarisSelectorTest {

    private static final int PORT_BASE = 1230;
    private PluginConfig selectorConfig;
    private int expPort = PORT_BASE + 1024;

    @Before
    public void before() {
        ConfigManager.stopTest();
        DataTest.init();
        this.selectorConfig = DataTest.createSelectorConfig();
        mockPolaris();
        ConfigManager.startTest();
    }

    @After
    public void after() {
        ConfigManager.stopTest();
    }

    @Test
    public void testEmptyReturn() {
        ServiceId serviceId = DataTest.newServiceId();
        serviceId.setServiceName("service_empty");

        PolarisSelector clusterNaming = new PolarisSelector();
        clusterNaming.setPluginConfig(selectorConfig);
        clusterNaming.init();
        clusterNaming.asyncSelectOne(serviceId, DataTest.request).whenComplete((res, err) -> {
            if (err != null) {
                err.printStackTrace();
                Assert.fail("error happens:" + err.getMessage());
            }
            Assert.assertNull("res not null", res);
        });

        clusterNaming.asyncSelectAll(serviceId, DataTest.request).whenComplete((res, err) -> {
            if (err != null) {
                err.printStackTrace();
                Assert.fail("list error happens:" + err.getMessage());
            }
            Assert.assertNull("list res not null", res);
        });
    }

    @Test
    public void testExp() {
        ServiceId expService = DataTest.getExpService();
        PolarisSelector clusterNaming = new PolarisSelector();
        clusterNaming.setPluginConfig(selectorConfig);
        clusterNaming.init();
        CompletionStage<ServiceInstance> future =
                clusterNaming.asyncSelectOne(expService, DataTest.request);
        future.whenComplete((res, err) -> Assert.assertNull("err not null", err));

        CompletionStage<List<ServiceInstance>> listFuture =
                clusterNaming.asyncSelectAll(expService, DataTest.request);
        listFuture.whenComplete((res, err) -> Assert.assertNull("err not null", err));
    }

    @Test
    public void testReport() {
        PolarisSelector clusterNaming = new PolarisSelector();
        clusterNaming.setPluginConfig(selectorConfig);
        clusterNaming.init();
        clusterNaming.report(DataTest.genServiceInstance(1), 0, 100L);
    }


    @Test
    public void testSelectAll() {
        PolarisSelector clusterNaming = new PolarisSelector();
        clusterNaming.setPluginConfig(selectorConfig);
        clusterNaming.init();
        CompletionStage<List<ServiceInstance>> future =
                clusterNaming.asyncSelectAll(DataTest.newServiceId(), DataTest.request);

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CompletionStage<List<ServiceInstance>> stage = future.whenComplete((res, err) -> {
            if (err != null) {
                errorRef.set(err);
                err.printStackTrace();
            }
        });
        CompletableFuture.allOf(stage.toCompletableFuture()).join();
        Assert.assertNull(errorRef.get());
    }

    @Test
    public void testFallback() {
        PolarisSelector clusterNaming = new PolarisSelector();
        clusterNaming.setPluginConfig(selectorConfig);
        clusterNaming.init();
        ServiceId serviceId = DataTest.newServiceId();
        serviceId.setServiceName("fallback");
        Request request = DataTest.request;
        CompletionStage<ServiceInstance> future = clusterNaming
                .asyncSelectOne(serviceId, request);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CompletionStage<ServiceInstance> stage = future.whenComplete((res, err) -> {
            if (err != null) {
                errorRef.set(err);
                err.printStackTrace();
            }
        });
        CompletableFuture.allOf(stage.toCompletableFuture()).join();
        Assert.assertNull(errorRef.get());
    }

    @Test
    public void testSelectOneWithServiceMetadata() {
        PolarisSelector clusterNaming = new PolarisSelector();
        clusterNaming.setPluginConfig(selectorConfig);
        clusterNaming.init();
        ServiceId serviceId = DataTest.newServiceId();
        serviceId.setServiceName("service-metadata-select-one");
        clusterNaming.warmup(serviceId);
        Request request = DataTest.mockServiceMetadataRequest();
        CompletionStage<ServiceInstance> future = clusterNaming.asyncSelectOne(serviceId, request);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CompletionStage<ServiceInstance> stage = future.whenComplete((res, err) -> {
            if (err != null) {
                errorRef.set(err);
                err.printStackTrace();
            }
        });
        CompletableFuture.allOf(stage.toCompletableFuture()).join();
        Assert.assertNull(errorRef.get());
    }

    @Test
    public void testSelectAllWithServiceMetadata() {
        PolarisSelector clusterNaming = new PolarisSelector();
        clusterNaming.setPluginConfig(selectorConfig);
        clusterNaming.init();
        ServiceId serviceId = DataTest.newServiceId();
        serviceId.setServiceName("service-metadata-select-all");
        Request request = DataTest.mockServiceMetadataRequest();
        CompletionStage<List<ServiceInstance>> future = clusterNaming.asyncSelectAll(serviceId, request);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CompletionStage<List<ServiceInstance>> stage = future.whenComplete((res, err) -> {
            if (err != null) {
                errorRef.set(err);
                err.printStackTrace();
            }
        });
        CompletableFuture.allOf(stage.toCompletableFuture()).join();
        Assert.assertNull(errorRef.get());
    }

    private void mockPolaris() {
        try {
            ConsumerAPI consumerAPI = Mockito.mock(ConsumerAPI.class);
            Mockito.doNothing().when(consumerAPI).updateServiceCallResult(Mockito.any());
            Supplier supplier = Mockito.mock(Supplier.class);
            SDKContext sdkContext = Mockito.mock(SDKContext.class);
            when(sdkContext.getPlugins()).thenReturn(supplier);

            when(consumerAPI.asyncGetInstances(Mockito.any())).thenAnswer(newPolarisAnswer(false));

            when(consumerAPI.asyncGetOneInstance(Mockito.any())).thenAnswer(newPolarisAnswer(true));

            PowerMockito.mockStatic(APIFactory.class);
            when(APIFactory.createConsumerAPIByContext(Mockito.any())).thenReturn(consumerAPI);
            when(APIFactory.createConsumerAPIByConfig(Mockito.any())).thenReturn(consumerAPI);
            when(APIFactory.initContextByConfig(Mockito.any())).thenReturn(sdkContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Answer<InstancesFuture> newPolarisAnswer(boolean getOne) {
        return inv -> {
            Object req = inv.getArguments()[0];
            String service;
            if (req instanceof GetInstancesRequest) {
                service = ((GetInstancesRequest) req).getService();
            } else {
                service = ((GetOneInstanceRequest) req).getService();
            }
            if (service.contains("exp")) {
                throw new PolarisException(
                        ErrorCode.INSTANCE_INFO_ERROR,
                        "test polaris exp");
            }
            int size = 10;
            if (service.contains("empty")) {
                size = 0;
            }
            if (service.contains("fallback") && getOne) {
                size = 0;
            }
            CompletableFuture<InstancesResponse> instancesResponseCompletableFuture = CompletableFuture
                    .completedFuture(new InstancesResponse(getServiceInstances(size), null, null));
            InstancesFuture instancesFuture = new InstancesFuture(() -> {
                try {
                    return instancesResponseCompletableFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
            return instancesFuture;
        };

    }

    private ServiceInstances getServiceInstances(int size) {
        List<ServiceInstance> instanceList = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            instanceList.add(DataTest.genServiceInstance(i + 1));
        }
        PolarisServiceInstances serviceInstances = new PolarisServiceInstances();
        serviceInstances.setInitialized(true);
        serviceInstances.setInstances(
                instanceList.stream().map(PolarisTrans::toPolarisInstance)
                        .collect(Collectors.toList()));
        serviceInstances.setMetadata(new HashMap<>());
        serviceInstances.setNamespace(DataTest.getNamespace());
        serviceInstances.setService(DataTest.getService());
        serviceInstances.setRevision("test");
        serviceInstances.setWeightType(WeightType.DYNAMIC);

        return serviceInstances;
    }

    @Test
    public void toPolarisInstance() {
        Instance instance = Mockito.mock(Instance.class);
        when(instance.getHost()).thenReturn("127.0.0.1");
        when(instance.getPort()).thenReturn(111);
        when(instance.isHealthy()).thenReturn(true);
        when(instance.getRevision()).thenReturn("1.0.0");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("set", "set.sz.1");
        when(instance.getMetadata()).thenReturn(metadata);

        InstancesResponse instancesResponse = Mockito.mock(InstancesResponse.class);
        when(instancesResponse.getTotalWeight()).thenReturn(1000);
        when(instancesResponse.getNamespace()).thenReturn("dev");
        when(instancesResponse.getService()).thenReturn("trpc.test.test.1");
        when(instancesResponse.getInstances()).thenReturn(new Instance[]{instance});

        ServiceInstance serviceInstance = PolarisTrans
                .toServiceInstance(instancesResponse, instance);

        List<ServiceInstance> serviceInstances = new ArrayList<>();
        serviceInstances.add(serviceInstance);
        ServiceInstances polarisServiceInstance = PolarisTrans.toPolarisInstance(serviceInstances);
        Assert.assertEquals("1.0.0", polarisServiceInstance.getRevision());
        Assert.assertEquals(1000, polarisServiceInstance.getTotalWeight());
        Assert.assertEquals(1, polarisServiceInstance.getInstances().size());
        Assert.assertEquals("dev", polarisServiceInstance.getNamespace());
        Assert.assertEquals("trpc.test.test.1", polarisServiceInstance.getService());
        Assert.assertEquals("set.sz.1", polarisServiceInstance.getMetadata().get("set"));
    }

    @Test
    public void testEmptyToPolarisInstance() {
        List<ServiceInstance> serviceInstances = null;
        Assert.assertNull(PolarisTrans.toPolarisInstance(serviceInstances));
    }

    @Test
    public void testDestroy() {
        PolarisSelector clusterNaming = new PolarisSelector();
        clusterNaming.setPluginConfig(selectorConfig);
        clusterNaming.init();
        clusterNaming.destroy();
    }


    /***
     *   --story=880096511
     * */
    @Test
    public void testGetConsumerAPI() {
        PolarisSelector polarisSelector = new PolarisSelector();
        polarisSelector.setPluginConfig(selectorConfig);
        polarisSelector.init();
        Assert.assertNotNull(polarisSelector.getPolarisAPI());
        polarisSelector.destroy();
    }

    @Test
    public void testExceptionInit() {
        PolarisSelector polarisSelector = new PolarisSelector();
        try {
            polarisSelector.setPluginConfig(selectorConfig);
            polarisSelector.init();
            Assert.assertNull(polarisSelector.getPolarisAPI());
            polarisSelector.destroy();
        } catch (Exception e) {
            return;
        }
    }

    @Test
    public void testExceptionWarmup() {
        PolarisSelector clusterNaming = new PolarisSelector();
        clusterNaming.setPluginConfig(selectorConfig);
        clusterNaming.init();
        ServiceId serviceId = DataTest.newTestServiceId();
        try {
            clusterNaming.warmup(serviceId);
        } catch (Exception e) {
            return;
        }
    }

    @Test
    public void testAsyncSelectOne() {
        PolarisSelector clusterNaming = new PolarisSelector();
        clusterNaming.setPluginConfig(selectorConfig);
        clusterNaming.init();
        ServiceId serviceId = DataTest.newServiceId();
        serviceId.setServiceName("service-metadata-select-one");
        clusterNaming.warmup(serviceId);
        Request request = DataTest.mockServiceMetadataRequest();
        CompletionStage<ServiceInstance> future = clusterNaming.asyncSelectOne(serviceId, request);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CompletionStage<ServiceInstance> stage = future.whenComplete((res, err) -> {
            if (err != null) {
                errorRef.set(err);
                err.printStackTrace();
            }
        });
        CompletableFuture.allOf(stage.toCompletableFuture()).join();
        Assert.assertNull(errorRef.get());
    }

    @Test
    public void testGetCriteria() {
        PolarisSelector clusterNaming = new PolarisSelector();
        clusterNaming.setPluginConfig(selectorConfig);
        clusterNaming.init();
        ServiceId serviceId = DataTest.newServiceId();
        serviceId.setServiceName("service-metadata-select-one");
        clusterNaming.warmup(serviceId);
        Request request = DataTest.mockServiceMetadataRequest();
        request.getMeta().setHashVal("123333");
        CompletionStage<ServiceInstance> future = clusterNaming.asyncSelectOne(serviceId, request);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CompletionStage<ServiceInstance> stage = future.whenComplete((res, err) -> {
            if (err != null) {
                errorRef.set(err);
                err.printStackTrace();
            }
        });
        CompletableFuture.allOf(stage.toCompletableFuture()).join();
        Assert.assertNull(errorRef.get());
    }

    @Test
    public void testExceptionAsyncSelectOne() {
        PolarisSelector clusterNaming = new PolarisSelector();
        clusterNaming.setPluginConfig(selectorConfig);
        ServiceId serviceId = DataTest.newServiceId();
        serviceId.setServiceName("fallback");
        Request request = DataTest.request;
        try {
            CompletionStage<ServiceInstance> future = clusterNaming
                    .asyncSelectOne(serviceId, request);
        } catch (Exception e) {
            return;
        }
    }

    @Test
    public void testExceptionReport() {
        PolarisSelector clusterNaming = new PolarisSelector();
        clusterNaming.setPluginConfig(selectorConfig);
        clusterNaming.init();
        try {
            clusterNaming.report(null, 1, -1L);
        } catch (Exception e) {
            return;
        }
    }

    @Test
    public void testExceptionAsyncSelectAll() {
        PolarisSelector clusterNaming = new PolarisSelector();
        clusterNaming.setPluginConfig(selectorConfig);
        clusterNaming.init();
        try {
            ServiceId serviceId = Mockito.mock(ServiceId.class);
            clusterNaming.asyncSelectAll(serviceId,DataTest.request);
        } catch (Exception e) {
            return;
        }
    }
}
