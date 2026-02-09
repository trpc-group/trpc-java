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

package com.tencent.trpc.selector.open.polaris.discovery;

import static com.tencent.trpc.polaris.common.PolarisConstant.NAMESPACE_DIFF_ALLOWED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.InstancesFuture;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.factory.api.APIFactory;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.spi.Discovery;
import com.tencent.trpc.polaris.common.PolarisConstant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.ArgumentMatcher;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PolarisDiscoveryTest {

    private MockedStatic<APIFactory> mockedStatic;

    @BeforeEach
    public void setUp() {
        ConfigManager.stopTest();
        mockedStatic = Mockito.mockStatic(APIFactory.class);
        Map<String, Object> extMap = new HashMap<>();
        extMap.put(PolarisConstant.POLARIS_ADDRESSES_KEY, "10.0.0.1");
        HashMap<String, Object> localCache = new HashMap<>();
        localCache.put(PolarisConstant.POLARIS_LOCALCACHE_PERSISTDIR, "/tmp");
        extMap.put(PolarisConstant.POLARIS_LOCALCACHE, localCache);
        ConfigManager.startTest();
        ConfigManager.getInstance()
                .registerPlugin(new PluginConfig("polaris", PolarisDiscovery.class, extMap));
        ConfigManager.getInstance().getGlobalConfig().setNamespace("test");
    }

    @AfterEach
    public void tearDown() {
        if (mockedStatic != null) {
            mockedStatic.close();
        }
        ConfigManager.stopTest();
    }

    @Test
    public void testAsyncList() throws Exception {
        Instance instance1 = Mockito.mock(Instance.class);
        Mockito.when(instance1.getId()).thenReturn("10001");
        Mockito.when(instance1.getHost()).thenReturn("10.0.0.1");
        Mockito.when(instance1.getPort()).thenReturn(2);
        Instance instance2 = Mockito.mock(Instance.class);
        Mockito.when(instance2.getId()).thenReturn("10002");
        Mockito.when(instance2.getHost()).thenReturn("10.0.0.1");
        Mockito.when(instance2.getPort()).thenReturn(1);
        List<Instance> instances = new ArrayList<>();
        instances.add(instance1);
        instances.add(instance2);
        ServiceInstances serviceInstances = Mockito.mock(ServiceInstances.class);
        Mockito.when(serviceInstances.getInstances()).thenReturn(instances);
        InstancesResponse response = new InstancesResponse(serviceInstances, null, null);

        CompletableFuture<InstancesResponse> instancesResponseCompletableFuture = CompletableFuture
                .completedFuture(response);
        InstancesFuture instancesFuture = new InstancesFuture(() -> {
            try {
                return instancesResponseCompletableFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

        ConsumerAPI consumerAPI = Mockito.mock(ConsumerAPI.class);
        Mockito.when(consumerAPI.asyncGetInstances(any())).thenReturn(instancesFuture);
        mockedStatic.when(() -> APIFactory
                .createConsumerAPIByConfig(
                        argThat((ArgumentMatcher<Configuration>) new ArgumentMatcher<Configuration>() {
                            @Override
                            public boolean matches(Configuration o) {
                                Configuration configuration = o;
                                Assertions.assertEquals("/tmp",
                                        configuration.getConsumer().getLocalCache().getPersistDir());
                                // Modify the properties of input parameters
                                return true;
                            }
                        }))).thenReturn(consumerAPI);

        Discovery discovery =
                ExtensionLoader.getExtensionLoader(Discovery.class).getExtension("polaris");

        ServiceId serviceId = new ServiceId();
        Map<String, Object> params = new HashMap<>();
        params.put(PolarisConstant.TrpcPolarisParams.NAMESPACE_KEY.getKey(), "test");
        serviceId.setParameters(params);
        CompletionStage<List<ServiceInstance>> stage =
                discovery.asyncList(serviceId, Executors.newSingleThreadExecutor());
        AtomicReference<List> result = new AtomicReference<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        stage.whenComplete((list, ex) -> {
            result.set(list);
            countDownLatch.countDown();
        });
        countDownLatch.await();
        Assertions.assertEquals(2, result.get().size());
    }

    @Test
    public void testInitExp() {
        try {
            mockedStatic.when(() -> APIFactory.createConsumerAPIByConfig(any())).then(inv -> {
                throw new PolarisException(ErrorCode.INVALID_CONFIG, "test error");
            });
            ExtensionLoader.getExtensionLoader(Discovery.class).getExtension("polaris");
            Assertions.fail("no error happen");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testListExp() throws Exception {
        Instance instance1 = Mockito.mock(Instance.class);
        Mockito.when(instance1.getId()).thenReturn("10001");
        Mockito.when(instance1.getHost()).thenReturn("10.0.0.1");
        Mockito.when(instance1.getPort()).thenReturn(2);
        Instance instance2 = Mockito.mock(Instance.class);
        Mockito.when(instance2.getId()).thenReturn("10002");
        Mockito.when(instance2.getHost()).thenReturn("10.0.0.1");
        Mockito.when(instance2.getPort()).thenReturn(1);
        List<Instance> instances = new ArrayList<>();
        instances.add(instance1);
        instances.add(instance2);
        ServiceInstances serviceInstances = Mockito.mock(ServiceInstances.class);
        Mockito.when(serviceInstances.getInstances()).thenReturn(instances);
        InstancesResponse response = new InstancesResponse(serviceInstances, null, null);
        CompletableFuture<InstancesResponse> instancesResponseCompletableFuture = CompletableFuture
                .completedFuture(response);
        InstancesFuture instancesFuture = new InstancesFuture(() -> {
            try {
                return instancesResponseCompletableFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        ConsumerAPI consumerAPI = Mockito.mock(ConsumerAPI.class);
        Mockito.when(consumerAPI.asyncGetInstances(any())).then(inv -> {
            throw new PolarisException(ErrorCode.INVALID_CONFIG, "test error");
        });
        mockedStatic.when(() -> APIFactory.createConsumerAPIByConfig(any()))
                .thenReturn(consumerAPI);

        Discovery discovery =
                ExtensionLoader.getExtensionLoader(Discovery.class).getExtension("polaris");

        try {
            ServiceId serviceId = new ServiceId();
            Map<String, Object> params = new HashMap<>();
            params.put(PolarisConstant.TrpcPolarisParams.NAMESPACE_KEY.getKey(), "test");
            serviceId.setParameters(params);
            CompletionStage<List<ServiceInstance>> stage =
                    discovery.asyncList(serviceId, Executors.newSingleThreadExecutor());
            Assertions.fail("no error happen");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAsyncListEmpty() throws Exception {
        InstancesResponse response = new InstancesResponse(Mockito.mock(ServiceInstances.class), null, null);
        CompletableFuture<InstancesResponse> instancesResponseCompletableFuture = CompletableFuture
                .completedFuture(response);
        InstancesFuture instancesFuture = new InstancesFuture(() -> {
            try {
                return instancesResponseCompletableFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        ConsumerAPI consumerAPI = Mockito.mock(ConsumerAPI.class);
        Mockito.when(consumerAPI.asyncGetInstances(any())).thenReturn(instancesFuture);
        mockedStatic.when(() -> APIFactory.createConsumerAPIByConfig(any()))
                .thenReturn(consumerAPI);

        Discovery discovery =
                ExtensionLoader.getExtensionLoader(Discovery.class).getExtension("polaris");

        ServiceId serviceId = new ServiceId();
        Map<String, Object> params = new HashMap<>();
        params.put(PolarisConstant.TrpcPolarisParams.NAMESPACE_KEY.getKey(), "test");
        serviceId.setParameters(params);
        CompletionStage<List<ServiceInstance>> stage =
                discovery.asyncList(serviceId, Executors.newSingleThreadExecutor());
        AtomicReference<List> result = new AtomicReference<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        stage.whenComplete((list, ex) -> {
            result.set(list);
            countDownLatch.countDown();
        });
        countDownLatch.await();
        Assertions.assertEquals(0, result.get().size());
    }

    @Test
    public void testList() throws Exception {
        Instance instance1 = Mockito.mock(Instance.class);
        Mockito.when(instance1.getId()).thenReturn("10001");
        Mockito.when(instance1.getHost()).thenReturn("10.0.0.1");
        Mockito.when(instance1.getPort()).thenReturn(2);
        Instance instance2 = Mockito.mock(Instance.class);
        Mockito.when(instance2.getId()).thenReturn("10002");
        Mockito.when(instance2.getHost()).thenReturn("10.0.0.1");
        Mockito.when(instance2.getPort()).thenReturn(1);
        List<Instance> instances = new ArrayList<>();
        instances.add(instance1);
        instances.add(instance2);
        ServiceInstances serviceInstances = Mockito.mock(ServiceInstances.class);
        Mockito.when(serviceInstances.getInstances()).thenReturn(instances);
        InstancesResponse response = new InstancesResponse(serviceInstances, null, null);
        ConsumerAPI consumerAPI = Mockito.mock(ConsumerAPI.class);
        Mockito.when(consumerAPI.getInstances(any())).thenReturn(response);
        mockedStatic.when(() -> APIFactory.createConsumerAPIByConfig(any()))
                .thenReturn(consumerAPI);

        Discovery discovery =
                ExtensionLoader.getExtensionLoader(Discovery.class).getExtension("polaris");
        ServiceId serviceId = new ServiceId();
        Map<String, Object> params = new HashMap<>();
        params.put(PolarisConstant.TrpcPolarisParams.NAMESPACE_KEY.getKey(), "test");
        serviceId.setParameters(params);
        List<ServiceInstance> trpcInstances = discovery.list(serviceId);
        Assertions.assertEquals(2, trpcInstances.size());
    }

    @Test
    public void testListAndNamespaceNotEqual() throws Exception {
        Instance instance1 = Mockito.mock(Instance.class);
        Mockito.when(instance1.getId()).thenReturn("10001");
        Mockito.when(instance1.getHost()).thenReturn("10.0.0.1");
        Mockito.when(instance1.getPort()).thenReturn(2);
        List<Instance> instances = new ArrayList<>();
        instances.add(instance1);
        ServiceInstances serviceInstances = Mockito.mock(ServiceInstances.class);
        Mockito.when(serviceInstances.getInstances()).thenReturn(instances);
        InstancesResponse response = new InstancesResponse(serviceInstances, null, null);
        ConsumerAPI consumerAPI = Mockito.mock(ConsumerAPI.class);
        Mockito.when(consumerAPI.getInstances(any())).thenReturn(response);
        mockedStatic.when(() -> APIFactory.createConsumerAPIByConfig(any()))
                .thenReturn(consumerAPI);
        ServiceId serviceId = new ServiceId();
        Map<String, Object> params = new HashMap<>();
        params.put("namespace", "test1");
        serviceId.setParameters(params);
        Exception exception = null;
        Discovery discovery =
                ExtensionLoader.getExtensionLoader(Discovery.class).getExtension("polaris");
        try {
            discovery.list(serviceId);
        } catch (Exception e) {
            exception = e;
            Assertions.assertTrue(e instanceof IllegalStateException);
        }
        Assertions.assertNotNull(exception);

        // namespace is null
        serviceId = new ServiceId();
        exception = null;
        discovery =
                ExtensionLoader.getExtensionLoader(Discovery.class).getExtension("polaris");
        try {
            discovery.list(serviceId);
        } catch (Exception e) {
            exception = e;
            Assertions.assertTrue(e instanceof IllegalStateException);
        }
        Assertions.assertNotNull(exception);
    }

    @Test
    public void testListAndNamespaceNotEqualButAllowed() throws Exception {
        Map<Class<?>, Map<String, PluginConfig>> map = ExtensionLoader.getPluginConfigMap();
        PluginConfig confg = map.get(Discovery.class)
                .get("polaris");
        confg.getProperties().put(NAMESPACE_DIFF_ALLOWED, true);
        Instance instance1 = Mockito.mock(Instance.class);
        Mockito.when(instance1.getId()).thenReturn("10001");
        Mockito.when(instance1.getHost()).thenReturn("10.0.0.1");
        Mockito.when(instance1.getPort()).thenReturn(2);
        List<Instance> instances = new ArrayList<>();
        instances.add(instance1);
        ServiceInstances serviceInstances = Mockito.mock(ServiceInstances.class);
        Mockito.when(serviceInstances.getInstances()).thenReturn(instances);
        InstancesResponse response = new InstancesResponse(serviceInstances, null, null);
        ConsumerAPI consumerAPI = Mockito.mock(ConsumerAPI.class);
        Mockito.when(consumerAPI.getInstances(any())).thenReturn(response);
        mockedStatic.when(() -> APIFactory.createConsumerAPIByConfig(any()))
                .thenReturn(consumerAPI);

        ServiceId serviceId = new ServiceId();
        Map<String, Object> params = new HashMap<>();
        params.put("namespace", "test1");
        serviceId.setParameters(params);

        Discovery discovery =
                ExtensionLoader.getExtensionLoader(Discovery.class).getExtension("polaris");
        List<ServiceInstance> trpcInstances = discovery.list(serviceId);
        Assertions.assertEquals(1, trpcInstances.size());
        Assertions.assertEquals("10.0.0.1", trpcInstances.get(0).getHost());
        Assertions.assertEquals(2, trpcInstances.get(0).getPort());
    }


    @Test
    public void testListEmpty() throws Exception {
        ConsumerAPI consumerAPI = Mockito.mock(ConsumerAPI.class);
        InstancesResponse response = new InstancesResponse(Mockito.mock(ServiceInstances.class), null, null);

        Mockito.when(consumerAPI.getInstances(any())).thenReturn(response);
        mockedStatic.when(() -> APIFactory.createConsumerAPIByConfig(any()))
                .thenReturn(consumerAPI);

        Discovery discovery =
                ExtensionLoader.getExtensionLoader(Discovery.class).getExtension("polaris");
        ServiceId serviceId = new ServiceId();
        Map<String, Object> params = new HashMap<>();
        params.put(PolarisConstant.TrpcPolarisParams.NAMESPACE_KEY.getKey(), "test");
        serviceId.setParameters(params);
        List<ServiceInstance> serviceInstances = discovery.list(serviceId);
        Assertions.assertEquals(0, serviceInstances.size());
    }

    @Test
    public void testAsyncListHasEx() throws Exception {
        CompletableFuture<InstancesResponse> instancesResponseCompletableFuture = CompletableFuture.supplyAsync(() -> {
            throw new NullPointerException();
        });
        InstancesFuture instancesFuture = new InstancesFuture(() -> {
            try {
                return instancesResponseCompletableFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

        ConsumerAPI consumerAPI = Mockito.mock(ConsumerAPI.class);
        Mockito.when(consumerAPI.asyncGetInstances(any())).thenReturn(instancesFuture);
        mockedStatic.when(() -> APIFactory.createConsumerAPIByConfig(any()))
                .thenReturn(consumerAPI);

        ServiceId serviceId = new ServiceId();
        Map<String, Object> params = new HashMap<>();
        params.put(PolarisConstant.TrpcPolarisParams.NAMESPACE_KEY.getKey(), "test");
        serviceId.setParameters(params);
        Discovery discovery =
                ExtensionLoader.getExtensionLoader(Discovery.class).getExtension("polaris");
        CompletionStage<List<ServiceInstance>> stage =
                discovery.asyncList(serviceId, Executors.newSingleThreadExecutor());
        AtomicReference<List> result = new AtomicReference<>();
        AtomicReference<Throwable> exception = new AtomicReference<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        stage.whenComplete((list, ex) -> {
            result.set(list);
            exception.set(ex);
            countDownLatch.countDown();
        });
        countDownLatch.await();
        Assertions.assertEquals(result.get(), null);
        Assertions.assertEquals(exception.get().getCause().getClass(), TRpcException.class);
    }

}
