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

package com.tencent.trpc.admin.dto;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Log level modification view class
 */
public class LoggerLevelRevisedDtoTest {

    private LoggerLevelRevisedDto loggerLevelRevisedDto;

    @Before
    public void setUp() {
        this.loggerLevelRevisedDto = new LoggerLevelRevisedDto();
    }

    @Test
    public void testBuildFail() {
        LoggerLevelDto fail = LoggerLevelDto.buildFail("FAIL");
        Assert.assertEquals(fail.getMessage(), "FAIL");
        Assert.assertEquals(fail.getErrorcode(), CommonDto.FAIL);
    }

    @Test
    public void testGetLevel() {
        Assert.assertNull(loggerLevelRevisedDto.getLevel());
    }

    @Test
    public void testSetLevel() {
        loggerLevelRevisedDto.setLevel("1");
        Assert.assertEquals(loggerLevelRevisedDto.getLevel(), "1");
    }

    @Test
    public void testGetPrelevel() {
        Assert.assertNull(loggerLevelRevisedDto.getPrelevel());
    }

    @Test
    public void testSetPrelevel() {
        loggerLevelRevisedDto.setPrelevel("2");
        Assert.assertEquals(loggerLevelRevisedDto.getPrelevel(), "2");
    }

    @Test
    public void testToString() {
        Assert.assertEquals(loggerLevelRevisedDto.toString(),
                "LoggerLevelRevisedDto{level='null', prelevel='null'} CommonDto{errorcode='0', message=''}");

    }
}