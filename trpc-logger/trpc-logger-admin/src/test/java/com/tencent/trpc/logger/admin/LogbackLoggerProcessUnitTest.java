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

package com.tencent.trpc.logger.admin;

import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.tencent.trpc.core.logger.LoggerLevel;
import com.tencent.trpc.core.logger.slf4j.Slf4jLogger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.LoggerFactory;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LoggerFactory.class})
public class LogbackLoggerProcessUnitTest {

    public static final String LOGGER = "logger";
    public static final String LOGGER_ROOT = "ROOT";
    public static final String LOGGER_DEBUG = "DEBUG";

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private LogbackLoggerProcessUnit logger;

    @Before
    public void setUp() throws Exception {
        logger = new LogbackLoggerProcessUnit();
        logger.setLoggerLevel(LOGGER, LoggerLevel.ALL);
        logger.addLogger(LOGGER, new Slf4jLogger(org.slf4j.LoggerFactory.getLogger(LOGGER)));
    }

    @Test
    public void testInit() {
        expectedEx.expect(ClassCastException.class);
        expectedEx.expectMessage("cannot be cast");
        logger.init();
    }

    @Test
    @PrepareForTest({LoggerFactory.class})
    public void testInitSuccess() {
        mockStatic(LoggerFactory.class);
        when(LoggerFactory.getILoggerFactory()).thenReturn(new LoggerContext());
        logger.init();
    }

    @Test
    public void testGetLoggerLevelInfoByError() {
        expectedEx.expect(ClassCastException.class);
        expectedEx.expectMessage("cannot be cast");
        Assert.assertNotNull(logger.getLoggerLevelInfo());
    }

    @Test
    public void testGetLoggerLevelInfo() {
        addLoggerToUnit();
        List<LoggerLevelInfo> info = logger.getLoggerLevelInfo();
        Assert.assertNotNull(info);
    }

    @Test
    public void testSetLogger() {
        addLoggerToUnit();
        String loggerLevel = logger.setLoggerLevel(LOGGER, LoggerLevel.ALL);
        Assert.assertEquals(LOGGER_DEBUG, loggerLevel);
    }

    private void addLoggerToUnit() {
        LoggerContext loggerContext = new LoggerContext();
        Logger logger = loggerContext.getLogger(LOGGER_ROOT);
        this.logger.addLogger(LOGGER, logger);
    }

}