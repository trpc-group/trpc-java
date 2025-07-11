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

package com.tencent.trpc.core.logger.slf4j;

import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.InitializingExtension;
import com.tencent.trpc.core.extension.PluginConfigAware;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerAdapter;
import com.tencent.trpc.core.logger.LoggerLevel;
import java.util.Objects;

@SuppressWarnings("unchecked")
public class Slf4jLoggerAdapter implements LoggerAdapter, PluginConfigAware, InitializingExtension {

    private PluginConfig config;

    @Override
    public void init() throws TRpcExtensionException {
        Objects.requireNonNull(config, "config");
    }

    @Override
    public void setPluginConfig(PluginConfig pluginConfig) throws TRpcExtensionException {
        config = pluginConfig;
    }

    @Override
    public LoggerLevel getLoggerLevel() {
        return null;
    }

    @Override
    public void setLoggerLevel(LoggerLevel loggerLevel) {
    }

    @Override
    public Logger getLogger(String key) {
        return getLogger(org.slf4j.LoggerFactory.getLogger(key));
    }

    @Override
    public Logger getLogger(Class<?> key) {
        return getLogger(org.slf4j.LoggerFactory.getLogger(key));
    }

    private Logger getLogger(org.slf4j.Logger logger) {
        return new Slf4jLogger(logger);
    }

}