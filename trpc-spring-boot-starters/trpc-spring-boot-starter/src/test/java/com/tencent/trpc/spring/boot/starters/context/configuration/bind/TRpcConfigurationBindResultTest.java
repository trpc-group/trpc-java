/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2026 THL A29 Limited, a Tencent company.
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.spring.boot.starters.context.configuration.bind;

import com.tencent.trpc.spring.boot.starters.context.configuration.TRpcConfigurationProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TRpcConfigurationBindResultTest {

    @Test
    void toMapPreservesSnakeCaseConfiguration() {
        TRpcConfigurationProperties properties = new TRpcConfigurationProperties();
        properties.getGlobal().setEnvName("test-env");
        properties.getGlobal().setFullSetName("app.area.unit");
        properties.getServer().setLocalIp("127.0.0.1");
        properties.getServer().setRequestTimeout(1234);
        properties.getServer().setEnableLinkTimeout(false);
        properties.getServer().setRunListeners(List.of("test-listener"));
        properties.getClient().setRequestTimeout(2345);
        properties.getClient().setCallerServiceName("test-caller");

        Map<String, Object> result = new TRpcConfigurationBindResult(properties).toMap();

        Map<?, ?> global = (Map<?, ?>) result.get("global");
        Assertions.assertNotNull(global);
        Assertions.assertEquals("test-env", global.get("env_name"));
        Assertions.assertEquals("app.area.unit", global.get("full_set_name"));
        Assertions.assertFalse(global.containsKey("envName"));
        Map<?, ?> server = (Map<?, ?>) result.get("server");
        Assertions.assertNotNull(server);
        Assertions.assertEquals("127.0.0.1", server.get("local_ip"));
        Assertions.assertEquals(1234, server.get("request_timeout"));
        Assertions.assertEquals(false, server.get("enable_link_timeout"));
        Assertions.assertEquals(List.of("test-listener"), server.get("run_listeners"));
        Map<?, ?> client = (Map<?, ?>) result.get("client");
        Assertions.assertNotNull(client);
        Assertions.assertEquals(2345, client.get("request_timeout"));
        Assertions.assertEquals("test-caller", client.get("caller_service_name"));
    }

    @Test
    void toMapPreservesExtensionMapKeys() {
        TRpcConfigurationProperties properties = new TRpcConfigurationProperties();
        Map<String, Object> extension = Map.of("customKey", "value", "nested", Map.of("userKey", 17));
        properties.getGlobal().setExt(extension);

        Map<String, Object> result = new TRpcConfigurationBindResult(properties).toMap();

        Map<?, ?> global = (Map<?, ?>) result.get("global");
        Assertions.assertNotNull(global);
        Assertions.assertEquals(extension, global.get("ext"));
    }
}
