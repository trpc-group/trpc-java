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

package com.tencent.trpc.limiter.sentinel;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.LimiterBlockException;
import com.tencent.trpc.core.exception.LimiterFallbackException;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.limiter.spi.Limiter;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.def.DefRequest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

/**
 * SentinelLimiter test class
 */
public class SentinelLimiterTest  {

    @Test
    public void testBlock() {
        Map<String, Object> extMap = new HashMap<>();
        Map<String, Object> datasource = new HashMap<>();
        Map<String, Object> fileDatasource = new HashMap<>();
        fileDatasource.put("path", "classpath:flow_rule.json");
        datasource.put("file", fileDatasource);
        Map<String, Object> limiterConfigMap = new HashMap<>();
        extMap.put("datasource", datasource);
        extMap.put("limiter_config", limiterConfigMap);

        DefRequest requestQps = new DefRequest();
        RpcInvocation rpcInvocationQps = new RpcInvocation();
        rpcInvocationQps.setFunc("/trpc.TestApp.TestServer.Greeter/sayHello");
        requestQps.setInvocation(rpcInvocationQps);

        ConfigManager.getInstance()
                .registerPlugin(new PluginConfig("sentinel01", SentinelLimiter.class, extMap));
        SentinelLimiter limiter01 = (SentinelLimiter) ExtensionLoader
                .getExtensionLoader(Limiter.class)
                .getExtension("sentinel01");
        while (true) {
            try {
                limiter01.block(new TestSentinelInvoker(), requestQps).toCompletableFuture().get().getException();
            } catch (Exception exception) {
                Throwable cause = exception.getCause();
                Assertions.assertTrue(
                        (cause instanceof LimiterBlockException)
                                || (cause instanceof LimiterFallbackException));
                break;
            }
        }

        // Verify that datasource is not configured
        extMap.remove("datasource");
        ConfigManager.getInstance()
                .registerPlugin(new PluginConfig("sentinel02", SentinelLimiter.class, extMap));
        SentinelLimiter limiter02 = (SentinelLimiter) ExtensionLoader
                .getExtensionLoader(Limiter.class)
                .getExtension("sentinel02");
        try {
            DefRequest request = new DefRequest();
            RpcInvocation rpcInvocation = new RpcInvocation();
            rpcInvocation.setFunc("/trpc.TestApp.TestServer.Greeter/sayHello/exception");
            request.setInvocation(rpcInvocation);

            limiter02.block(new TestSentinelIndexOutExceptionInvoker(), request).toCompletableFuture().get()
                    .getException();
        } catch (Exception exception) {
            Throwable cause = exception.getCause();
            Assertions.assertTrue(cause instanceof LimiterFallbackException);
        }
    }

}
