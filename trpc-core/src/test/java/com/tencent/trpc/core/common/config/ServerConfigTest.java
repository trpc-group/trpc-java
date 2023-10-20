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

import org.junit.Test;

public class ServerConfigTest {

    @Test
    public void testDefaultConfig() {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setDisableDefaultFilter(false);
        serverConfig.getServiceMap().put("server1", new ServiceConfig());
        serverConfig.setDefault();
    }

    @Test
    public void testNic() {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setNic("eth1");
        serverConfig.setDisableDefaultFilter(false);
        serverConfig.getServiceMap().put("server1", new ServiceConfig());
        serverConfig.setDefault();
    }
}