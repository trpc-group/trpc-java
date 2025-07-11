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

package com.tencent.trpc.core.logger.slf4j;

import static org.slf4j.spi.LocationAwareLogger.DEBUG_INT;
import static org.slf4j.spi.LocationAwareLogger.ERROR_INT;
import static org.slf4j.spi.LocationAwareLogger.INFO_INT;
import static org.slf4j.spi.LocationAwareLogger.TRACE_INT;
import static org.slf4j.spi.LocationAwareLogger.WARN_INT;

import com.tencent.trpc.core.logger.Logger;
import org.slf4j.spi.LocationAwareLogger;

/**
 * Difference with {@link Slf4jLogger}, Slf4jLocationAwareLogger can log correct location information
 * such as line number, method name, etc.
 *
 * <p>Because logger use stack trace and fqcn(current logger name) to get correct location which is behind fqcn.</p>
 *
 * <p>Log location information is an expensive operation and may impact performance.</p>
 * If you use log4j2, you can see more information from it:
 * <a href="https://logging.apache.org/log4j/2.x/manual/layouts.html#LocationInformation">Log4j2 Location
 * Information</a>
 */
public class Slf4jLocationAwareLogger implements Logger {

    private static final String FQCN = Slf4jLocationAwareLogger.class.getName();
    private final LocationAwareLogger logger;

    public Slf4jLocationAwareLogger(LocationAwareLogger logger) {
        this.logger = logger;
    }

    @Override
    public void trace(String msg) {
        if (isTraceEnabled()) {
            logMessage(TRACE_INT, msg);
        }
    }

    @Override
    public void trace(String format, Object... argArray) {
        if (isTraceEnabled()) {
            logMessage(TRACE_INT, format, argArray);
        }
    }

    @Override
    public void trace(String msg, Throwable e) {
        if (isTraceEnabled()) {
            logMessage(TRACE_INT, msg, e);
        }
    }

    @Override
    public void debug(String msg) {
        if (isDebugEnabled()) {
            logMessage(DEBUG_INT, msg);
        }
    }

    @Override
    public void debug(String format, Object... argArray) {
        if (isDebugEnabled()) {
            logMessage(DEBUG_INT, format, argArray);
        }
    }

    @Override
    public void debug(String msg, Throwable e) {
        if (isDebugEnabled()) {
            logMessage(DEBUG_INT, msg, e);
        }
    }

    @Override
    public void info(String msg) {
        if (isInfoEnabled()) {
            logMessage(INFO_INT, msg);
        }
    }

    @Override
    public void info(String format, Object... argArray) {
        if (isInfoEnabled()) {
            logMessage(INFO_INT, format, argArray);
        }
    }

    @Override
    public void info(String msg, Throwable e) {
        if (isInfoEnabled()) {
            logMessage(INFO_INT, msg, e);
        }
    }

    @Override
    public void warn(String msg) {
        if (isWarnEnabled()) {
            logMessage(WARN_INT, msg);
        }
    }

    @Override
    public void warn(String format, Object... argArray) {
        if (isWarnEnabled()) {
            logMessage(WARN_INT, format, argArray);
        }
    }

    @Override
    public void warn(String msg, Throwable e) {
        if (isWarnEnabled()) {
            logMessage(WARN_INT, msg, e);
        }
    }

    @Override
    public void error(String msg) {
        if (isErrorEnabled()) {
            logMessage(ERROR_INT, msg);
        }
    }

    @Override
    public void error(String format, Object... argArray) {
        if (isErrorEnabled()) {
            logMessage(ERROR_INT, format, argArray);
        }
    }

    @Override
    public void error(String msg, Throwable e) {
        if (isErrorEnabled()) {
            logMessage(ERROR_INT, msg, e);
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    private void logMessage(final int level, String message) {
        logger.log(null, FQCN, level, message, null, null);
    }

    private void logMessage(final int level, String message, Object[] argArray) {
        logger.log(null, FQCN, level, message, argArray, null);
    }

    private void logMessage(final int level, String message, Throwable cause) {
        logger.log(null, FQCN, level, message, null, cause);
    }

}