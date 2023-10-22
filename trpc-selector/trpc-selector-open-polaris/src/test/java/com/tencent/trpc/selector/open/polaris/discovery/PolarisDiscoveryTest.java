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
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.argThat;

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
import java.util.concurrent.Executors;
import javax.xml.ws.Holder;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(APIFactory.class)
@PowerMockIgnore({"javax.management.*"})
public class PolarisDiscoveryTest extends TestCase {

    @Override
    protected void setUp() {
        ConfigManager.stopTest();
        PowerMockito.mockStatic(APIFactory.class);
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

    @Override
    protected void tearDown() {
        ConfigManager.stopTest();
    }

    @Test
    public void testAsyncList() throws Exception {
        Instance instance1 = PowerMockito.mock(Instance.class);
        PowerMockito.when(instance1.getId()).thenReturn("10001");
        PowerMockito.when(instance1.getHost()).thenReturn("10.0.0.1");
        PowerMockito.when(instance1.getPort()).thenReturn(2);
        Instance instance2 = PowerMockito.mock(Instance.class);
        PowerMockito.when(instance2.getId()).thenReturn("10002");
        PowerMockito.when(instance2.getHost()).thenReturn("10.0.0.1");
        PowerMockito.when(instance2.getPort()).thenReturn(1);
        List<Instance> instances = new ArrayList<>();
        instances.add(instance1);
        instances.add(instance2);
        ServiceInstances serviceInstances = PowerMockito.mock(ServiceInstances.class);
        PowerMockito.when(serviceInstances.getInstances()).thenReturn(instances);
        InstancesResponse response = new InstancesResponse(serviceInstances, null, null);

        CompletableFuture<InstancesResponse> instancesResponseCompletableFuture = CompletableFuture
                .completedFuture(response);
        InstancesFuture instancesFuture = new InstancesFuture(instancesResponseCompletableFuture);

        ConsumerAPI consumerAPI = PowerMockito.mock(ConsumerAPI.class);
        PowerMockito.when(consumerAPI.asyncGetInstances(anyObject())).thenReturn(instancesFuture);
        PowerMockito.when(APIFactory
                .createConsumerAPIByConfig(argThat(new ArgumentMatcher<Configuration>() {
                    @Override
                    public boolean matches(Object o) {
                        Configuration configuration = (Configuration) o;
                        Assert.assertEquals("/tmp",
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
        final Holder<List> result = new Holder<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        stage.whenComplete((list, ex) -> {
            result.value = list;
            countDownLatch.countDown();
        });
        countDownLatch.await();
        Assert.assertEquals(2, result.value.size());
    }

    @Test
    public void testInitExp() {
        try {
            PowerMockito.when(APIFactory.createConsumerAPIByConfig(anyObject())).then(inv -> {
                throw new PolarisException(ErrorCode.INVALID_CONFIG, "test error");
            });
            ExtensionLoader.getExtensionLoader(Discovery.class).getExtension("polaris");
            Assert.fail("no error happen");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testListExp() throws Exception {
        Instance instance1 = PowerMockito.mock(Instance.class);
        PowerMockito.when(instance1.getId()).thenReturn("10001");
        PowerMockito.when(instance1.getHost()).thenReturn("10.0.0.1");
        PowerMockito.when(instance1.getPort()).thenReturn(2);
        Instance instance2 = PowerMockito.mock(Instance.class);
        PowerMockito.when(instance2.getId()).thenReturn("10002");
        PowerMockito.when(instance2.getHost()).thenReturn("10.0.0.1");
        PowerMockito.when(instance2.getPort()).thenReturn(1);
        List<Instance> instances = new ArrayList<>();
        instances.add(instance1);
        instances.add(instance2);
        ServiceInstances serviceInstances = PowerMockito.mock(ServiceInstances.class);
        PowerMockito.when(serviceInstances.getInstances()).thenReturn(instances);
        InstancesResponse response = new InstancesResponse(serviceInstances, null, null);
        CompletableFuture<InstancesResponse> instancesResponseCompletableFuture = CompletableFuture
                .completedFuture(response);
        InstancesFuture instancesFuture = new InstancesFuture(instancesResponseCompletableFuture);
        ConsumerAPI consumerAPI = PowerMockito.mock(ConsumerAPI.class);
        PowerMockito.when(consumerAPI.asyncGetInstances(anyObject())).then(inv -> {
            throw new PolarisException(ErrorCode.INVALID_CONFIG, "test error");
        });
        PowerMockito.when(APIFactory.createConsumerAPIByConfig(anyObject()))
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
            Assert.fail("no error happen");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAsyncListEmpty() throws Exception {
        InstancesResponse response = new InstancesResponse(PowerMockito.mock(ServiceInstances.class), null, null);
        CompletableFuture<InstancesResponse> instancesResponseCompletableFuture = CompletableFuture
                .completedFuture(response);
        InstancesFuture instancesFuture = new InstancesFuture(instancesResponseCompletableFuture);
        ConsumerAPI consumerAPI = PowerMockito.mock(ConsumerAPI.class);
        PowerMockito.when(consumerAPI.asyncGetInstances(anyObject())).thenReturn(instancesFuture);
        PowerMockito.when(APIFactory.createConsumerAPIByConfig(anyObject()))
                .thenReturn(consumerAPI);

        Discovery discovery =
                ExtensionLoader.getExtensionLoader(Discovery.class).getExtension("polaris");

        ServiceId serviceId = new ServiceId();
        Map<String, Object> params = new HashMap<>();
        params.put(PolarisConstant.TrpcPolarisParams.NAMESPACE_KEY.getKey(), "test");
        serviceId.setParameters(params);
        CompletionStage<List<ServiceInstance>> stage =
                discovery.asyncList(serviceId, Executors.newSingleThreadExecutor());
        final Holder<List> result = new Holder<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        stage.whenComplete((list, ex) -> {
            result.value = list;
            countDownLatch.countDown();
        });
        countDownLatch.await();
        Assert.assertEquals(0, result.value.size());
    }

    @Test
    public void testList() throws Exception {
        Instance instance1 = PowerMockito.mock(Instance.class);
        PowerMockito.when(instance1.getId()).thenReturn("10001");
        PowerMockito.when(instance1.getHost()).thenReturn("10.0.0.1");
        PowerMockito.when(instance1.getPort()).thenReturn(2);
        Instance instance2 = PowerMockito.mock(Instance.class);
        PowerMockito.when(instance2.getId()).thenReturn("10002");
        PowerMockito.when(instance2.getHost()).thenReturn("10.0.0.1");
        PowerMockito.when(instance2.getPort()).thenReturn(1);
        List<Instance> instances = new ArrayList<>();
        instances.add(instance1);
        instances.add(instance2);
        ServiceInstances serviceInstances = PowerMockito.mock(ServiceInstances.class);
        PowerMockito.when(serviceInstances.getInstances()).thenReturn(instances);
        InstancesResponse response = new InstancesResponse(serviceInstances, null, null);
        ConsumerAPI consumerAPI = PowerMockito.mock(ConsumerAPI.class);
        PowerMockito.when(consumerAPI.getInstances(anyObject())).thenReturn(response);
        PowerMockito.when(APIFactory.createConsumerAPIByConfig(anyObject()))
                .thenReturn(consumerAPI);

        Discovery discovery =
                ExtensionLoader.getExtensionLoader(Discovery.class).getExtension("polaris");
        ServiceId serviceId = new ServiceId();
        Map<String, Object> params = new HashMap<>();
        params.put(PolarisConstant.TrpcPolarisParams.NAMESPACE_KEY.getKey(), "test");
        serviceId.setParameters(params);
        List<ServiceInstance> trpcInstances = discovery.list(serviceId);
        Assert.assertEquals(2, trpcInstances.size());
    }

    @Test
    public void testListAndNamespaceNotEqual() throws Exception {
        Instance instance1 = PowerMockito.mock(Instance.class);
        PowerMockito.when(instance1.getId()).thenReturn("10001");
        PowerMockito.when(instance1.getHost()).thenReturn("10.0.0.1");
        PowerMockito.when(instance1.getPort()).thenReturn(2);
        List<Instance> instances = new ArrayList<>();
        instances.add(instance1);
        ServiceInstances serviceInstances = PowerMockito.mock(ServiceInstances.class);
        PowerMockito.when(serviceInstances.getInstances()).thenReturn(instances);
        InstancesResponse response = new InstancesResponse(serviceInstances, null, null);
        ConsumerAPI consumerAPI = PowerMockito.mock(ConsumerAPI.class);
        PowerMockito.when(consumerAPI.getInstances(anyObject())).thenReturn(response);
        PowerMockito.when(APIFactory.createConsumerAPIByConfig(anyObject()))
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
            Assert.assertTrue(e instanceof IllegalStateException);
        }
        Assert.assertNotNull(exception);

        // namespace is null
        serviceId = new ServiceId();
        exception = null;
        discovery =
                ExtensionLoader.getExtensionLoader(Discovery.class).getExtension("polaris");
        try {
            discovery.list(serviceId);
        } catch (Exception e) {
            exception = e;
            Assert.assertTrue(e instanceof IllegalStateException);
        }
        Assert.assertNotNull(exception);
    }

    @Test
    public void testListAndNamespaceNotEqualButAllowed() throws Exception {
        Map<Class<?>, Map<String, PluginConfig>> map = ExtensionLoader.getPluginConfigMap();
        PluginConfig confg = map.get(Discovery.class)
                .get("polaris");
        confg.getProperties().put(NAMESPACE_DIFF_ALLOWED, true);
        Instance instance1 = PowerMockito.mock(Instance.class);
        PowerMockito.when(instance1.getId()).thenReturn("10001");
        PowerMockito.when(instance1.getHost()).thenReturn("10.0.0.1");
        PowerMockito.when(instance1.getPort()).thenReturn(2);
        List<Instance> instances = new ArrayList<>();
        instances.add(instance1);
        ServiceInstances serviceInstances = PowerMockito.mock(ServiceInstances.class);
        PowerMockito.when(serviceInstances.getInstances()).thenReturn(instances);
        InstancesResponse response = new InstancesResponse(serviceInstances, null, null);
        ConsumerAPI consumerAPI = PowerMockito.mock(ConsumerAPI.class);
        PowerMockito.when(consumerAPI.getInstances(anyObject())).thenReturn(response);
        PowerMockito.when(APIFactory.createConsumerAPIByConfig(anyObject()))
                .thenReturn(consumerAPI);

        ServiceId serviceId = new ServiceId();
        Map<String, Object> params = new HashMap<>();
        params.put("namespace", "test1");
        serviceId.setParameters(params);

        Discovery discovery =
                ExtensionLoader.getExtensionLoader(Discovery.class).getExtension("polaris");
        List<ServiceInstance> trpcInstances = discovery.list(serviceId);
        Assert.assertEquals(1, trpcInstances.size());
        Assert.assertEquals("10.0.0.1", trpcInstances.get(0).getHost());
        Assert.assertEquals(2, trpcInstances.get(0).getPort());
    }


    @Test
    public void testListEmpty() throws Exception {
        ConsumerAPI consumerAPI = PowerMockito.mock(ConsumerAPI.class);
        InstancesResponse response = new InstancesResponse(PowerMockito.mock(ServiceInstances.class), null, null);

        PowerMockito.when(consumerAPI.getInstances(anyObject())).thenReturn(response);
        PowerMockito.when(APIFactory.createConsumerAPIByConfig(anyObject()))
                .thenReturn(consumerAPI);

        Discovery discovery =
                ExtensionLoader.getExtensionLoader(Discovery.class).getExtension("polaris");
        ServiceId serviceId = new ServiceId();
        Map<String, Object> params = new HashMap<>();
        params.put(PolarisConstant.TrpcPolarisParams.NAMESPACE_KEY.getKey(), "test");
        serviceId.setParameters(params);
        List<ServiceInstance> serviceInstances = discovery.list(serviceId);
        Assert.assertEquals(0, serviceInstances.size());
    }

    @Test
    public void testAsyncListHasEx() throws Exception {
        CompletableFuture<InstancesResponse> instancesResponseCompletableFuture = CompletableFuture.supplyAsync(() -> {
            throw new NullPointerException();
        });
        InstancesFuture instancesFuture = new InstancesFuture(instancesResponseCompletableFuture);

        ConsumerAPI consumerAPI = PowerMockito.mock(ConsumerAPI.class);
        PowerMockito.when(consumerAPI.asyncGetInstances(anyObject())).thenReturn(instancesFuture);
        PowerMockito.when(APIFactory.createConsumerAPIByConfig(anyObject()))
                .thenReturn(consumerAPI);

        ServiceId serviceId = new ServiceId();
        Map<String, Object> params = new HashMap<>();
        params.put(PolarisConstant.TrpcPolarisParams.NAMESPACE_KEY.getKey(), "test");
        serviceId.setParameters(params);
        Discovery discovery =
                ExtensionLoader.getExtensionLoader(Discovery.class).getExtension("polaris");
        CompletionStage<List<ServiceInstance>> stage =
                discovery.asyncList(serviceId, Executors.newSingleThreadExecutor());
        final Holder<List> result = new Holder<>();
        final Holder<Throwable> exception = new Holder<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        stage.whenComplete((list, ex) -> {
            result.value = list;
            exception.value = ex;
            countDownLatch.countDown();
        });
        countDownLatch.await();
        Assert.assertEquals(result.value, null);
        Assert.assertEquals(exception.value.getCause().getClass(), TRpcException.class);
    }

}
