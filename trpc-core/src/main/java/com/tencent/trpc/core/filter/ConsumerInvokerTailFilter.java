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

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.TrpcTransInfoKeys;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * [Before the real method call]
 * Chain: [head]->filter1->filter2-[tail]->remote call
 * The calling container name and set name are passed through before the remote call.
 */
public class ConsumerInvokerTailFilter implements Filter {

    @Override
    public int getOrder() {
        return FilterOrdered.CONSUMER_TAIL_ORDERED;
    }

    @Override
    public CompletionStage<Response> filter(Invoker<?> invoker, Request request) {
        transInfoWithContainerName(request);
        transInfoWithSetName(request);
        return invoker.invoke(request);
    }

    /**
     * Pass the caller container name through the chain.
     *
     * @param request Request
     */
    private void transInfoWithContainerName(Request request) {
        Optional.ofNullable(ConfigManager.getInstance().getGlobalConfig().getContainerName()).ifPresent(cn ->
                request.getAttachments().putIfAbsent(TrpcTransInfoKeys.CALLER_CONTAINER_NAME, cn));
    }

    /**
     * Pass the caller set name through the chain, only pass it when the set routing switch is turned on.
     *
     * @param request Request
     */
    private void transInfoWithSetName(Request request) {
        if (ConfigManager.getInstance().getGlobalConfig().isEnableSet()) {
            Optional.ofNullable(ConfigManager.getInstance().getGlobalConfig().getFullSetName()).ifPresent(sn ->
                    request.getAttachments().putIfAbsent(TrpcTransInfoKeys.CALLER_SET_NAME, sn));
        }
    }
    
}
