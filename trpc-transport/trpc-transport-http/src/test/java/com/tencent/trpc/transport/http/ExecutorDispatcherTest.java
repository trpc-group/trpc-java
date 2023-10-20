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

package com.tencent.trpc.transport.http;

import static org.mockito.Mockito.times;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

public class ExecutorDispatcherTest {

    @Test
    public void test() throws ServletException, IOException {
        ExecutorDispatcher dispatcher = new ExecutorDispatcher();
        HttpServletRequest req = PowerMockito.mock(HttpServletRequest.class);
        PowerMockito.when(req.getLocalPort()).thenReturn(1024);
        HttpServletResponse rsp = PowerMockito.mock(HttpServletResponse.class);
        dispatcher.service(req, rsp);
        org.mockito.Mockito.verify(rsp, times(1)).sendError(404,
                "HttpExecutor is closed: port is:1024.");
    }
}
