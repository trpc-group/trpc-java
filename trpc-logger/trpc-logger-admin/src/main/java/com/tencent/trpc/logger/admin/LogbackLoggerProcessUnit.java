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

package com.tencent.trpc.logger.admin;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.tencent.trpc.core.logger.LoggerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.LoggerFactory;

public class LogbackLoggerProcessUnit extends AbstractLoggerProcessUnit {

    LogbackLoggerProcessUnit() {
    }

    @Override
    public void init() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        List<Logger> loggerList = loggerContext.getLoggerList();
        for (Logger logger : loggerList) {
            if (logger.getLevel() != null) {
                addLogger(logger.getName(), logger);
            }
        }
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        addLogger(rootLogger.getName(), rootLogger);
    }

    @Override
    public List<LoggerLevelInfo> getLoggerLevelInfo() {
        Map<String, Object> loggers = getLoggers();
        List<LoggerLevelInfo> loggerLevelInfoList = new ArrayList<>(loggers.size());
        for (Entry<String, Object> loggerEntry : loggers.entrySet()) {
            Logger logger = (Logger) loggerEntry.getValue();
            loggerLevelInfoList
                    .add(new LoggerLevelInfo(loggerEntry.getKey(), logger.getLevel().toString()));
        }
        return loggerLevelInfoList;
    }

    @Override
    public String setLoggerLevel(String loggerName, LoggerLevel level) {
        Logger logger = (Logger) getLogger(loggerName);
        if (logger == null) {
            return null;
        }
        Level currentLevel = logger.getLevel();
        Level newLevel = Level.toLevel(level.name(), currentLevel);
        logger.setLevel(newLevel);
        return currentLevel.levelStr;
    }

}
