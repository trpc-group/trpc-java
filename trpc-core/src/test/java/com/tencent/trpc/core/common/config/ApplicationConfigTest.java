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

package com.tencent.trpc.core.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tencent.trpc.core.common.ConfigManager;
import org.junit.jupiter.api.Test;

public class ApplicationConfigTest {

    @Test
    public void test() {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setContainerName("trpc");
        globalConfig.setEnvName("test");
        globalConfig.setNamespace("trpc");
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setConfigCenter("tconf");
        ClientConfig clientConfig = new ClientConfig();
        ConfigManager applicationConfig = ConfigManager.getInstance();
        applicationConfig.setGlobalConfig(globalConfig);
        applicationConfig.setServerConfig(serverConfig);
        applicationConfig.setClientConfig(clientConfig);
        assertEquals(applicationConfig.getGlobalConfig().getEnvName(), "test");
    }
}
