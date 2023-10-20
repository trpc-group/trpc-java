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

package com.tencent.trpc.support.proxy;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.support.ConsulInstanceManager;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.Callable;


/**
 * Client proxy class handler test class
 */
public class ConsulExceptionProcessHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulExceptionProcessHandlerTest.class);

    @Test
    public void intercept() throws Exception {
        ConsulInstanceManager consulInstanceManager = Mockito.mock(ConsulInstanceManager.class);
        ConsulClientProxy consulClientProxy = new ConsulClientProxy(consulInstanceManager);
        ConsulExceptionProcessHandler handler = new ConsulExceptionProcessHandler(consulClientProxy);
        handler.intercept(Object::new);
    }

    @Test
    public void intercept01() throws Exception {

        try {
            ConsulClientProxy clientProxy = Mockito.mock(ConsulClientProxy.class);
            Mockito.when(clientProxy.getUsingAddresses(Mockito.anyInt())).thenReturn("127.0.0.1:9500");
            ConsulExceptionProcessHandler handler = new ConsulExceptionProcessHandler(clientProxy);
            handler.intercept(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    throw new RuntimeException("Connection refused");
                }
            });
        } catch (Exception e) {
            LOGGER.warn("intercept warn {}", e);
        }

    }

    @Test
    public void intercept02() throws Exception {
        ConsulInstanceManager consulInstanceManager = Mockito.mock(ConsulInstanceManager.class);
        ConsulClientProxy consulClientProxy = new ConsulClientProxy(consulInstanceManager);
        ConsulExceptionProcessHandler handler = new ConsulExceptionProcessHandler(consulClientProxy);
        try {
            handler.intercept(() -> {
                throw new RuntimeException("test");
            });
        } catch (Exception e) {
            LOGGER.warn("intercept warn {}", e);
        }
    }

}