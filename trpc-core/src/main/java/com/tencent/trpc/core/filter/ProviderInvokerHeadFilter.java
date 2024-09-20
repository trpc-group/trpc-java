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

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.GlobalConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.RequestMeta;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcContextValueKeys;
import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.core.rpc.TrpcTransInfoKeys;
import com.tencent.trpc.core.utils.PreconditionUtils;
import com.tencent.trpc.core.utils.RpcContextUtils;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.lang3.StringUtils;

/**
 * [Before the chain call] Used for setting the context and request pre-information.
 * Chain: [head]->filter1->filter2-[tail]->business exposed remote method.
 */
public class ProviderInvokerHeadFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ProviderInvokerHeadFilter.class);

    @Override
    public int getOrder() {
        return FilterOrdered.PROVIDER_HEAD_ORDERED;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public CompletionStage<Response> filter(Invoker<?> invoker, Request request) {
        RpcServerContext serverContext = (RpcServerContext) (request.getContext());
        prepareRequestInfoBeforeInvoke(request, (ProviderInvoker) invoker);
        contextWithRemoteCallerIp(serverContext, request);
        startLog(serverContext, request);
        CompletableFuture<Response> future = invoker.invoke(request).toCompletableFuture();
        if (logger.isDebugEnabled()) {
            future.whenComplete((rsp, t) -> endLog(serverContext, request, rsp, t));
        }
        return future;
    }

    /**
     * Set the request remote caller IP to RpcServerContext, with the key as CTX_CALLER_REMOTE_IP.
     *
     * @param serverContext RpcServerContext
     * @param request Request
     */
    private void contextWithRemoteCallerIp(RpcServerContext serverContext, Request request) {
        Optional.ofNullable(request.getMeta().getRemoteAddress()).ifPresent(remoteAddr
                -> RpcContextUtils.putValueMapValue(serverContext, RpcContextValueKeys.CTX_CALLER_REMOTE_IP,
                remoteAddr.getAddress().getHostAddress()));

    }

    private void startLog(RpcContext context, Request request) {
        logger.debug(">>>Provider filter start, rpcServiceName={}, rpcMethodName={}, context({}), request({})",
                request.getInvocation().getRpcServiceName(), request.getInvocation().getRpcMethodName(),
                context, request);
    }

    private void endLog(RpcContext context, Request request, Response response, Throwable ex) {
        logger.debug("<<<Provider filter end, rpcServiceName={}, rpcMethodName={}, context({}), "
                        + "request({}), response({}), exception({})",
                request.getInvocation().getRpcServiceName(),
                request.getInvocation().getRpcMethodName(), context, request, response, ex);
    }

    private void prepareRequestInfoBeforeInvoke(Request request,
            @SuppressWarnings("rawtypes") ProviderInvoker invoker) {
        PreconditionUtils.checkArgument(request != null, "request is null");
        int methodTimeout = invoker.getConfig().getRequestTimeout();
        RequestMeta meta = request.getMeta();
        int reqTimeout = meta.getTimeout();
        // add an additional timeout judgment layer,
        // combined with the timeout time of the request and the timeout time of the server.
        if (reqTimeout <= 0) {
            reqTimeout = methodTimeout;
        } else {
            reqTimeout = Math.min(reqTimeout, methodTimeout);
        }
        meta.setTimeout(reqTimeout);
        if (meta.getLocalAddress() == null) {
            meta.setLocalAddress(invoker.getProtocolConfig().toInetSocketAddress());
        }
        if (meta.getProviderConfig() == null) {
            meta.setProviderConfig(invoker.getConfig());
        }
        CallInfo callInfo = meta.getCallInfo();
        ServerConfig serverConfig = ConfigManager.getInstance().getServerConfig();
        // if it is not set, the framework sets a default information.
        if (serverConfig != null) {
            if (StringUtils.isBlank(callInfo.getCalleeApp())) {
                callInfo.setCalleeApp(serverConfig.getApp());
            }
            if (StringUtils.isBlank(callInfo.getCalleeServer())) {
                callInfo.setCalleeServer(serverConfig.getServer());
            }
        }
        GlobalConfig globalConfig = ConfigManager.getInstance().getGlobalConfig();
        // fill in the set information of the called callInfo container.
        Optional.ofNullable(globalConfig).ifPresent(gc -> {
            Optional.ofNullable(gc.getContainerName()).ifPresent(callInfo::setCalleeContainerName);
            Optional.ofNullable(gc.getFullSetName()).ifPresent(callInfo::setCalleeSetName);
        });
        // fill in the set information of the calling callInfo container.
        Optional.ofNullable(request.getAttachment(TrpcTransInfoKeys.CALLER_CONTAINER_NAME))
                .ifPresent(ccn -> callInfo.setCallerContainerName(new String((byte[]) ccn)));
        Optional.ofNullable(request.getAttachment(TrpcTransInfoKeys.CALLER_SET_NAME))
                .ifPresent(csn -> callInfo.setCallerSetName(new String((byte[]) csn)));
    }

}
