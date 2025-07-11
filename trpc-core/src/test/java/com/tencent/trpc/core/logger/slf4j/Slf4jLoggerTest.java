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

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.logger.LoggerFactoryTest;
import com.tencent.trpc.core.logger.LoggerLevel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.helpers.NOPLogger;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LoggerFactory.class})
public class Slf4jLoggerTest {

    @Before
    public void before() {
        ConfigManager.stopTest();
        ConfigManager.startTest();
        PowerMockito.mockStatic(LoggerFactory.class);
        PowerMockito.when(LoggerFactory.getLogger(LoggerFactoryTest.class))
                .thenReturn(new Slf4jLogger(NOPLogger.NOP_LOGGER));
        PowerMockito.when(LoggerFactory.getLogger("logger"))
                .thenReturn(new Slf4jLogger(NOPLogger.NOP_LOGGER));
    }

    @After
    public void after() {
        ConfigManager.stopTest();
    }

    @Test
    public void buildNormalTest() {
        Logger logger = LoggerFactory.getLogger(LoggerFactoryTest.class);
        String name = "9527";
        Exception e = new Exception("test");
        logger.trace("hello world");
        logger.trace("hello world %s", name);
        logger.trace("hello world", e);

        logger.debug("hello world");
        logger.debug("hello world %s", name);
        logger.debug("hello world", e);

        logger.info("hello world");
        logger.info("hello world %s", name);
        logger.info("hello world", e);

        logger.warn("hello world");
        logger.warn("hello world %s", name);
        logger.warn("hello world", e);

        logger.error("hello world");
        logger.error("hello world %s", name);
        logger.error("hello world %s", e);
        logger.error("hello world", e);

        Assert.assertFalse(logger.isDebugEnabled());
        Assert.assertFalse(logger.isTraceEnabled());
        Assert.assertFalse(logger.isInfoEnabled());
        Assert.assertFalse(logger.isWarnEnabled());
        Assert.assertFalse(logger.isErrorEnabled());
        Assert.assertNotNull(logger.getName());
        LoggerFactory.getLoggerLevel();

        LoggerFactory.setLoggerLevel(LoggerLevel.DEBUG);
    }

    @Test
    public void testGetLogger() {
        Logger logger = LoggerFactory.getLogger("logger");
        Assert.assertNotNull(logger);
    }

}
