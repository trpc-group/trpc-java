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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class Log4j2LoggerProcessUnit extends AbstractLoggerProcessUnit {

    public static final String ROOT_LOGGER_NAME = "ROOT";

    Log4j2LoggerProcessUnit() {
    }

    public void init() {
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        Map<String, LoggerConfig> loggerConfigMap = loggerContext.getConfiguration().getLoggers();
        for (LoggerConfig loggerConfig : loggerConfigMap.values()) {
            String loggerName = loggerConfig.getName();
            if (StringUtils.isBlank(loggerName)) {
                loggerName = ROOT_LOGGER_NAME;
            }
            addLogger(loggerName, loggerConfig);
        }
        if (!getLoggers().containsKey(ROOT_LOGGER_NAME)) {
            Logger rootLogger = LogManager.getRootLogger();
            addLogger(ROOT_LOGGER_NAME, rootLogger);
        }
    }

    @Override
    public List<LoggerLevelInfo> getLoggerLevelInfo() {
        Map<String, Object> loggers = getLoggers();
        List<LoggerLevelInfo> loggerLevelInfoList = new ArrayList<>(loggers.size());
        for (Entry<String, Object> loggerEntry : loggers.entrySet()) {
            LoggerConfig loggerConfig = (LoggerConfig) loggerEntry.getValue();
            loggerLevelInfoList
                    .add(new LoggerLevelInfo(loggerEntry.getKey(), loggerConfig.getLevel().name()));
        }
        return loggerLevelInfoList;
    }

    @Override
    public String setLoggerLevel(String loggerName, LoggerLevel level) {
        LoggerConfig loggerConfig = (LoggerConfig) getLogger(loggerName);
        if (loggerConfig == null) {
            return null;
        }
        Level currentLevel = loggerConfig.getLevel();
        Level newLevel = Level.toLevel(level.name(), currentLevel);
        loggerConfig.setLevel(newLevel);
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        loggerContext.updateLoggers();
        return currentLevel.name();
    }

}
