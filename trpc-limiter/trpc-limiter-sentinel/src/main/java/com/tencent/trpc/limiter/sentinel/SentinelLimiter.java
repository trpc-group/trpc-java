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

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.LimiterBlockException;
import com.tencent.trpc.core.exception.LimiterException;
import com.tencent.trpc.core.exception.LimiterFallbackException;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.extension.InitializingExtension;
import com.tencent.trpc.core.extension.PluginConfigAware;
import com.tencent.trpc.core.limiter.spi.Limiter;
import com.tencent.trpc.core.limiter.spi.LimiterBlockHandler;
import com.tencent.trpc.core.limiter.spi.LimiterFallback;
import com.tencent.trpc.core.limiter.spi.LimiterResourceExtractor;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.limiter.sentinel.config.SentinelConfig;
import com.tencent.trpc.limiter.sentinel.config.SentinelLimiterConfig;
import com.tencent.trpc.limiter.sentinel.config.datasource.DatasourceConfig;
import java.util.concurrent.CompletionStage;

/**
 * Sentinel default flow control component implementation.
 */
@Extension("sentinel")
public class SentinelLimiter implements Limiter, PluginConfigAware, InitializingExtension {

    private static final Logger logger = LoggerFactory.getLogger(SentinelLimiter.class);

    /**
     * Sentinel plugin YAML configuration.
     */
    private PluginConfig sentinelPluginConfig;
    /**
     * Flow control callback plugin.
     */
    private LimiterBlockHandler limiterBlockHandler;
    /**
     * Flow control degradation plugin.
     */
    private LimiterFallback limiterFallback;
    /**
     * Flow control resource identifier parser.
     */
    private LimiterResourceExtractor limiterResourceExtractor;

    @Override
    public void setPluginConfig(PluginConfig pluginConfig) throws TRpcExtensionException {
        this.sentinelPluginConfig = pluginConfig;
    }

    @Override
    public void init() throws TRpcExtensionException {
        SentinelConfig sentinelConfig = SentinelConfig.parse(sentinelPluginConfig.getProperties());
        initLimiterConfig(sentinelConfig);
        registerDataSource(sentinelConfig);
    }

    /**
     * Initialize flow control configuration.
     *
     * @param sentinelConfig sentinel YAML configuration information
     */
    private void initLimiterConfig(SentinelConfig sentinelConfig) {
        SentinelLimiterConfig limiterConfig = sentinelConfig.getLimiterConfig();
        limiterBlockHandler = loadLimiterComponent(LimiterBlockHandler.class, limiterConfig.getBlockHandler());
        limiterResourceExtractor = loadLimiterComponent(LimiterResourceExtractor.class,
                limiterConfig.getResourceExtractor());
        limiterFallback = loadLimiterComponent(LimiterFallback.class, limiterConfig.getFallback());
    }

    /**
     * Load the flow control component by the specified component class and name, including callback, degradation,
     * resource identifier parser, etc. When the component does not exist, an exception is thrown.
     *
     * @param clazz component class
     * @param name component name
     * @param <T> component class generic
     * @return component instance
     */
    private <T> T loadLimiterComponent(Class<T> clazz, String name) {
        T limiterComponent = ExtensionLoader.getExtensionLoader(clazz).getExtension(name);
        if (limiterComponent == null) {
            throw new LimiterException("not found the limiter plugin(name=" + name + ",class=" + clazz.getName() + ")");
        }
        return limiterComponent;
    }

    /**
     * Register flow control rule data source.
     *
     * @param sentinelConfig sentinel YAML configuration information
     */
    private void registerDataSource(SentinelConfig sentinelConfig) {
        DatasourceConfig datasourceConfig = sentinelConfig.getDataSourceConfig();
        if (datasourceConfig == null) {
            logger.warn("not use datasource for sentinel flow rule");
            return;
        }
        datasourceConfig.register();
    }

    /**
     * Flow control entry method.
     *
     * Execution process:
     * <p>1. If the flow control condition is triggered, execute the flow control callback logic.</p>
     *
     * <p>2. If an exception occurs during the execution of the service method, execute the flow control degradation
     * logic.</p>
     *
     * <p>3. If the execution process does not trigger flow control and does not throw an exception, the method returns
     * the execution result normally.</p>
     *
     * @param filterChain method invocation
     * @param request client request entity
     * @return method invocation result
     */
    @Override
    public CompletionStage<Response> block(Invoker<?> filterChain, Request request) {
        Entry entry = null;
        String resource = limiterResourceExtractor.extract(filterChain, request);
        try {
            entry = SphU.entry(resource);
            return filterChain.invoke(request);
        } catch (BlockException e) {
            return limiterBlockHandler.handle(filterChain, request, new LimiterBlockException(e));
        } catch (Throwable t) {
            Tracer.traceEntry(t, entry);
            return limiterFallback.fallback(filterChain, request, new LimiterFallbackException(t));
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

}
