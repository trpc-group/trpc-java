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

package com.tencent.trpc.opentelemetry;

import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.extension.InitializingExtension;
import com.tencent.trpc.core.extension.PluginConfigAware;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.telemetry.spi.TelemetryFactory;
import io.opentelemetry.api.internal.ConfigUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * The factory of OpenTelemetry extension, initialize OpenTelemetry SDK.
 */
@Extension(OpenTelemetryFactory.NAME)
public class OpenTelemetryFactory implements TelemetryFactory, PluginConfigAware, InitializingExtension {

    public static final String NAME = "opentelemetry";
    private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryFactory.class);

    private final AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder();
    private HashMap<String, String> telemetryConfig;

    @Override
    public void setPluginConfig(PluginConfig pluginConfig) throws TRpcExtensionException {
        Objects.requireNonNull(pluginConfig, "The OpenTelemetry plugin config can't be null");
        this.telemetryConfig = parseConfig(pluginConfig.getProperties());
    }

    private HashMap<String, String> parseConfig(Map<String, Object> properties) {
        HashMap<String, String> parsedConfig = new HashMap<>(properties.size());
        for (Entry<String, Object> configEntry : properties.entrySet()) {
            if (!(configEntry.getValue() instanceof String)) {
                continue;
            }
            parsedConfig.put(configEntry.getKey(),
                    ConfigUtil.normalizeEnvironmentVariableKey((String) configEntry.getValue()));
        }
        return parsedConfig;
    }

    /**
     * Initialize OpenTelemetry SDK
     *
     * @throws TRpcExtensionException extension initialize exception
     */
    @Override
    public void init() throws TRpcExtensionException {
        builder.setResultAsGlobal(true);
        builder.addPropertiesCustomizer(configProperties -> telemetryConfig);
        builder.build();
        logger.info("Initialize OpenTelemetry plugin success");
    }

}
