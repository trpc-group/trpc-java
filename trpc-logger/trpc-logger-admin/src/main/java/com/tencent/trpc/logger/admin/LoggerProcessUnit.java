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

public interface LoggerProcessUnit {

    void init();

    Map<String, Object> getLoggers();

    List<LoggerLevelInfo> getLoggerLevelInfo();

    String setLoggerLevel(String loggerName, LoggerLevel level);

}
