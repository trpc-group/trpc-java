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

package com.tencent.trpc.core.logger;

import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.logger.slf4j.Slf4jLoggerAdapter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.TrpcMDCAdapter;

@SuppressWarnings("unchecked")
public class LoggerFactory {

    private static final ConcurrentMap<String, Logger> LOCAL_LOGGERS = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, RemoteLogger> REMOTE_LOGGERS =
            new ConcurrentHashMap<>();

    private static LoggerAdapter loggerAdapter;

    static {
        loggerAdapter = new Slf4jLoggerAdapter();
        Logger logger = getLogger(LoggerFactory.class);
        if (logger.isDebugEnabled() || logger.isTraceEnabled()) {
            TrpcMDCAdapter.init();
        }
    }

    public static Logger getLogger(Class<?> key) {
        return LOCAL_LOGGERS.computeIfAbsent(key.getName(), name -> loggerAdapter.getLogger(name));
    }

    public static Logger getLogger(String name) {
        return LOCAL_LOGGERS.computeIfAbsent(name, keyname -> loggerAdapter.getLogger(keyname));
    }

    public static RemoteLogger getRemoteLogger(String name) {
        ExtensionLoader<?> extensionLoader = ExtensionLoader.getExtensionLoader(RemoteLoggerAdapter.class);
        RemoteLoggerAdapter remoteLoggerAdapter = (RemoteLoggerAdapter) extensionLoader.getExtension(name);
        return REMOTE_LOGGERS.computeIfAbsent(name, keyname -> remoteLoggerAdapter.getRemoteLogger());
    }

    public static LoggerLevel getLoggerLevel() {
        return loggerAdapter.getLoggerLevel();
    }

    public static void setLoggerLevel(LoggerLevel loggerLevel) {
        loggerAdapter.setLoggerLevel(loggerLevel);
    }
}