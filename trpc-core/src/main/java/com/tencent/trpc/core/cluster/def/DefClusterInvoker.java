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

package com.tencent.trpc.core.cluster.def;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.cluster.AbstractClusterInvoker;
import com.tencent.trpc.core.cluster.RpcClusterClientManager;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.TRpcProtocolType;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.ErrorCodeUtils;
import com.tencent.trpc.core.exception.ExceptionHelper;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.filter.FilterChain;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClient;
import com.tencent.trpc.core.selector.SelectorManager;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.spi.Selector;
import com.tencent.trpc.core.utils.FutureUtils;
import com.tencent.trpc.core.utils.RpcUtils;
import com.tencent.trpc.core.utils.TimerUtil;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class DefClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(DefClusterInvoker.class);

    private final ConcurrentMap<String, ConsumerInvokerProxy<T>> invokerCache = Maps.newConcurrentMap();

    private final Object lock = new Object();

    public DefClusterInvoker(ConsumerConfig<T> config) {
        super(config);
    }

    @Override
    protected CompletionStage<Response> doInvoke(Request request, CompletionStage<ServiceInstance> instance) {
        Function<? super ServiceInstance, ? extends CompletionStage<Response>> invokerFunc =
                (ins) -> Optional.ofNullable(ins)
                        .map(i -> getInvoker(i).invoke(request, ins))
                        .orElseGet(() ->
                                FutureUtils.failed(TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_ROUTER_ERR,
                                        "Service(name=" + consumerConfig.getServiceInterface().getName()
                                                + ", naming=" + backendConfig.getNamingOptions().getServiceNaming()
                                                + "), Client router error [found no available instance]")));
        if (instance.toCompletableFuture().isDone()) {
            return invokerFunc.apply(instance.toCompletableFuture().join());
        }
        return instance.thenComposeAsync(invokerFunc, backendConfig.getWorkerPoolObj().toExecutor());
    }

    protected ConsumerInvokerProxy<T> getInvoker(ServiceInstance instance) {
        String key = toUniqKey(instance);
        ConsumerInvokerProxy<T> result = invokerCache.get(key);
        return Optional.ofNullable(result).orElseGet(() -> createInvoker(instance));
    }

    @SuppressWarnings("rawtypes")
    public ConsumerInvokerProxy<T> createInvoker(ServiceInstance instance) {
        String key = toUniqKey(instance);
        ConsumerInvokerProxy value = invokerCache.get(key);
        // add value.isAvailable to prevent other threads
        // from accessing the Invoker during the cleaning process of Invoker#close.
        // At this time, the Invoker needs to be rebuilt.
        if (value == null || !value.isAvailable()) {
            synchronized (lock) {
                value = invokerCache.get(key);
                if (value == null || !value.isAvailable()) {
                    if (value != null && !value.isAvailable()) {
                        invokerCache.remove(key);
                    }
                    RpcClient rpcClient;
                    try {
                        TRpcProtocolType protocolType = RpcUtils.checkAndGetProtocolType(
                                consumerConfig.getServiceInterface());
                        rpcClient = RpcClusterClientManager.getOrCreateClient(backendConfig,
                                backendConfig.generateProtocolConfig(instance.getHost(), instance.getPort(),
                                        backendConfig.getNetwork(), protocolType.getName(), backendConfig.getExtMap()));
                        ConsumerInvoker<T> originInvoker = rpcClient.createInvoker(consumerConfig);
                        value = new ConsumerInvokerProxy<>(FilterChain.buildConsumerChain(consumerConfig,
                                originInvoker), rpcClient);
                        invokerCache.put(key, value);
                        // When the rpcClient is cleaned up and closed during idle time,
                        // the corresponding map should also be cleaned up.
                        rpcClient.closeFuture().whenComplete((r, e) -> {
                            ConsumerInvokerProxy<T> remove = invokerCache.remove(key);
                            if (remove != null) {
                                logger.warn("Service [name=" + consumerConfig.getServiceInterface()
                                        .getName()
                                        + ", naming=" + backendConfig.getNamingOptions()
                                        .getServiceNaming()
                                        + ")], remove rpc client invoker["
                                        + remove.getInvoker().getProtocolConfig().toSimpleString()
                                        + "], due to rpc client close");
                            }
                        });
                        return value;
                    } catch (Exception ex) {
                        throw TRpcException.newFrameException(ErrorCode.TRPC_INVOKE_UNKNOWN_ERR,
                                "Service(name=" + consumerConfig.getServiceInterface().getName()
                                        + ", naming="
                                        + backendConfig.getNamingOptions().getServiceNaming()
                                        + "), Create rpc client(ip:" + instance.getHost() + ",port:"
                                        + instance.getPort() + ",network:" + backendConfig
                                        .getNetwork() + ") exception("
                                        + ex.getMessage() + ")", ex);
                    }
                }
            }
        }
        return value;
    }

    /**
     * The toString method override {@link Object#toString()}
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "NamingClusterInvoker [" + consumerConfig.getServiceInterface() + "]";
    }

    private String toUniqKey(ServiceInstance instance) {
        String networkType = backendConfig.getNetwork();
        return ProtocolConfig.toUniqId(instance.getHost(), instance.getPort(), networkType);
    }

    public static class ConsumerInvokerProxy<T> {

        private ConsumerInvoker<T> invoker;

        private RpcClient client;

        ConsumerInvokerProxy(ConsumerInvoker<T> invoker, RpcClient client) {
            this.invoker = invoker;
            this.client = client;
        }

        public boolean isAvailable() {
            return client != null && client.isAvailable();
        }

        /**
         * Core wrapper function that reports the client's recent usage time and result statistics after invocation.
         *
         * @param request {@link Request}
         * @param serviceInstance {@link ServiceInstance}
         * @return {@code CompletionStage<Response>}
         */
        public CompletionStage<Response> invoke(Request request, ServiceInstance serviceInstance) {
            TimerUtil timer = TimerUtil.newInstance();
            timer.start();
            fillCallInfo(request, serviceInstance);
            return invoker.invoke(request).whenComplete((r, t) -> {
                timer.end();
                Throwable e = ExceptionHelper.parseResponseException(r, t);
                if (e != null) {
                    if (ExceptionHelper.isTRpcException(e)) {
                        int code = ((TRpcException) e).getCode();
                        if (ErrorCodeUtils.needCircuitBreaker(code)) {
                            report(serviceInstance, code, timer.getCost());
                            return;
                        }
                    }
                    if (logger.isDebugEnabled()) {
                        ConsumerConfig<T> config = invoker.getConfig();
                        logger.debug("Service [name=" + config.getServiceInterface().getName()
                                + ", naming="
                                + config.getBackendConfig().getNamingOptions().getServiceNaming()
                                + ")], circuitBreaker success, exception(" + e.getMessage()
                                + ") need not CircuitBreaker)");
                    }
                    report(serviceInstance, ErrorCode.TRPC_INVOKE_SUCCESS, timer.getCost());
                } else {
                    report(serviceInstance, ErrorCode.TRPC_INVOKE_SUCCESS, timer.getCost());
                }
            });
        }

        /**
         * Fill in some fields of the callInfo based on the Instance instance returned by the naming service.
         *
         * @param request {@link Request}
         * @param serviceInstance {@link ServiceInstance}
         */
        private void fillCallInfo(Request request, ServiceInstance serviceInstance) {
            if (request.getMeta().getCallInfo() != null) {
                request.getMeta().getCallInfo().setCalleeContainerName(serviceInstance.getParameter(
                        Constants.CONTAINER_NAME));
                request.getMeta().getCallInfo().setCalleeSetName(serviceInstance.getParameter(
                        Constants.SET_DIVISION));
                logger.debug("[invoke] container:{},set:{}", serviceInstance.getParameter(
                        Constants.CONTAINER_NAME), serviceInstance.getParameter(Constants.SET_DIVISION));
            }
        }

        private void report(ServiceInstance serviceInstance, int code, long costMs) {
            BackendConfig backendConfig = invoker.getConfig().getBackendConfig();
            String selectorId = backendConfig.getNamingOptions().getSelectorId();
            try {
                Selector selector = SelectorManager.getManager().get(selectorId);
                Optional.ofNullable(selector).ifPresent(s -> s.report(serviceInstance, code, costMs));
                if (logger.isDebugEnabled()) {
                    ConsumerConfig<T> config = invoker.getConfig();
                    logger.debug("Service [name=" + config.getServiceInterface().getName() + ", naming="
                            + backendConfig.getNamingOptions().getServiceNaming()
                            + ")], circuitBreaker report[code=" + code + ", costMs="
                            + costMs + ", instance="
                            + serviceInstance.toString() + "])");
                }
            } catch (Exception ex) {
                logger.error("Selector(name=" + selectorId + ",naming="
                        + backendConfig.getNamingOptions().getServiceNaming()
                        + ") report error, info(circuitBreaker report[code=" + code + ", costMs="
                        + costMs
                        + ", instance=" + serviceInstance.toString() + "), ignore", ex);
            }
        }

        public ConsumerInvoker<T> getInvoker() {
            return invoker;
        }
    }

}
