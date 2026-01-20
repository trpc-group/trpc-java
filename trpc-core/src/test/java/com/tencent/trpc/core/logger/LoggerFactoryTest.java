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

package com.tencent.trpc.core.logger;

import com.tencent.trpc.core.common.ConfigManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LoggerFactoryTest {

    @BeforeEach
    public void before() {
        ConfigManager.stopTest();
        ConfigManager.startTest();
    }

    @AfterEach
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

        Assertions.assertTrue(logger.isDebugEnabled());
        Assertions.assertTrue(logger.isTraceEnabled());
        Assertions.assertTrue(logger.isInfoEnabled());
        Assertions.assertTrue(logger.isWarnEnabled());
        Assertions.assertTrue(logger.isErrorEnabled());
        Assertions.assertNotNull(logger.getName());
        LoggerFactory.getLoggerLevel();

        LoggerFactory.setLoggerLevel(LoggerLevel.DEBUG);
    }

    @Test
    public void testGetLogger() {
        Logger logger = LoggerFactory.getLogger("logger");
        Assertions.assertNotNull(logger);
    }

    @Test
    public void testGetRemoteLogger() {
        RemoteLogger test = LoggerFactory.getRemoteLogger("test");
        Assertions.assertNotNull(test);
        Assertions.assertTrue(test instanceof TestRemoteLogger);
    }
}
