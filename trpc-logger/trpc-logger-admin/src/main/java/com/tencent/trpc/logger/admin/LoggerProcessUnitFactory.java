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

import org.slf4j.LoggerFactory;

public class LoggerProcessUnitFactory {

    private static LoggerProcessUnit loggerProcessUnit;

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public static synchronized LoggerProcessUnit getLoggerProcessUnit() {
        if (loggerProcessUnit != null) {
            return loggerProcessUnit;
        }
        String loggerFactoryClassName = LoggerFactory.getILoggerFactory().getClass().getName();
        LoggerFactoryEnum loggerFactoryEnum = LoggerFactoryEnum
                .getLoggerFactoryEnum(loggerFactoryClassName);
        switch (loggerFactoryEnum) {
            case LOG4J2_FACTORY:
                loggerProcessUnit = new Log4j2LoggerProcessUnit();
                break;
            case LOGBACK_FACTORY:
                loggerProcessUnit = new LogbackLoggerProcessUnit();
                break;
            case UN_SUPPORT_FACTORY:
                loggerProcessUnit = new UnSupportLoggerProcessUnit();
                break;
            default:
        }
        loggerProcessUnit.init();
        return loggerProcessUnit;
    }

}
