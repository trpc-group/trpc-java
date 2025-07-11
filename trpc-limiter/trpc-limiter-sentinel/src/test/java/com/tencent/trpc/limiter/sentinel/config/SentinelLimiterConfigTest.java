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

package com.tencent.trpc.limiter.sentinel.config;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test limit callback, downgrade configuration
 */
public class SentinelLimiterConfigTest {

    @Test
    public void testParse() {
        Map<String, Object> limiterConfigMap = new HashMap<>();
        limiterConfigMap.put("block_handler", "test_handler");
        limiterConfigMap.put("fallback", "test_fallback");
        limiterConfigMap.put("resource_extractor", "test_resource_extractor");
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("limiter_config", limiterConfigMap);

        SentinelLimiterConfig sentinelLimiterConfig1 = SentinelLimiterConfig.parse(configMap);
        Assert.assertTrue("test_handler".equals(sentinelLimiterConfig1.getBlockHandler()));
        Assert.assertTrue("test_fallback".equals(sentinelLimiterConfig1.getFallback()));
        Assert.assertTrue("test_resource_extractor".equals(sentinelLimiterConfig1.getResourceExtractor()));

        SentinelLimiterConfig sentinelLimiterConfig2 = SentinelLimiterConfig.parse(null);
        Assert.assertTrue("default".equals(sentinelLimiterConfig2.getBlockHandler()));
        Assert.assertTrue("default".equals(sentinelLimiterConfig2.getFallback()));
        Assert.assertTrue("default".equals(sentinelLimiterConfig2.getResourceExtractor()));

        Map<String, Object> configMap3 = new HashMap<>();
        Map<String, Object> limiterConfigMap3 = new HashMap<>();
        limiterConfigMap3.put("block_handler", "test_handler3");
        limiterConfigMap3.put("fallback", "test_fallback3");
        configMap3.put("limiter_config", limiterConfigMap3);
        SentinelLimiterConfig sentinelLimiterConfig3 = SentinelLimiterConfig.parse(configMap3);
        Assert.assertTrue("test_handler3".equals(sentinelLimiterConfig3.getBlockHandler()));
        Assert.assertTrue("test_fallback3".equals(sentinelLimiterConfig3.getFallback()));
        Assert.assertTrue("default".equals(sentinelLimiterConfig3.getResourceExtractor()));
    }

}
