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
import java.util.List;
import java.util.Map;

public class UnSupportLoggerProcessUnit implements LoggerProcessUnit {

    @Override
    public void init() {
    }

    @Override
    public Map<String, Object> getLoggers() {
        throw new UnsupportedOperationException(
                "Current log frame doesn't support this operation!");
    }

    @Override
    public List<LoggerLevelInfo> getLoggerLevelInfo() {
        throw new UnsupportedOperationException(
                "Current log frame doesn't support this operation!");
    }

    @Override
    public String setLoggerLevel(String loggerName, LoggerLevel level) {
        throw new UnsupportedOperationException(
                "Current log frame doesn't support this operation!");
    }

}
