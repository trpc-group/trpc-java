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

package com.tencent.trpc.admin;

import com.tencent.trpc.admin.dto.CommonDto;
import com.tencent.trpc.admin.dto.ConfigOverviewDto;
import com.tencent.trpc.admin.impl.ConfigAdmin;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.GlobalConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.Assert;
import org.junit.Test;

public class ConfigAdminTest {

    @Test
    public void test() {
        ConfigManager applicationConfig = ConfigManager.getInstance();
        applicationConfig.setServerConfig(new ServerConfig());
        applicationConfig.setGlobalConfig(new GlobalConfig());
        Map<String, ProtocolConfig> protocolConfigMap = new HashMap<>();
        protocolConfigMap.put("META-INF/trpc", new ProtocolConfig());
        // applicationConfig.setProtocolConfigMap(protocolConfigMap);
        ConfigAdmin configAdmin = new ConfigAdmin();
        ConfigOverviewDto configOverviewDto = configAdmin.getServerConfigInfo();
        configOverviewDto.toString();
        configOverviewDto.setContent(configOverviewDto.getContent());
        Assert.assertTrue(CommonDto.SUCCESS.equals(configOverviewDto.getErrorcode()));
        Assert.assertTrue(Objects.nonNull(configOverviewDto.getContent()));
    }

}
