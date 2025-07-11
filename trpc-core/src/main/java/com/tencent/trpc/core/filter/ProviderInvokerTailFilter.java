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

package com.tencent.trpc.core.filter;

import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.RequestMeta;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcServerContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * [Before the real method call], used to copy request information to context for business use.
 * Note:
 * The context has little internal significance and all information can be obtained through the request internally,
 * so the assignment of serverContext is at the tail of the chain, before the real method call.
 * This way, the filter will not use the value of serverContext when used internally.
 * Chain: [head]->filter1->filter2->[tail]->business exposed remote method.
 */
public class ProviderInvokerTailFilter implements Filter {

    @Override
    public int getOrder() {
        return FilterOrdered.PROVIDER_TAIL_ORDERED;
    }

    @Override
    public CompletionStage<Response> filter(Invoker<?> invoker, Request request) {
        RpcServerContext serverContext = (RpcServerContext) (request.getContext());
        prepareServerContext(serverContext, request);
        CompletableFuture<Response> future = invoker.invoke(request).toCompletableFuture();
        // copy to the attachMap of the response after the real method call.
        return future.thenApply(r -> {
            if (r != null) {
                r.setAttachments(serverContext.getRspAttachMap());
            }
            return r;
        });
    }

    private void prepareServerContext(RpcServerContext serverContext, Request request) {
        RequestMeta meta = request.getMeta();
        serverContext.setOneWay(meta.isOneWay());
        serverContext.getReqAttachMap().putAll(request.getAttachments());
        serverContext.setDyeingKey(meta.getDyeingKey());
        serverContext.setCallInfo(meta.getCallInfo());
    }

}