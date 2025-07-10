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

package com.tencent.trpc.admin;

import com.tencent.trpc.admin.dto.CommonDto;
import com.tencent.trpc.admin.dto.LoggerLevelDto;
import com.tencent.trpc.admin.dto.LoggerLevelRevisedDto;
import com.tencent.trpc.admin.impl.LoggerAdmin;
import java.util.Objects;
import org.junit.Assert;
import org.junit.Test;

public class LoggerAdminTest {

    @Test
    public void testGetLoggerLevel() {
        LoggerAdmin loggerAdmin = new LoggerAdmin();
        LoggerLevelDto loggerLevelDto = loggerAdmin.getLoggerLevelInfo();
        loggerLevelDto.toString();
        Assert.assertTrue(Objects.nonNull(CommonDto.SUCCESS.equals(loggerLevelDto.getErrorcode())));
        Assert.assertTrue(Objects.nonNull(loggerLevelDto.getLogger()));
    }

    @Test
    public void testSetLoggerLevelSuccess() {
        LoggerAdmin loggerAdmin = new LoggerAdmin();
        LoggerLevelRevisedDto loggerLevelRevisedDto = loggerAdmin.setLoggerLevel("ROOT", "INFO");
        LoggerLevelRevisedDto loggerLevelRevisedDto2 = loggerAdmin.setLoggerLevel("root", "INFO");
        Assert.assertTrue(
                CommonDto.SUCCESS.equals(loggerLevelRevisedDto.getErrorcode()) || CommonDto.SUCCESS
                        .equals(loggerLevelRevisedDto2.getErrorcode()));
    }

    @Test
    public void testSetLoggerLevelWithInvalidLevel() {
        LoggerAdmin loggerAdmin = new LoggerAdmin();
        LoggerLevelRevisedDto loggerLevelRevisedDto = loggerAdmin.setLoggerLevel("ROOT", "1234");
        Assert.assertTrue(CommonDto.FAIL.equals(loggerLevelRevisedDto.getErrorcode()));
    }

    @Test
    public void testSetLoggerLevelWithInvalidLogger() {
        LoggerAdmin loggerAdmin = new LoggerAdmin();
        LoggerLevelRevisedDto loggerLevelRevisedDto = loggerAdmin.setLoggerLevel("123", "INFO");
        loggerLevelRevisedDto.toString();
        Assert.assertTrue(CommonDto.FAIL.equals(loggerLevelRevisedDto.getErrorcode()));
    }

    @Test
    public void testLoggerLevelDtoBuildFail() {
        String message = "error";
        LoggerLevelDto loggerLevelDto = LoggerLevelDto.buildFail(message);
        Assert.assertTrue(CommonDto.FAIL.equals(loggerLevelDto.getErrorcode()));
        Assert.assertTrue(message.equals(loggerLevelDto.getMessage()));
    }

    @Test
    public void testLoggerLevelDtoSet() {
        String errorCode = CommonDto.SUCCESS;
        String message = "error";
        LoggerLevelDto loggerLevelDto = new LoggerLevelDto();
        loggerLevelDto.setErrorcode(errorCode);
        loggerLevelDto.setMessage(message);
        Assert.assertTrue(errorCode.equals(loggerLevelDto.getErrorcode()));
        Assert.assertTrue(message.equals(loggerLevelDto.getMessage()));
    }

    @Test
    public void testLoggerLevelRevisedDtoSet() {
        String errorCode = CommonDto.SUCCESS;
        String message = "error";
        String level = "INFO";
        String preLevel = "DEBUG";
        LoggerLevelRevisedDto loggerLevelRevisedDto = new LoggerLevelRevisedDto();
        loggerLevelRevisedDto.setErrorcode(errorCode);
        loggerLevelRevisedDto.setMessage(message);
        loggerLevelRevisedDto.setLevel(level);
        loggerLevelRevisedDto.setPrelevel(preLevel);
        Assert.assertTrue(errorCode.equals(loggerLevelRevisedDto.getErrorcode()));
        Assert.assertTrue(message.equals(loggerLevelRevisedDto.getMessage()));
        Assert.assertTrue(level.equals(loggerLevelRevisedDto.getLevel()));
        Assert.assertTrue(preLevel.equals(loggerLevelRevisedDto.getPrelevel()));
    }
}

