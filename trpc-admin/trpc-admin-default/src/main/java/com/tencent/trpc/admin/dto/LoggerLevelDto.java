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
import java.util.List;

/**
 * Log level view class
 */
public class LoggerLevelDto extends CommonDto {

    /**
     * Log level information list
     */
    private List<LoggerLevelInfo> logger;

    public LoggerLevelDto() {
    }

    public LoggerLevelDto(List<LoggerLevelInfo> logger) {
        this.logger = logger;
    }

    public LoggerLevelDto(String errorcode, String message,
            List<LoggerLevelInfo> logger) {
        super(errorcode, message);
        this.logger = logger;
    }

    public static LoggerLevelDto buildFail(String message) {
        return new LoggerLevelDto(CommonDto.FAIL, message, null);
    }

    public List<LoggerLevelInfo> getLogger() {
        return logger;
    }

    public void setLogger(List<LoggerLevelInfo> logger) {
        this.logger = logger;
    }

    @Override
    public String toString() {
        return "LoggerLevelDto{" + "logger=" + logger + "} " + super.toString();
    }
}
