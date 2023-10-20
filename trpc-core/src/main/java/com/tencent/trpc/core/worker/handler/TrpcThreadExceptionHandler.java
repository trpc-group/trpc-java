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

package com.tencent.trpc.core.worker.handler;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.AdminConfig;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TRPC thread pool exception handler, handles all exceptions thrown by TRPC threads.
 */
public class TrpcThreadExceptionHandler implements UncaughtExceptionHandler {

    private static final String PACKAGE_NAME = "com.tencent.trpc.core.exception";

    private AtomicLong errorCount;
    private AtomicLong businessError;
    private AtomicLong protocolError;

    public TrpcThreadExceptionHandler(AtomicLong errorCount, AtomicLong businessError,
            AtomicLong protocolError) {
        this.errorCount = errorCount;
        this.businessError = businessError;
        this.protocolError = protocolError;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        AdminConfig adminConfig = ConfigManager.getInstance().getServerConfig().getAdminConfig();
        if (adminConfig != null) {
            this.errorCount.incrementAndGet();
            if (e.getClass().getPackage().getName().startsWith(PACKAGE_NAME)) {
                this.protocolError.incrementAndGet();
            } else {
                this.businessError.incrementAndGet();
            }
        }
    }

    public Long getErrorCount() {
        return errorCount.get();
    }

    public Long getBusinessError() {
        return businessError.get();
    }

    public Long getProtocolError() {
        return protocolError.get();
    }

}
