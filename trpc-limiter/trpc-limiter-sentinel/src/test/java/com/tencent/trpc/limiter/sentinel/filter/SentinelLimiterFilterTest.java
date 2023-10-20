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

package com.tencent.trpc.limiter.sentinel.filter;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.filter.FilterOrdered;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.limiter.sentinel.SentinelLimiter;
import com.tencent.trpc.limiter.sentinel.TestSentinelInvoker;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * SentinelLimiterFilter test class
 */
public class SentinelLimiterFilterTest {

    private static final Logger logger = LoggerFactory.getLogger(SentinelLimiterFilterTest.class);

    @Before
    public void setUp() {
        try {
            Map<String, Object> extMap = new HashMap<>();
            Map<String, Object> datasource = new HashMap<>();
            Map<String, Object> fileDatasource = new HashMap<>();
            fileDatasource.put("path", "classpath:flow_rule.json");
            datasource.put("file", fileDatasource);
            extMap.put("datasource", datasource);
            ConfigManager.getInstance()
                    .registerPlugin(new PluginConfig("sentinel", SentinelLimiter.class, extMap));
        } catch (Exception e) {
            logger.error("set uo error:", e);
        }
    }

    @Test
    public void testFilter() {
        SentinelLimiterFilter sentinelLimiterFilter = new SentinelLimiterFilter();
        Assert.assertEquals(FilterOrdered.SENTINEL_LIMITER_ORDERED, sentinelLimiterFilter.getOrder());

        sentinelLimiterFilter.init();
        DefRequest request = new DefRequest();
        RpcInvocation rpcInvocationQps = new RpcInvocation();
        rpcInvocationQps.setFunc("/trpc.TestApp.TestServer.Greeter/sayHello");
        request.setInvocation(rpcInvocationQps);
        sentinelLimiterFilter.filter(new TestSentinelInvoker(), request);
    }

}
