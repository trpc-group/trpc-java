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

package com.tencent.trpc.admin.dto;

import com.tencent.trpc.logger.admin.LoggerLevelInfo;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Log level view test class
 */
public class LoggerLevelDtoTest {

    private LoggerLevelDto loggerLevelDto;

    @Before
    public void setUp() {
        this.loggerLevelDto = new LoggerLevelDto();
    }

    @Test
    public void testBuildFail() {
        LoggerLevelDto fail = LoggerLevelDto.buildFail("FAIL");
        Assert.assertEquals(fail.getMessage(), "FAIL");
        Assert.assertEquals(fail.getErrorcode(), CommonDto.FAIL);
    }

    @Test
    public void testGetLogger() {
        Assert.assertNull(loggerLevelDto.getLogger());
    }

    @Test
    public void testSetLogger() {
        loggerLevelDto.setLogger(new ArrayList<>());
        Assert.assertEquals(loggerLevelDto.getLogger().size(), 0);
        loggerLevelDto.getLogger().add(new LoggerLevelInfo());
        Assert.assertEquals(loggerLevelDto.getLogger().size(), 1);

    }

    @Test
    public void testToString() {
        Assert.assertEquals(loggerLevelDto.toString(),
                "LoggerLevelDto{logger=null} CommonDto{errorcode='0', message=''}");
    }
}