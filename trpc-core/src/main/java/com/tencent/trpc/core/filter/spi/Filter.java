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

package com.tencent.trpc.core.filter.spi;

import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.filter.Ordered;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import java.util.concurrent.CompletionStage;

/**
 * TRPC framework filter API definition, supports full-link asynchronous and priority-controlled filter interceptor
 * capabilities.
 */
@Extensible
public interface Filter extends Ordered {

    /**
     * <pre> Note: For ClientFilter, the framework has copied the contents of RpcContext into Request#attachments.
     * For ServerFilter,
     * the framework will copy the contents of Request#attachments into ServerContext after the filter ends.
     * 1) In the implementation of Filter,
     * it is necessary to trigger invoker.invoke(RpcContext context, Request request)
     * to ensure that the call is propagated to the real method call.
     * 2) It is recommended to use the following when calling whenComplete:
     * <code>
     *     CompletableFuture.whenComplete((r, t) -> {
     *         Throwable e = ExceptionHelper.parseResponseException(r, t);
     *           if(e != null && r != null) {
     *           }
     *      }
     * </code>
     * 3) When obtaining context information related to the method, use {@link Request#getInvocation()},
     * such as method name, method parameters, synchronous/asynchronous judgment, etc.
     * 4) When obtaining context information related to the request,
     * use {@link Request#getMeta()}, such as local address, remote address, timeout, whether it is oneway, etc.
     * 5) When obtaining context information of the request with attached parameters,
     * use {@link Request#getAttachments()} | {@link Request#getAttachments()} ()}
     * to obtain business pass-through parameters.
     * 6) When obtaining context information of the response with attached parameters,
     * use {@link Response#getAttachments()} | {@link Response#getAttachments()} ()}
     * to obtain business pass-through parameters.
     * 7) To get the context, use {@link Request#getContext()}}
     * 8) Note: In the oneway scenario, Response is null, so be sure to check for null. </pre>
     *
     * @param filterChain the filter chain
     * @param req the request
     * @return the response
     */
    CompletionStage<Response> filter(Invoker<?> filterChain, Request req);

}