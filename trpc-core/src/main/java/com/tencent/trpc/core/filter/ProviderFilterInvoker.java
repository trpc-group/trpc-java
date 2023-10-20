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

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.utils.PreconditionUtils;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public class ProviderFilterInvoker<T> implements ProviderInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(ProviderFilterInvoker.class);
    private ProviderConfig<T> providerConfig;
    private ProviderInvoker<T> invoker;

    public ProviderFilterInvoker(ProviderConfig<T> providerConfig, ProviderInvoker<T> target) {
        this.providerConfig = providerConfig;
        this.invoker = buildProviderChain(providerConfig.getDisableDefaultFilter(), providerConfig.getFilters(),
                target);
    }

    private static <T> ProviderInvoker<T> buildProviderChain(boolean disableDefaultFilter, List<String> filterNames,
            ProviderInvoker<T> last) {
        filterNames = filterNames == null ? Collections.emptyList() : filterNames;
        ProviderInvoker<T> lastProvider = last;
        Stream<Filter> defaultFilters = disableDefaultFilter ? Stream.empty()
                : Stream.of(new ProviderInvokerHeadFilter(), new ProviderInvokerTailFilter());
        List<Filter> filters = Stream.concat(filterNames.stream().map(FilterManager::get), defaultFilters)
                .sorted(Comparator.comparing(Filter::getOrder)).collect(Collectors.toList());
        logger.debug("===Build invoke provider filter size: {}", filters.size());

        for (int i = filters.size() - 1; i >= 0; i--) {
            final Filter filter = filters.get(i);
            final ProviderInvoker<T> before = lastProvider;
            lastProvider = new ProviderInvoker<T>() {
                @Override
                public ProviderConfig<T> getConfig() {
                    return last.getConfig();
                }

                @Override
                public Class<T> getInterface() {
                    return last.getInterface();
                }

                @Override
                public CompletionStage<Response> invoke(Request request) {
                    RpcInvocation inv = request.getInvocation();
                    logger.debug(">>>Before Invoke provider filter(service={}, rpcServiceName={}, rpcMehthodName={})",
                            filter.getClass(), inv.getRpcServiceName(), inv.getRpcMethodName());

                    CompletionStage<Response> f = filter.filter(before, request);
                    logger.debug("<<<After Invoke provider filter(service={}, rpcServiceName={},rpcMehthodName={})",
                            filter.getClass(), inv.getRpcServiceName(), inv.getRpcMethodName());

                    PreconditionUtils.checkArgument(f != null,
                            "Invoke provider filter(service=%s, rpcServiceName=%s,rpcMehthodName=%s) return Null",
                            filter.getClass(), inv.getRpcServiceName(), inv.getRpcMethodName());
                    return f;
                }

                @Override
                public T getImpl() {
                    return last.getImpl();
                }

                @Override
                public ProtocolConfig getProtocolConfig() {
                    return last.getProtocolConfig();
                }
            };
        }
        return lastProvider;
    }

    @Override
    public Class<T> getInterface() {
        return providerConfig.getServiceInterface();
    }

    @Override
    public CompletionStage<Response> invoke(Request request) {
        return invoker.invoke(request);
    }

    @Override
    public ProtocolConfig getProtocolConfig() {
        return invoker.getProtocolConfig();
    }

    @Override
    public T getImpl() {
        return providerConfig.getRef();
    }

    @Override
    public ProviderConfig<T> getConfig() {
        return providerConfig;
    }

}