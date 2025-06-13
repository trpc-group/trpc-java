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

package com.tencent.trpc.springmvc;

import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.common.RpcMethodInfoAndInvoker;
import com.tencent.trpc.proto.http.server.AbstractHttpExecutor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;

/**
 * HTTP processing logic of the Spring MVC module.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class TRpcHandlerAdapter extends AbstractHttpExecutor implements HandlerAdapter {

    public TRpcHandlerAdapter() {
        this.httpCodec = new TRpcHttpCodec();
    }

    @Override
    public boolean supports(Object handler) {
        return handler instanceof RpcMethodInfoAndInvoker;
    }

    @Override
    public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        super.execute(request, response, getRpcMethodInfoAndInvoker(handler));
        return null;
    }

    @Override
    protected RpcMethodInfoAndInvoker getRpcMethodInfoAndInvoker(Object object) {
        if (object instanceof RpcMethodInfoAndInvoker) {
            return (RpcMethodInfoAndInvoker) object;
        }
        throw TRpcException.newFrameException(ErrorCode.TRPC_SERVER_NOFUNC_ERR, "not found rpc invoker %s", object);
    }

    @Override
    public long getLastModified(HttpServletRequest request, Object handler) {
        return -1;  // not support
    }

}
