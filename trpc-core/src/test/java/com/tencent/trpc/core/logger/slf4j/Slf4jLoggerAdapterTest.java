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

package com.tencent.trpc.core.logger.slf4j;

import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.logger.LoggerAdapter;
import com.tencent.trpc.core.logger.LoggerLevel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Slf4jLoggerAdapterTest {

    private Slf4jLoggerAdapter slf4jLoggerAdapter;
    private PluginConfig pluginConfig;

    @Before
    public void setUp() {
        this.slf4jLoggerAdapter = new Slf4jLoggerAdapter();
        this.pluginConfig = new PluginConfig("slf4j", LoggerAdapter.class, Slf4jLoggerAdapter.class);
        this.slf4jLoggerAdapter.setPluginConfig(pluginConfig);
    }

    @Test
    public void testInit() {
        slf4jLoggerAdapter.init();
    }

    @Test
    public void testGetLoggerWithName() {
        Assert.assertNotNull(slf4jLoggerAdapter.getLogger("a"));
    }

    @Test
    public void testGetLoggerWithClass() {
        Assert.assertNotNull(slf4jLoggerAdapter.getLogger(Slf4jLoggerAdapterTest.class));
    }

    @Test
    public void testGetLoggerLevel() {
        LoggerLevel loggerLevel = slf4jLoggerAdapter.getLoggerLevel();
        Assert.assertNull(loggerLevel);
    }

    @Test
    public void testSetLoggerLevel() {
        this.slf4jLoggerAdapter.setLoggerLevel(LoggerLevel.ALL);
        LoggerLevel loggerLevel = slf4jLoggerAdapter.getLoggerLevel();
        Assert.assertNull(loggerLevel);
    }

}
