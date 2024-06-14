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

package com.tencent.trpc.selector.open.polaris.common;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.factory.config.consumer.CircuitBreakerConfigImpl;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.selector.SelectorManager;
import com.tencent.trpc.core.selector.spi.Selector;
import com.tencent.trpc.selector.open.polaris.DataTest;
import com.tencent.trpc.selector.polaris.PolarisSelector;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Maps;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.plugin.route.RouteResult.NextRouterInfo;
import com.tencent.polaris.api.plugin.route.RouteResult.State;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.consumer.ConsumerConfigImpl;
import com.tencent.polaris.factory.config.consumer.LocalCacheConfigImpl;
import com.tencent.polaris.factory.config.global.APIConfigImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.polaris.common.PolarisConstant;
import com.tencent.trpc.polaris.common.PolarisTrans;
import com.tencent.trpc.selector.polaris.common.PolarisCommon;

public class PolarisTransTest {

    @Test
    public void testTrans2StringMap() {
        Map<String, Object> serviceIdParameters = Maps.newHashMap();

        Map<String, Object> map = new HashMap<>();
        serviceIdParameters.put(PolarisConstant.TrpcPolarisParams.METADATA.getKey(), map);
        map.put("test", "test");
        Map<String, String> strMap = PolarisTrans.trans2PolarisMetadata(serviceIdParameters);
        Assert.assertEquals("test", strMap.get("test"));

        map = new HashMap<>();
        serviceIdParameters.put(PolarisConstant.TrpcPolarisParams.METADATA.getKey(), map);
        map.put("test", 1L);
        strMap = PolarisTrans.trans2PolarisMetadata(serviceIdParameters);
        Assert.assertEquals("1", strMap.get("test"));

        map = new HashMap<>();
        serviceIdParameters.put(PolarisConstant.TrpcPolarisParams.METADATA.getKey(), map);
        Map<String, Integer> notString = new HashMap<>();
        notString.put("test", 2);
        map.put("test", notString);
        strMap = PolarisTrans.trans2PolarisMetadata(serviceIdParameters);
        Assert.assertEquals("{\"test\":2}", strMap.get("test"));

        strMap = PolarisTrans.trans2PolarisMetadata(Maps.newHashMap());
        Assert.assertEquals(0, strMap.size());

        serviceIdParameters.put(PolarisConstant.TrpcPolarisParams.METADATA.getKey(), Maps.newHashMap());
        strMap = PolarisTrans.trans2PolarisMetadata(serviceIdParameters);
        Assert.assertEquals(0, strMap.size());

        serviceIdParameters.put(PolarisConstant.TrpcPolarisParams.METADATA.getKey(), new Object());
        strMap = PolarisTrans.trans2PolarisMetadata(serviceIdParameters);
        Assert.assertEquals(0, strMap.size());
    }

    @Test
    public void testTrans2ApiConfig() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put(PolarisConstant.POLARIS_API_MAXRETRYTIMES_KEY, 10);
        extMap.put(PolarisConstant.POLARIS_API_BINDIF_KEY, "if");
        APIConfigImpl apiConfig = new APIConfigImpl();
        PolarisTrans.updateApiConfig(apiConfig, extMap);
        Assert.assertEquals(10, apiConfig.getMaxRetryTimes());
        Assert.assertEquals("if", apiConfig.getBindIf());
    }

    @Test
    public void testTrans2ServerConnectorConfig() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put(PolarisConstant.POLARIS_RUNMODE_KEY, 0);
        extMap.put(PolarisConstant.POLARIS_ADDRESSES_KEY, "10.0.0.1:1239");
        extMap.put(PolarisConstant.POLARIS_PROTOCOL_KEY, "http");
        ServerConnectorConfigImpl serverConnectorConfig = new ServerConnectorConfigImpl();
        PolarisTrans.updateServerConnectorConfig(serverConnectorConfig, extMap);
        Assert.assertEquals("10.0.0.1:1239", serverConnectorConfig.getAddresses().get(0));
        Assert.assertEquals("http", serverConnectorConfig.getProtocol());
    }

    @Test
    public void testUpdateConsumerConfig() {
        Map<String, Object> extMap = new HashMap<>();
        ConfigurationImpl configuration = new ConfigurationImpl();
        configuration.setDefault();
        ConsumerConfigImpl consumerConfig = configuration.getConsumer();
        PolarisTrans.updateConsumerConfig(consumerConfig, extMap);
        LocalCacheConfigImpl cacheConfig = consumerConfig.getLocalCache();
        Assert.assertEquals("./polaris/backup", cacheConfig.getPersistDir());

        //非map结构
        extMap.put(PolarisConstant.POLARIS_LOCALCACHE, "not map");
        configuration = new ConfigurationImpl();
        configuration.setDefault();
        consumerConfig = configuration.getConsumer();

        PolarisTrans.updateConsumerConfig(consumerConfig, extMap);
        cacheConfig = consumerConfig.getLocalCache();
        Assert.assertEquals("./polaris/backup", cacheConfig.getPersistDir());

        Map<Object, Object> objectObjectMap = new HashMap<>();
        objectObjectMap.put(1L, 2L);
        extMap.put(PolarisConstant.POLARIS_LOCALCACHE, objectObjectMap);
        configuration = new ConfigurationImpl();
        configuration.setDefault();
        consumerConfig = configuration.getConsumer();

        PolarisTrans.updateConsumerConfig(consumerConfig, extMap);
        cacheConfig = consumerConfig.getLocalCache();
        Assert.assertEquals("./polaris/backup", cacheConfig.getPersistDir());

        //空 map
        Map<String, Object> localCache = new HashMap<>();
        extMap.put(PolarisConstant.POLARIS_LOCALCACHE, localCache);
        configuration = new ConfigurationImpl();
        configuration.setDefault();
        consumerConfig = configuration.getConsumer();
        PolarisTrans.updateConsumerConfig(consumerConfig, extMap);
        cacheConfig = consumerConfig.getLocalCache();
        Assert.assertEquals("./polaris/backup", cacheConfig.getPersistDir());

        localCache.put(PolarisConstant.POLARIS_LOCALCACHE_TYPE, "test_type");
        localCache.put(PolarisConstant.POLARIS_LOCALCACHE_PERSISTMAXREADRETRY, 2);
        localCache.put(PolarisConstant.POLARIS_LOCALCACHE_PERSISTMAXWRITERETRY, 3);
        localCache.put(PolarisConstant.POLARIS_LOCALCACHE_MAXEJECTPERCENTTHRESHOLD, 0.9);
        localCache.put(PolarisConstant.POLARIS_LOCALCACHE_PERSISTDIR, "/tmp");

        configuration = new ConfigurationImpl();
        configuration.setDefault();
        consumerConfig = configuration.getConsumer();

        PolarisTrans.updateConsumerConfig(consumerConfig, extMap);
        cacheConfig = consumerConfig.getLocalCache();
        Assert.assertEquals("/tmp", cacheConfig.getPersistDir());
        Assert.assertEquals(2, cacheConfig.getPersistMaxReadRetry());
        Assert.assertEquals(3, cacheConfig.getPersistMaxWriteRetry());
        Assert.assertEquals("test_type", cacheConfig.getType());
    }

    @Test
    public void  testGetPolarisInstanceId() {
        Instance instance = Mockito.mock(Instance.class);
        when(instance.getHost()).thenReturn("127.0.0.1");
        when(instance.getPort()).thenReturn(111);
        when(instance.isHealthy()).thenReturn(true);
        when(instance.getRevision()).thenReturn("1.0.0");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("set", "set.sz.1");
        metadata.put(PolarisConstant.POLARIS_ID, "sz0001");
        when(instance.getMetadata()).thenReturn(metadata);

        InstancesResponse instancesResponse = Mockito.mock(InstancesResponse.class);
        when(instancesResponse.getTotalWeight()).thenReturn(1000);
        when(instancesResponse.getNamespace()).thenReturn("dev");
        when(instancesResponse.getService()).thenReturn("trpc.test.test.1");
        when(instancesResponse.getInstances()).thenReturn(new Instance[]{instance});

        ServiceInstance serviceInstance = PolarisTrans
                .toServiceInstance(instancesResponse, instance);
        String instanceId = PolarisTrans.getPolarisInstanceId(serviceInstance);
        Assert.assertEquals("sz0001",instanceId);
    }

     @Test
    public void  testParseRouterResult() {
        Instance instance1 = Mockito.mock(Instance.class);
        when(instance1.getId()).thenReturn("10001");
        when(instance1.getHost()).thenReturn("10.0.0.1");
        when(instance1.getPort()).thenReturn(2);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("set", "set.sz.1");
        metadata.put(PolarisConstant.POLARIS_ID, "sz0001");
        when(instance1.getMetadata()).thenReturn(metadata);

        Instance instance2 = Mockito.mock(Instance.class);
        when(instance2.getId()).thenReturn("10002");
        when(instance2.getHost()).thenReturn("10.0.0.1");
        when(instance2.getPort()).thenReturn(1);
        List<Instance> instances = new ArrayList<>();
        instances.add(instance1);
        instances.add(instance2);
        InstancesResponse instancesResponse = Mockito.mock(InstancesResponse.class);
        when(instancesResponse.getTotalWeight()).thenReturn(1000);
        when(instancesResponse.getNamespace()).thenReturn("dev");
        when(instancesResponse.getService()).thenReturn("trpc.test.test.1");
        when(instancesResponse.getInstances()).thenReturn(instances.toArray(new Instance[0]));

        RouteResult routeResult = Mockito.mock(RouteResult.class);
        when(routeResult.getInstances()).thenReturn(instances);
        when(routeResult.getNextRouterInfo()).thenReturn(new NextRouterInfo(State.Next));

        List<ServiceInstance> serviceInstances = PolarisTrans.parseRouterResult(routeResult,instancesResponse);

        ServiceInstances convertServiceIns = PolarisCommon.toServiceInstances(serviceInstances);

        Assert.assertEquals(2,serviceInstances.size());
        Assert.assertEquals("sz0001",serviceInstances.get(0).getParameter(PolarisConstant.POLARIS_ID));
        Assert.assertEquals(2,convertServiceIns.getInstances().size());
    }

    @Test
    public void testGenCircuitBreakerConfiguration() {
        ConfigurationImpl configuration = new ConfigurationImpl();
        configuration.setDefault();
        Map<String, Object> extMap = new HashMap<>();
        extMap.put(PolarisConstant.POLARIS_ID, "sz0001");
        CircuitBreakerConfigImpl circuitBreakerConfig =
                PolarisCommon.genCircuitBreakerConfiguration(configuration,extMap);
        Assert.assertEquals(1,circuitBreakerConfig.getChain().size());
    }

    @Test
    public void testEmptyGenCircuitBreakerConfiguration() {
        ConfigurationImpl configuration = new ConfigurationImpl();
        configuration.setDefault();
        Map<String, Object> extMap = null;
        CircuitBreakerConfigImpl circuitBreakerConfig =
                PolarisCommon.genCircuitBreakerConfiguration(configuration,extMap);
        Assert.assertEquals(1,circuitBreakerConfig.getChain().size());
    }

    @Test
    public void testEmptyGenPolarisConfiguration() {
        Assert.assertNotNull(PolarisCommon.genPolarisConfiguration(null));
    }

    @Test
    public void testGetSetName() {
        Instance instance = Mockito.mock(Instance.class);
        when(instance.getHost()).thenReturn("127.0.0.1");
        when(instance.getPort()).thenReturn(111);
        when(instance.isHealthy()).thenReturn(true);
        when(instance.getRevision()).thenReturn("1.0.0");
        Map<String, String> metadata = new HashMap<>();
        metadata.put(PolarisConstant.INTERNAL_SET_NAME_KEY, "set.sz.1");
        metadata.put(PolarisConstant.INTERNAL_ENABLE_SET_KEY, "Y");
        when(instance.getMetadata()).thenReturn(metadata);
        Assert.assertEquals("set.sz.1",PolarisTrans.getSetName(instance));
    }

    @Test
    public void testGetContainerName() {
        Instance instance = Mockito.mock(Instance.class);
        when(instance.getHost()).thenReturn("127.0.0.1");
        when(instance.getPort()).thenReturn(111);
        when(instance.isHealthy()).thenReturn(true);
        when(instance.getRevision()).thenReturn("1.0.0");
        Map<String, String> metadata = new HashMap<>();
        metadata.put(PolarisConstant.CONTAINER_NAME, "xxx1");
        when(instance.getMetadata()).thenReturn(metadata);
        Assert.assertEquals("xxx1",PolarisTrans.getContainerName(instance));
    }

    @Test
    public void testPickMetaData() {
        Assert.assertNotNull(PolarisTrans.pickMetaData(new HashMap<>(),false,"sz11"));
    }

    @Test
    public void testEmptyTransfer2ServiceInstance() {
        Assert.assertEquals(0,PolarisTrans.transfer2ServiceInstance(null,null).size());
    }

    @Test
    public void testExceptionParseRouterResult() {
        try {
            PolarisTrans.parseRouterResult(null,null);
        } catch (Exception e) {
            return;
        }
    }
}
