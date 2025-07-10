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

import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
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

/**
 * The construction order of the consumer filter chain is as follows:
 * Cn(C(last), Fn) -> Cn-1(Cn, Fn-1) -> ... -> C1(C2, F1)
 *
 * The execution order is as follows
 * C1(C2, F1) -> C2(C3, F2) -> ... -> Cn(C(last), Fn) -> C(last)
 */
public class ConsumerFilterInvoker<T> implements ConsumerInvoker<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumerFilterInvoker.class);
    private ConsumerConfig<T> consumerConfig;
    private ConsumerInvoker<T> invoker;

    public ConsumerFilterInvoker(ConsumerConfig<T> consumerConfig, ConsumerInvoker<T> target) {
        this.consumerConfig = consumerConfig;
        this.invoker = buildConsumerChain(consumerConfig.getBackendConfig().getFilters(), target);
    }

    /**
     * Build Consumer filter Chain
     *
     * @param filterNames {@link List}
     * @param last ConsumerInvoker
     * @param <T> T
     * @return ConsumerInvoker <code>ConsumerInvoker</code>
     */
    public static <T> ConsumerInvoker<T> buildConsumerChain(List<String> filterNames, ConsumerInvoker<T> last) {
        filterNames = filterNames == null ? Collections.emptyList() : filterNames;
        List<Filter> filters = Stream.concat(filterNames.stream().map(FilterManager::get),
                        Stream.of(new ConsumerInvokerHeadFilter(), new ConsumerInvokerTailFilter()))
                .sorted(Comparator.comparing(Filter::getOrder)).collect(Collectors.toList());
        ConsumerInvoker<T> lastConsumer = last;
        for (int i = filters.size() - 1; i >= 0; i--) {
            final Filter filter = filters.get(i);
            final ConsumerInvoker<T> before = lastConsumer;
            lastConsumer = new ConsumerInvoker<T>() {
                @Override
                public ConsumerConfig<T> getConfig() {
                    return last.getConfig();
                }

                @Override
                public Class<T> getInterface() {
                    return last.getInterface();
                }

                @Override
                public CompletionStage<Response> invoke(Request request) {
                    RpcInvocation inv = request.getInvocation();
                    LOG.debug(">>>Before Invoke consumer filter(service={},rpcServiceName={},rpcMethodName={})",
                            filter.getClass(), inv.getRpcServiceName(), inv.getRpcMethodName());
                    CompletionStage<Response> f = filter.filter(before, request);
                    LOG.debug("<<<After Invoke consumer filter(service={}, rpcServiceName={},rpcMethodName={})",
                            filter.getClass(), inv.getRpcServiceName(), inv.getRpcMethodName());
                    PreconditionUtils.checkArgument(f != null,
                            "Invoke consumer filter(service=%s, rpcServiceName=%s,rpcMethodName=%s) return null",
                            filter.getClass(), inv.getRpcServiceName(), inv.getRpcMethodName());
                    return f;
                }

                @Override
                public ProtocolConfig getProtocolConfig() {
                    return last.getProtocolConfig();
                }
            };
        }
        return lastConsumer;
    }

    @Override
    public Class<T> getInterface() {
        return consumerConfig.getServiceInterface();
    }

    @Override
    public CompletionStage<Response> invoke(Request request) {
        return invoker.invoke(request).toCompletableFuture();
    }

    @Override
    public ProtocolConfig getProtocolConfig() {
        return invoker.getProtocolConfig();
    }

    @Override
    public ConsumerConfig<T> getConfig() {
        return consumerConfig;
    }

}