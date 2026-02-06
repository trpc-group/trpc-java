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
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.telemetry.spi.TelemetryFactory;
import io.opentelemetry.api.GlobalOpenTelemetry;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OpenTelemetryFactoryTest {

    @Test
    public void factoryInitTest() {
        GlobalOpenTelemetry.resetForTest(); // Reset the global instance before each test
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("endpoint", "http://test.com");
        extMap.put("platform", "123");
        PluginConfig pluginConfig = new PluginConfig(OpenTelemetryFactory.NAME, OpenTelemetryFactory.class, extMap);
        ExtensionLoader.registerPlugin(pluginConfig);
        OpenTelemetryFactory factory = (OpenTelemetryFactory) ExtensionLoader.getExtensionLoader(
                TelemetryFactory.class).getExtension(OpenTelemetryFactory.NAME);
        Assertions.assertNotNull(factory);
    }

}
