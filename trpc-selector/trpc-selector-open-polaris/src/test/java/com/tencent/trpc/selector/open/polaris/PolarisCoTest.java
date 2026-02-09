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

package com.tencent.trpc.selector.open.polaris;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.selector.SelectorManager;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.spi.Selector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


/**
 * polaris joint debugging testing class
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PolarisCoTest {

    @BeforeEach
    public void before() {
        ConfigManager.stopTest();
        DataTest.init();
        ConfigManager.startTest();
    }

    @AfterEach
    public void after() {
        ConfigManager.stopTest();
    }

    //@Test
    public void testReport() {
        PluginConfig selectorConfig = DataTest.createSelectorConfig();
        Selector clusterNaming = SelectorManager.getManager().get(selectorConfig.getName());

        ServiceInstance serviceInstance = DataTest.genServiceInstance(1);

        clusterNaming.report(serviceInstance, 0, 100L);
    }

    @Test
    public void testSelectAll() {

    }
}
