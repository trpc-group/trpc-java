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

package com.tencent.trpc.container.config.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.tencent.trpc.container.config.YamlUtils;
import com.tencent.trpc.container.container.DefaultServerListener;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.container.spi.ServerListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * ServerConfigParser test class
 */
public class ServerConfigParserTest {

    private YamlUtils yamlUtils;

    private List<Map<String, Object>> serverListenerConfig;

    @BeforeEach
    public void before() {
        this.yamlUtils = new YamlUtils("");
    }

    @Test
    public void testParseDefaultStartedListeners() {
        ServerConfigParser serverConfigParser = new ServerConfigParser();
        Assertions.assertNotNull(serverConfigParser);
        List<ServerListener> serverListeners = ServerConfigParser.parseStartedListeners(yamlUtils,
                serverListenerConfig);
        assertNotNull(serverListeners);
        assertEquals(serverListeners.size(), 0);
        try {
            ServerConfig serverConfig = ServerConfigParser.parseServerConfig(null, null);
            Assertions.assertNotNull(serverConfig);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }


    @Test
    public void testParseStartedListeners() {
        serverListenerConfig = new ArrayList<>();
        Map<String, Object> defaultServerListener = new HashMap<>();
        defaultServerListener.put("listener_class", "com.tencent.trpc.container.container.DefaultServerListener");
        serverListenerConfig.add(defaultServerListener);
        List<ServerListener> serverListeners = ServerConfigParser.parseStartedListeners(yamlUtils,
                serverListenerConfig);
        assertNotNull(serverListeners);
        assertTrue(serverListeners.get(0) instanceof DefaultServerListener);
    }

    @Test
    public void testParseStartedListenersEx() {
        serverListenerConfig = new ArrayList<>();
        Map<String, Object> defaultServerListener = new HashMap<>();
        defaultServerListener.put("listener_class", "com.tencent.trpc.container.container.DefaultServerListener1");
        serverListenerConfig.add(defaultServerListener);
        try {
            ServerConfigParser.parseStartedListeners(yamlUtils, serverListenerConfig);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

}
