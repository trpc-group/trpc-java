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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * When writing an HTTP service, each HTTP server corresponds to an HttpExecutor. Implement this class to use it.
 */
public interface HttpExecutor {

    /**
     * Execute a http request and write response through the response param.
     *
     * @param request the {@link HttpServletRequest}
     * @param response the {@link HttpServletResponse}
     * @throws IOException in case of I/O error
     * @throws ServletException in case of Servlet handler error
     */
    void execute(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException;

    /**
     * Destroy this class
     */
    default void destroy() {
    }

}
