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

public enum LoggerFactoryEnum {

    LOGBACK_FACTORY("ch.qos.logback.classic.LoggerContext"),
    LOG4J2_FACTORY("org.apache.logging.slf4j.Log4jLoggerFactory"),
    LOG4J_FACTORY("org.slf4j.impl.Log4jLoggerFactory"),
    UN_SUPPORT_FACTORY("un_support_factory");

    private String loggerFactoryClassName;

    LoggerFactoryEnum(String loggerFactoryClassName) {
        this.loggerFactoryClassName = loggerFactoryClassName;
    }

    public static LoggerFactoryEnum getLoggerFactoryEnum(String loggerFactoryClassName) {
        for (LoggerFactoryEnum loggerFactoryEnum : LoggerFactoryEnum.values()) {
            if (loggerFactoryEnum.getLoggerFactoryClassName().equals(loggerFactoryClassName)) {
                return loggerFactoryEnum;
            }
        }
        return LoggerFactoryEnum.UN_SUPPORT_FACTORY;
    }

    public String getLoggerFactoryClassName() {
        return loggerFactoryClassName;
    }

}
