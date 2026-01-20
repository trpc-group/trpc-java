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

import com.tencent.trpc.logger.admin.LoggerLevelInfo;
import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Log level view test class
 */
public class LoggerLevelDtoTest {

    private LoggerLevelDto loggerLevelDto;

    @BeforeEach
    public void setUp() {
        this.loggerLevelDto = new LoggerLevelDto();
    }

    @Test
    public void testBuildFail() {
        LoggerLevelDto fail = LoggerLevelDto.buildFail("FAIL");
        Assertions.assertEquals(fail.getMessage(), "FAIL");
        Assertions.assertEquals(fail.getErrorcode(), CommonDto.FAIL);
    }

    @Test
    public void testGetLogger() {
        Assertions.assertNull(loggerLevelDto.getLogger());
    }

    @Test
    public void testSetLogger() {
        loggerLevelDto.setLogger(new ArrayList<>());
        Assertions.assertEquals(loggerLevelDto.getLogger().size(), 0);
        loggerLevelDto.getLogger().add(new LoggerLevelInfo());
        Assertions.assertEquals(loggerLevelDto.getLogger().size(), 1);

    }

    @Test
    public void testToString() {
        Assertions.assertEquals(loggerLevelDto.toString(),
                "LoggerLevelDto{logger=null} CommonDto{errorcode='0', message=''}");
    }
}
