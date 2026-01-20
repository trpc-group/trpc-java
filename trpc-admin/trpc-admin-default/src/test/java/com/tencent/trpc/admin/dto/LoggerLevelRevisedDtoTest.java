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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Log level modification view class
 */
public class LoggerLevelRevisedDtoTest {

    private LoggerLevelRevisedDto loggerLevelRevisedDto;

    @BeforeEach
    public void setUp() {
        this.loggerLevelRevisedDto = new LoggerLevelRevisedDto();
    }

    @Test
    public void testBuildFail() {
        LoggerLevelDto fail = LoggerLevelDto.buildFail("FAIL");
        Assertions.assertEquals(fail.getMessage(), "FAIL");
        Assertions.assertEquals(fail.getErrorcode(), CommonDto.FAIL);
    }

    @Test
    public void testGetLevel() {
        Assertions.assertNull(loggerLevelRevisedDto.getLevel());
    }

    @Test
    public void testSetLevel() {
        loggerLevelRevisedDto.setLevel("1");
        Assertions.assertEquals(loggerLevelRevisedDto.getLevel(), "1");
    }

    @Test
    public void testGetPrelevel() {
        Assertions.assertNull(loggerLevelRevisedDto.getPrelevel());
    }

    @Test
    public void testSetPrelevel() {
        loggerLevelRevisedDto.setPrelevel("2");
        Assertions.assertEquals(loggerLevelRevisedDto.getPrelevel(), "2");
    }

    @Test
    public void testToString() {
        Assertions.assertEquals(loggerLevelRevisedDto.toString(),
                "LoggerLevelRevisedDto{level='null', prelevel='null'} CommonDto{errorcode='0', message=''}");

    }
}
