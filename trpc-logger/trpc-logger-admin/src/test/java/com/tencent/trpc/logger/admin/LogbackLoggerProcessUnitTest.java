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

import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.tencent.trpc.core.logger.LoggerLevel;
import com.tencent.trpc.core.logger.slf4j.Slf4jLogger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.slf4j.LoggerFactory;
import java.util.List;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class LogbackLoggerProcessUnitTest {

    public static final String LOGGER = "logger";
    public static final String LOGGER_ROOT = "ROOT";
    public static final String LOGGER_DEBUG = "DEBUG";

    private LogbackLoggerProcessUnit logger;

    @BeforeEach
    public void setUp() throws Exception {
        logger = new LogbackLoggerProcessUnit();
        logger.setLoggerLevel(LOGGER, LoggerLevel.ALL);
        logger.addLogger(LOGGER, new Slf4jLogger(org.slf4j.LoggerFactory.getLogger(LOGGER)));
    }

    @Test
    public void testInit() {
        ClassCastException exception = assertThrows(ClassCastException.class,
                () -> logger.init());
        assertTrue(exception.getMessage().contains("cannot be cast"));
    }

    @Test
    public void testInitSuccess() {
        try (MockedStatic<LoggerFactory> mockedLoggerFactory = Mockito.mockStatic(LoggerFactory.class)) {
            when(LoggerFactory.getILoggerFactory()).thenReturn(new LoggerContext());
            logger.init();
        }
    }

    @Test
    public void testGetLoggerLevelInfoByError() {
        ClassCastException exception = assertThrows(ClassCastException.class,
                () -> logger.getLoggerLevelInfo());
        assertTrue(exception.getMessage().contains("cannot be cast"));
    }

    @Test
    public void testGetLoggerLevelInfo() {
        addLoggerToUnit();
        List<LoggerLevelInfo> info = logger.getLoggerLevelInfo();
        Assertions.assertNotNull(info);
    }

    @Test
    public void testSetLogger() {
        addLoggerToUnit();
        String loggerLevel = logger.setLoggerLevel(LOGGER, LoggerLevel.ALL);
        Assertions.assertEquals(LOGGER_DEBUG, loggerLevel);
    }

    private void addLoggerToUnit() {
        LoggerContext loggerContext = new LoggerContext();
        Logger logger = loggerContext.getLogger(LOGGER_ROOT);
        this.logger.addLogger(LOGGER, logger);
    }

}
