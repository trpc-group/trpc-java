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

import com.tencent.trpc.core.logger.LoggerLevel;
import java.util.List;
import java.util.Map;

public class LoggerManager {

    private LoggerProcessUnit loggerProcessUnit;

    public LoggerManager() {
        loggerProcessUnit = LoggerProcessUnitFactory.getLoggerProcessUnit();
    }

    public List<LoggerLevelInfo> getLoggerLevelInfo() {
        return loggerProcessUnit.getLoggerLevelInfo();
    }

    public String setLoggerLevel(String loggerName, LoggerLevel level) {
        return loggerProcessUnit.setLoggerLevel(loggerName, level);
    }

    public Map<String, Object> getLoggers() {
        return loggerProcessUnit.getLoggers();
    }

}
