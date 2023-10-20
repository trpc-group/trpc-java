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

package com.tencent.trpc.spring.boot.starters.context.configuration;

import com.tencent.trpc.spring.boot.starters.context.configuration.bind.TRpcConfigurationBinder;
import com.tencent.trpc.spring.context.configuration.schema.GlobalSchema;
import com.tencent.trpc.spring.context.configuration.schema.client.ClientSchema;
import com.tencent.trpc.spring.context.configuration.schema.plugin.PluginsSchema;
import com.tencent.trpc.spring.context.configuration.schema.server.ServerSchema;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * TRpc Spring Boot configuration properties class
 *
 * Note: Users should not generate this binding using {@code @EnableConfigurationProperties(TRpcConfigurationProperties.class)}
 *
 * If you need this configuration class, simply inject the bean (e.g., {@code @Autowired})
 *
 * @see TRpcConfigurationBinder
 */
@ConfigurationProperties(TRpcConfigurationProperties.PREFIX)
public class TRpcConfigurationProperties {

    public static final String PREFIX = "trpc";

    @NestedConfigurationProperty
    private GlobalSchema global = new GlobalSchema();

    @NestedConfigurationProperty
    private ServerSchema server = new ServerSchema();

    @NestedConfigurationProperty
    private ClientSchema client = new ClientSchema();

    @NestedConfigurationProperty
    private PluginsSchema plugins = new PluginsSchema();

    public GlobalSchema getGlobal() {
        return global;
    }

    public void setGlobal(GlobalSchema global) {
        this.global = global;
    }

    public ServerSchema getServer() {
        return server;
    }

    public void setServer(ServerSchema server) {
        this.server = server;
    }

    public ClientSchema getClient() {
        return client;
    }

    public void setClient(ClientSchema client) {
        this.client = client;
    }

    public PluginsSchema getPlugins() {
        return plugins;
    }

    public void setPlugins(PluginsSchema plugins) {
        this.plugins = plugins;
    }

    @Override
    public String toString() {
        return "TRpcConfigurationProperties{"
                + "global=" + global
                + ", server=" + server
                + ", client=" + client
                + ", plugins=" + plugins
                + '}';
    }
}
