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
import com.tencent.trpc.core.exception.TRpcException;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TrpcThreadExceptionHandlerTest {

    @BeforeEach
    public void before() {
        ConfigManager.stopTest();
        ConfigManager.startTest();
    }

    @AfterEach
    public void after() {
        ConfigManager.stopTest();
    }

    @Test
    public void testTrpcThreadExceptionHandler() {
        AtomicLong errorCount = new AtomicLong(1);
        AtomicLong businessError = new AtomicLong(1);
        AtomicLong protocolError = new AtomicLong(1);
        AdminConfig adminConfig = new AdminConfig();
        ConfigManager.getInstance().getServerConfig().setAdminConfig(adminConfig);
        TrpcThreadExceptionHandler trpcThreadExceptionHandler = new TrpcThreadExceptionHandler(errorCount,
                businessError, protocolError);
        trpcThreadExceptionHandler.getBusinessError();
        trpcThreadExceptionHandler.getProtocolError();
        trpcThreadExceptionHandler.getErrorCount();
        trpcThreadExceptionHandler.uncaughtException(Thread.currentThread(), new TRpcException());
    }

    @Test
    public void testTrpcThreadExceptionHandler1() {
        AtomicLong errorCount = new AtomicLong(1);
        AtomicLong businessError = new AtomicLong(1);
        AtomicLong protocolError = new AtomicLong(1);
        AdminConfig adminConfig = new AdminConfig();
        ConfigManager.getInstance().getServerConfig().setAdminConfig(adminConfig);
        TrpcThreadExceptionHandler trpcThreadExceptionHandler = new TrpcThreadExceptionHandler(errorCount,
                businessError, protocolError);
        trpcThreadExceptionHandler.getBusinessError();
        trpcThreadExceptionHandler.getProtocolError();
        trpcThreadExceptionHandler.getErrorCount();
        trpcThreadExceptionHandler.uncaughtException(Thread.currentThread(), new SQLException());
    }
}
