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

package com.tencent.trpc.core.filter;

import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.*;
import com.tencent.trpc.core.utils.RpcContextUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Used for setting the context and request pre-information before the chain call.
 * Chain: [head]->filter1->filter2->[tail]->remote call.
 */
public class ConsumerInvokerHeadFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerInvokerHeadFilter.class);

    @Override
    public int getOrder() {
        return FilterOrdered.CONSUMER_HEAD_ORDERED;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public CompletionStage<Response> filter(Invoker<?> invoker, Request request) {
        RpcContext context = request.getContext();
        ConsumerInvoker consumerInvoker = (ConsumerInvoker) invoker;
        // combine with DefClusterInvocationHandler#genRequest to complete the information of the request.
        prepareRequestInfoBeforeInvoke(request, consumerInvoker);
        contextWithRemoteCalleeIp(context, request);
        startLog(context, request);
        CompletableFuture<Response> future = invoker.invoke(request).toCompletableFuture();
        if (logger.isDebugEnabled()) {
            future.whenComplete((rsp, t) -> endLog(context, request, rsp, t));
        }
        // after the RPC call is completed, copy the response to ClientContext for business use.
        return future.thenApply(r -> {
            if (r != null) {
                // rspAttachMap is the parameter passed through from the server to the client
                context.getRspAttachMap().putAll(r.getAttachments());
                // set the attachments sent from the server to the client
                context.setResponseUncodecDataSegment(r.getResponseUncodecDataSegment());
            }
            return r;
        });
    }

    /**
     * Set the request remote callee IP to RpcContext, with the key as CTX_CALLEE_REMOTE_IP.
     *
     * @param context RpcContext
     * @param request Request
     */
    private void contextWithRemoteCalleeIp(RpcContext context, Request request) {
        Optional.ofNullable(request.getMeta().getRemoteAddress()).ifPresent(remoteAddr
                -> RpcContextUtils.putValueMapValue(context, RpcContextValueKeys.CTX_CALLEE_REMOTE_IP,
                remoteAddr.getAddress().getHostAddress()));
    }

    private void prepareRequestInfoBeforeInvoke(Request request,
            ConsumerInvoker<?> consumerInvoker) {
        RequestMeta meta = request.getMeta();
        if (meta.getRemoteAddress() == null) {
            meta.setRemoteAddress(consumerInvoker.getProtocolConfig().toInetSocketAddress());
        }
    }

    private void startLog(RpcContext context, Request request) {
        if (logger.isDebugEnabled()) {
            logger.debug(">>>Consumer filter start, context({}), request({})", context, request);
        }
    }

    private void endLog(RpcContext context, Request request, Response response, Throwable ex) {
        if (logger.isDebugEnabled()) {
            logger.debug("<<<Consumer filter end, context({}), request({}), response({}), ex({}))",
                    context, request, response, ex);
        }
    }

}
