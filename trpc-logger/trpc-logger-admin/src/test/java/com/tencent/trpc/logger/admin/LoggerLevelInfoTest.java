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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LoggerLevelInfoTest {

    private LoggerLevelInfo loggerLevelInfo;

    @Before
    public void setUp() {
        loggerLevelInfo = new LoggerLevelInfo();
        loggerLevelInfo.setLoggerName("logger");
        loggerLevelInfo.setLevel(LoggerLevel.ALL.name());
    }

    @Test
    public void testGetLoggerName() {
        Assert.assertEquals("logger", loggerLevelInfo.getLoggerName());
    }

    @Test
    public void testGetLevel() {
        Assert.assertEquals(LoggerLevel.ALL.name(), loggerLevelInfo.getLevel());
    }

    @Test
    public void testTestToString() {
        Assert.assertTrue(loggerLevelInfo.toString().contains("LoggerLevelInfo"));
    }

}