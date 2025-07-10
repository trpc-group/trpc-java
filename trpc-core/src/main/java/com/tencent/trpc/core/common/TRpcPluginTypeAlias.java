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

package com.tencent.trpc.core.common;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.tencent.trpc.core.configcenter.spi.ConfigurationLoader;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.limiter.spi.Limiter;
import com.tencent.trpc.core.logger.RemoteLoggerAdapter;
import com.tencent.trpc.core.metrics.spi.MetricsFactory;
import com.tencent.trpc.core.registry.spi.Registry;
import com.tencent.trpc.core.selector.spi.CircuitBreaker;
import com.tencent.trpc.core.selector.spi.Discovery;
import com.tencent.trpc.core.selector.spi.LoadBalance;
import com.tencent.trpc.core.selector.spi.Router;
import com.tencent.trpc.core.selector.spi.Selector;
import com.tencent.trpc.core.telemetry.spi.TelemetryFactory;
import com.tencent.trpc.core.trace.spi.TracerFactory;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * Plugin alias configuration.
 */
public class TRpcPluginTypeAlias {

    /**
     * Plugin alias configuration, supporting multiple aliases for a single plugin.
     */
    private static final Map<String, Class<?>> PLUGIN_CLASS_MAP = Maps.newHashMap();
    /**
     * Plugin class alias mapping.
     */
    private static final Map<Class<?>, String> PLUGIN_NAME_MAP = Maps.newHashMap();

    static {
        Arrays.stream(SystemPluginType.values()).forEach(pt -> {
            register(pt.getAlias(), pt.getPluginInterface());
            PLUGIN_NAME_MAP.putIfAbsent(pt.getPluginInterface(), pt.getAlias());
        });
    }

    public static synchronized void register(String alias, Class<?> clazz) {
        Objects.requireNonNull(clazz, "clazz is null");
        Preconditions.checkArgument(StringUtils.isNotBlank(alias), "alias is empty");
        Optional.ofNullable(PLUGIN_CLASS_MAP.get(alias)).ifPresent(cls -> {
            throw new IllegalArgumentException(String.format(
                    "Duplicate plugin alias:%s,plugin:%s, newPlugin:%s", alias, cls.getName(),
                    clazz.getName()));
        });
        PLUGIN_CLASS_MAP.put(alias, clazz);
    }

    /**
     * Get the plugin interface by alias.
     *
     * @param alias the alias of the plugin
     * @return the plugin interface class
     */
    public static Class<?> getPluginInterface(String alias) {
        return PLUGIN_CLASS_MAP.get(alias);
    }

    /**
     * Get the plugin name by class.
     *
     * @param clazz the plugin class
     * @return the plugin name
     */
    public static String getPluginName(Class<?> clazz) {
        return PLUGIN_NAME_MAP.get(clazz);
    }

    /**
     * System plugin types.
     */
    public enum SystemPluginType {

        /**
         * Worker pool.
         */
        WORKER_POOL("worker_pool", WorkerPool.class),

        /**
         * Remote configuration.
         */
        CONFIG("config", ConfigurationLoader.class),

        FILTER("filter", Filter.class),
        /**
         * Telemetry standard plugin.
         */
        TELEMETRY("telemetry", TelemetryFactory.class),
        /**
         * Link tracking OpenTracing standard plugin, currently not recommended for use.
         */
        @Deprecated
        TRACING("tracing", TracerFactory.class),
        /**
         * Route selector.
         */
        SELECTOR("selector", Selector.class),
        /**
         * Route discovery.
         */
        DISCOVERY("discovery", Discovery.class),
        /**
         * Load balancing.
         */
        LOADBALANCE("loadbalance", LoadBalance.class),
        /**
         * Circuit breaker.
         */
        CIRCUITBREAKER("circuitbreaker", CircuitBreaker.class),

        ROUTER("router", Router.class),

        REGISTRY("registry", Registry.class),
        /**
         * Remote log.
         */
        REMOTE_LOG("remote_log", RemoteLoggerAdapter.class),
        /**
         * Monitoring.
         */
        METRICS("metrics", MetricsFactory.class),
        /**
         * Rate limiting.
         */
        LIMITER("limiter", Limiter.class),
        ;

        private String alias;

        private Class<?> pluginInterface;

        SystemPluginType(String alias, Class<?> pluginClass) {
            this.alias = alias;
            this.pluginInterface = pluginClass;
        }

        /**
         * Get the alias of the plugin type.
         *
         * @return the alias
         */
        public String getAlias() {
            return alias;
        }

        /**
         * Get the plugin interface class.
         *
         * @return the plugin interface class
         */
        public Class<?> getPluginInterface() {
            return pluginInterface;
        }

    }

}
