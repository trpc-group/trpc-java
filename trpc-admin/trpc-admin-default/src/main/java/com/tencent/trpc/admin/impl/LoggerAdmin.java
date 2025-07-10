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

package com.tencent.trpc.admin.impl;

import com.tencent.trpc.admin.dto.LoggerLevelDto;
import com.tencent.trpc.admin.dto.LoggerLevelRevisedDto;
import com.tencent.trpc.core.admin.spi.Admin;
import com.tencent.trpc.core.logger.LoggerLevel;
import com.tencent.trpc.logger.admin.LoggerLevelInfo;
import com.tencent.trpc.logger.admin.LoggerManager;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.EnumUtils;

/**
 * Log Management
 */
@Path("/cmds")
public class LoggerAdmin implements Admin {

    public static final String INVALID_LOGGER_MSG = "Invalid logger!";
    public static final String INVALID_LOGGER_LEVEL_MSG = "Invalid logger level!";
    private LoggerManager loggerManager;

    public LoggerAdmin() {
        loggerManager = new LoggerManager();
    }

    @Path("/loglevel/{logname}")
    @PUT
    @Produces({MediaType.APPLICATION_JSON})
    public LoggerLevelRevisedDto setLoggerLevel(@PathParam("logname") String logName,
            @FormParam("value") String level) {
        String preLevel = null;
        try {
            if (!isEffectiveLogger(logName)) {
                return LoggerLevelRevisedDto.buildFail(INVALID_LOGGER_MSG);
            }
            if (!isEffectiveLoggerLevel(level)) {
                return LoggerLevelRevisedDto.buildFail(INVALID_LOGGER_LEVEL_MSG);
            }
            LoggerLevel loggerLevel = EnumUtils.getEnum(LoggerLevel.class, level.toUpperCase());
            preLevel = loggerManager.setLoggerLevel(logName, loggerLevel);
        } catch (UnsupportedOperationException e) {
            return LoggerLevelRevisedDto.buildFail(e.getMessage());
        }
        return new LoggerLevelRevisedDto(level, preLevel);
    }

    @Path("/loglevel")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public LoggerLevelDto getLoggerLevelInfo() {
        List<LoggerLevelInfo> loggerLevelInfoList = null;
        try {
            loggerLevelInfoList = loggerManager.getLoggerLevelInfo();
        } catch (UnsupportedOperationException e) {
            return LoggerLevelDto.buildFail(e.getMessage());
        }
        return new LoggerLevelDto(loggerLevelInfoList);
    }

    private boolean isEffectiveLoggerLevel(String loggerLevel) {
        return Objects.nonNull(EnumUtils.getEnum(LoggerLevel.class, loggerLevel.toUpperCase()));
    }

    private boolean isEffectiveLogger(String loggerName) {
        Map<String, Object> loggers = loggerManager.getLoggers();
        return loggers.containsKey(loggerName);
    }
}
