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

import com.tencent.trpc.core.cluster.AbstractClusterInvocationHandler;
import com.tencent.trpc.core.cluster.ClusterInvoker;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.InvokeMode;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.RequestMeta;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcContextValueKeys;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.LeftTimeout;
import com.tencent.trpc.core.rpc.def.LinkInvokeTimeout;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.core.utils.RpcUtils;
import com.tencent.trpc.core.utils.SeqUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Dynamic proxy class for client clustering that supports standard service invocation.
 * It is mainly used to check parameters, encapsulate RPC requests, and parse results.
 */
public class DefClusterInvocationHandler extends AbstractClusterInvocationHandler {

    public DefClusterInvocationHandler(ClusterInvoker<?> invoker) {
        super(invoker);
    }

    @Override
    protected LeftTimeout validateTimeout(RpcInvocation invocation, Object[] args) {
        RpcClientContext context = (RpcClientContext) args[0];
        LeftTimeout leftTimeout = parseLeftTime(context, invocation.getRpcMethodName());
        int leftTime = leftTimeout.getLeftTimeout();
        int originTimeout = leftTimeout.getOriginTimeout();
        // if there is not enough remaining time and full-link timeout is enabled, return a full-link timeout exception.
        if (originTimeout > 0 && leftTime <= 0 && linkInvokeTimeoutEnable(context)) {
            throw TRpcException.newFrameException(ErrorCode.TRPC_LINK_INVOKE_TIMEOUT_ERR,
                    "link invoke request timeout > " + originTimeout + " ms");
        }
        return leftTimeout;
    }

    /**
     * Is full-link timeout enabled.
     *
     * @param context RpcContext  {@link RpcContext}
     * @return true Enable，false Disable
     */
    protected boolean linkInvokeTimeoutEnable(RpcContext context) {
        LinkInvokeTimeout linkTimeout = RpcContextUtils.getValueMapValue(context,
                RpcContextValueKeys.CTX_LINK_INVOKE_TIMEOUT);
        // If linkTimeout is null, it means that it is the first caller and cannot be enabled.
        return linkTimeout != null && linkTimeout.isServiceEnableLinkTimeout();
    }


    /**
     * Get the full-link timeout time. The rule is: if the client has configured the time,
     * take the shorter one as the timeout time for this call, and the unit is ms.
     *
     * @param context RpcClientContext {@link RpcClientContext}
     * @param rpcMethodName String
     * @return LeftTimeout {@link LeftTimeout}
     */
    protected LeftTimeout parseLeftTime(RpcClientContext context, String rpcMethodName) {
        // the timeout time set by the caller. Priority to context.
        int methodTimeout = context.getTimeoutMills();
        methodTimeout = (methodTimeout <= 0 ? consumerConfig.getMethodTimeout(rpcMethodName) : methodTimeout);
        // get the remaining time for the whole process. If this is the first caller, linkTimeout will be null.
        LinkInvokeTimeout linkTimeout = RpcContextUtils.getValueMapValue(context,
                RpcContextValueKeys.CTX_LINK_INVOKE_TIMEOUT);
        // if it is the first call or the server has not enabled the whole process timeout,
        // the response will be returned directly.
        if (linkTimeout == null || !linkTimeout.isServiceEnableLinkTimeout()) {
            return new LeftTimeout(methodTimeout, methodTimeout);
        }
        long costTime = System.currentTimeMillis() - linkTimeout.getStartTime();
        // if the client does not set a timeout for a single call, the timeout for the entire process will be used.
        if (methodTimeout <= 0) {
            return new LeftTimeout((int) linkTimeout.getTimeout(), (int) (linkTimeout.getLeftTimeout() - costTime));
        }
        // remaining timeout for the whole process.
        int linkLeftTime = (int) (linkTimeout.getLeftTimeout() - costTime);
        return new LeftTimeout(
                Math.min(methodTimeout, (int) linkTimeout.getTimeout()),
                Math.min(methodTimeout, linkLeftTime));
    }

    @Override
    protected Request buildRequest(LeftTimeout leftTimeout, RpcInvocation invocation, Object[] args) {
        RpcClientContext context = (RpcClientContext) args[0];
        DefRequest newRequest = new DefRequest();
        newRequest.setRequestId(SeqUtils.genIntegerSeq());
        newRequest.setContext(context);
        newRequest.setInvocation(invocation);
        newRequest.getAttachments().putAll(context.getReqAttachMap());
        RequestMeta newMeta = newRequest.getMeta();
        newMeta.setConsumerConfig(consumerConfig);
        if (consumerConfig.getLocalAddress() != null) {
            newMeta.setLocalAddress(consumerConfig.getLocalAddress());
        }
        newMeta.setTimeout(leftTimeout.getLeftTimeout());
        newMeta.setOneWay(context.isOneWay());
        newMeta.setDyeingKey(context.getDyeingKey());
        newMeta.setHashVal(context.getHashVal());
        // set the caller and callee information. First, set the context data,
        // and then set the system default values for the fields that have not been set.
        setCallerAndCalleeInfo(context, invocation, newMeta);
        return newRequest;
    }

    protected void setCallerAndCalleeInfo(RpcClientContext context, RpcInvocation inv, RequestMeta newMeta) {
        CallInfo newCallInfo = context.getCallInfo().clone();
        newMeta.setCallInfo(newCallInfo);
        ServerConfig serverConfig = ConfigManager.getInstance().getServerConfig();
        if (serverConfig != null) {
            setCaller(newCallInfo, serverConfig);
        }
        setCallee(inv, newCallInfo);
    }

    @Override
    protected Object parseResponse(Request request, LeftTimeout leftTimeout, CompletionStage<Response> response) {
        CompletableFuture<Response> resultFuture = response.toCompletableFuture();
        // oneWay model
        if (request.getContext().isOneWay()) {
            return null;
        }
        InvokeMode invokeMode = request.getInvocation().getInvokeMode();
        // async
        if (InvokeMode.isAsync(invokeMode)) {
            return RpcUtils.parseAsyncInvokeResult(resultFuture, request.getContext(),
                    request.getInvocation().getRpcMethodInfo());
        }
        // stream
        if (InvokeMode.isStream(invokeMode)) {
            return RpcUtils.parseStreamInvokeResult(response, invokeMode);
        }
        // Processing synchronization results without enabling backup request
        int backupRequestTimeMs = consumerConfig.getBackupRequestTimeMs();
        if (backupRequestTimeMs <= 0) {
            return RpcUtils.parseSyncInvokeResult(resultFuture, request.getContext(), leftTimeout.getLeftTimeout(),
                    leftTimeout.getOriginTimeout(), request.getInvocation().getRpcMethodInfo());
        }
        // Process the synchronization result of opening backup request
        return RpcUtils.parseSyncInvokeBackupResult(resultFuture, backupRequestTimeMs, leftTimeout, invoker, request);
    }

}
