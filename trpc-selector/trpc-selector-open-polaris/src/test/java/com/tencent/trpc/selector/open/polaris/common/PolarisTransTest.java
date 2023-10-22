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

import com.google.common.collect.Maps;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.consumer.ConsumerConfigImpl;
import com.tencent.polaris.factory.config.consumer.LocalCacheConfigImpl;
import com.tencent.polaris.factory.config.global.APIConfigImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.trpc.polaris.common.PolarisConstant;
import com.tencent.trpc.polaris.common.PolarisTrans;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

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
}
