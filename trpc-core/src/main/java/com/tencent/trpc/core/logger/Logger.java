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

package com.tencent.trpc.core.logger;

@SuppressWarnings("unchecked")
public interface Logger {

    void trace(String msg);

    void trace(String format, Object... argArray);

    void trace(String msg, Throwable e);

    void debug(String msg);

    void debug(String format, Object... argArray);

    void debug(String msg, Throwable e);

    void info(String msg);

    void info(String format, Object... argArray);

    void info(String msg, Throwable e);

    void warn(String msg);

    void warn(String format, Object... argArray);

    void warn(String msg, Throwable e);

    void error(String msg);

    void error(String format, Object... argArray);

    void error(String msg, Throwable e);

    boolean isTraceEnabled();

    boolean isDebugEnabled();

    boolean isInfoEnabled();

    boolean isWarnEnabled();

    boolean isErrorEnabled();

    String getName();

}