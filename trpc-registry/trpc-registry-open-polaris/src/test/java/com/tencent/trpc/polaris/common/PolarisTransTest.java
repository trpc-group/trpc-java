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

package com.tencent.trpc.polaris.common;

import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.POLARIS_ADDRESSES_KEY;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.POLARIS_API_BIND_IF_KEY;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.POLARIS_API_MAX_RETRY_TIMES_KEY;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.POLARIS_PROTOCOL_KEY;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.POLARIS_RUN_MODE_KEY;

import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.global.APIConfigImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class PolarisTransTest {

    @Test
    public void testTrans2StringMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("test", "test");
        Map<String, String> strMap = PolarisRegisterUtil.trans2StringMap(map);
        Assert.assertEquals("test", strMap.get("test"));

        map = new HashMap<>();
        map.put("test", 1L);
        strMap = PolarisRegisterUtil.trans2StringMap(map);
        Assert.assertEquals("1", strMap.get("test"));

        map = new HashMap<>();
        Map<String, Integer> notString = new HashMap<>();
        notString.put("test", 2);
        map.put("test", notString);
        strMap = PolarisRegisterUtil.trans2StringMap(map);
        Assert.assertEquals("{\"test\":2}", strMap.get("test"));
    }

    @Test
    public void testTrans2ApiConfig() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put(POLARIS_API_MAX_RETRY_TIMES_KEY, 10);
        extMap.put(POLARIS_API_BIND_IF_KEY, "if");
        ConfigurationImpl configuration = new ConfigurationImpl();
        configuration.setDefault();
        APIConfigImpl apiConfig = configuration.getGlobal().getAPI();
        PolarisRegisterUtil.overrideApiConfig(apiConfig, extMap);
        Assert.assertEquals(10, apiConfig.getMaxRetryTimes());
        Assert.assertEquals("if", apiConfig.getBindIf());
    }

    @Test
    public void testTrans2ServerConnectorConfig() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put(POLARIS_RUN_MODE_KEY, 0);
        extMap.put(POLARIS_ADDRESSES_KEY, "10.0.0.1:1239");
        extMap.put(POLARIS_PROTOCOL_KEY, "http");
        ConfigurationImpl configuration = new ConfigurationImpl();
        configuration.setDefault();
        ServerConnectorConfigImpl serverConnectorConfig = configuration.getGlobal().getServerConnector();
        PolarisRegisterUtil.overrideServerConnectorConfig(serverConnectorConfig, extMap);
        Assert.assertEquals("10.0.0.1:1239", serverConnectorConfig.getAddresses().get(0));
        Assert.assertEquals("http", serverConnectorConfig.getProtocol());
    }
}
