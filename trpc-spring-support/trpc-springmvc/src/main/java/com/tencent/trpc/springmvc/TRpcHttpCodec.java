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

package com.tencent.trpc.springmvc;

import com.tencent.trpc.proto.http.common.HttpCodec;
import javax.servlet.http.HttpServletRequest;

public class TRpcHttpCodec extends HttpCodec {

    /**
     * Check if the request method is valid. The Spring MVC module needs to be restricted to the POST method.
     *
     * @param request HttpServletRequest
     * @param expectMethod the expected HTTP request method
     */
    @Override
    protected void checkRequestMethod(HttpServletRequest request, String expectMethod) {
        if (!expectMethod.equalsIgnoreCase(request.getMethod())) {
            throw new IllegalStateException("unsupported http method " + request.getMethod());
        }
    }

}
