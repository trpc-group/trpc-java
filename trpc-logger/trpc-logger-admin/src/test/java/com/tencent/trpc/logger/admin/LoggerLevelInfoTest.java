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

import com.tencent.trpc.core.logger.LoggerLevel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LoggerLevelInfoTest {

    private LoggerLevelInfo loggerLevelInfo;

    @BeforeEach
    public void setUp() {
        loggerLevelInfo = new LoggerLevelInfo();
        loggerLevelInfo.setLoggerName("logger");
        loggerLevelInfo.setLevel(LoggerLevel.ALL.name());
    }

    @Test
    public void testGetLoggerName() {
        Assertions.assertEquals("logger", loggerLevelInfo.getLoggerName());
    }

    @Test
    public void testGetLevel() {
        Assertions.assertEquals(LoggerLevel.ALL.name(), loggerLevelInfo.getLevel());
    }

    @Test
    public void testTestToString() {
        Assertions.assertTrue(loggerLevelInfo.toString().contains("LoggerLevelInfo"));
    }

}
