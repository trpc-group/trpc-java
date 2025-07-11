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

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractLoggerProcessUnit implements LoggerProcessUnit {

    private Map<String, Object> loggerMap = new HashMap<>();

    @Override
    public Map<String, Object> getLoggers() {
        return loggerMap;
    }

    public void addLogger(String loggerName, Object logger) {
        loggerMap.put(loggerName, logger);
    }

    public Object getLogger(String loggerName) {
        return loggerMap.get(loggerName);
    }

}
