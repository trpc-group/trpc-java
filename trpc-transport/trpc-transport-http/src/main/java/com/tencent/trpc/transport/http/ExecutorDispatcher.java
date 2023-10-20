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

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ExecutorDispatcher extends HttpServlet {

    public static final long serialVersionUID = 6958185211125599194L;

    private static final ConcurrentMap<Integer, HttpExecutor> EXECUTORS =
            new ConcurrentHashMap<>();

    public ExecutorDispatcher() {
    }

    public static void addHttpExecutor(int port, HttpExecutor executor) {
        EXECUTORS.put(port, executor);
    }

    public static void removeHttpExecutor(int port) {
        EXECUTORS.remove(port);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpExecutor executor = EXECUTORS.get(request.getLocalPort());
        if (executor == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "HttpExecutor is closed: port is:" + request.getLocalPort() + ".");
        } else {
            executor.execute(request, response);
        }
    }

}
