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

package com.tencent.trpc.spring.boot.starters.context.configuration.bind;

import com.google.common.collect.Lists;
import com.tencent.trpc.spring.boot.starters.context.configuration.TRpcConfigurationProperties;
import com.tencent.trpc.spring.boot.starters.context.configuration.bind.handler.TRpcConfigurationBindHandler;
import java.util.List;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * Binds TRpc configuration from {@link Environment}
 */
public class TRpcConfigurationBinder {

    private final ConfigurableEnvironment environment;

    /**
     * Successfully bind properties
     *
     * @see BindResultDescription Binding result description
     */
    private final List<ConfigurationProperty> bound = Lists.newArrayList();

    public TRpcConfigurationBinder(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    public TRpcConfigurationBindResult bind() {
        // bind to object for type-safe purpose
        TRpcConfigurationProperties properties = doBind();
        return new TRpcConfigurationBindResult(properties);
    }

    public TRpcConfigurationProperties doBind() {
        Binder binder = Binder.get(environment);
        BindResult<TRpcConfigurationProperties> result = binder.bind(TRpcConfigurationProperties.PREFIX,
                Bindable.of(TRpcConfigurationProperties.class), getBindHandler());
        return result.orElseGet(TRpcConfigurationProperties::new);
    }

    public BindResultDescription getResultDescription() {
        return new BindResultDescription(environment, bound);
    }

    private BindHandler getBindHandler() {
        return new TRpcConfigurationBindHandler(bound::add);
    }
}