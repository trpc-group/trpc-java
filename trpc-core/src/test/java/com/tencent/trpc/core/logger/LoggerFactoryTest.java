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

package com.tencent.trpc.core.logger;

import com.tencent.trpc.core.common.ConfigManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LoggerFactoryTest {

    @Before
    public void before() {
        ConfigManager.stopTest();
        ConfigManager.startTest();
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

        Assert.assertTrue(logger.isDebugEnabled());
        Assert.assertTrue(logger.isTraceEnabled());
        Assert.assertTrue(logger.isInfoEnabled());
        Assert.assertTrue(logger.isWarnEnabled());
        Assert.assertTrue(logger.isErrorEnabled());
        Assert.assertNotNull(logger.getName());
        LoggerFactory.getLoggerLevel();

        LoggerFactory.setLoggerLevel(LoggerLevel.DEBUG);
    }

    @Test
    public void testGetLogger() {
        Logger logger = LoggerFactory.getLogger("logger");
        Assert.assertNotNull(logger);
    }

    @Test
    public void testGetRemoteLogger() {
        RemoteLogger test = LoggerFactory.getRemoteLogger("test");
        Assert.assertNotNull(test);
        Assert.assertTrue(test instanceof TestRemoteLogger);
    }
}
