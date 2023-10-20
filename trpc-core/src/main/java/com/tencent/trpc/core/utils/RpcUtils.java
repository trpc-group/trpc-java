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

package com.tencent.trpc.core.utils;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.cluster.ClusterInvoker;
import com.tencent.trpc.core.common.RpcResult;
import com.tencent.trpc.core.common.TRpcProtocolType;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.ExceptionHelper;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.GenericClient;
import com.tencent.trpc.core.rpc.InvokeMode;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcContextValueKeys;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.core.rpc.def.LeftTimeout;
import com.tencent.trpc.core.rpc.def.LinkInvokeTimeout;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Rpc utility.
 */
public class RpcUtils {

    private static final Logger logger = LoggerFactory.getLogger(RpcUtils.class);

    /**
     * Used to cache method execution types to avoid repeated parsing.
     */
    private static final ConcurrentMap<Method, InvokeMode> invokeModes = Maps.newConcurrentMap();

    /**
     * If the return value is CompletionStage, it is considered a future mode.
     */
    private static boolean isReturnFutureType(Method method) {
        Class<?> clazz = method.getReturnType();
        return CompletionStage.class.isAssignableFrom(clazz);
    }

    /**
     * Determines if the interface is a streaming interface.
     *
     * <p>
     * Currently supports the following streaming interface formats:
     *     <ol>
     *         <li>Server-side streaming: Flux&lt;RspT&gt; serverStream(RpcContext ctx, ReqT request)</li>
     *         <li>Client-side streaming: Mono&lt;RspT&gt; clientStream(RpcContext ctx, Flux&lt;ReqT&gt; requests)</li>
     *         <li>Bi-directional streaming: Flux&lt;RspT&gt;
     *         duplexStream(RpcContext ctx, Flux&lt;ReqT&gt; requests)</li>
     *     </ol>
     * </p>
     *
     * @param method the method to be determined
     * @return the interface type, or {@code null} if not a streaming interface
     */
    private static InvokeMode getStreamMode(Method method) {
        Class<?> returnType = method.getReturnType();
        Class<?>[] parameterTypes = method.getParameterTypes();
        // all streaming interfaces have 2 parameters, and the first one is RpcContext
        if (parameterTypes.length != 2 || !RpcContext.class.isAssignableFrom(parameterTypes[0])) {
            return null;
        }

        if (Mono.class.isAssignableFrom(returnType)) {
            if (Publisher.class.isAssignableFrom(parameterTypes[1])) {
                return InvokeMode.CLIENT_STREAM;
            }
        } else if (Flux.class.isAssignableFrom((returnType))) {
            if (Publisher.class.isAssignableFrom(parameterTypes[1])) {
                return InvokeMode.DUPLEX_STREAM;
            } else {
                return InvokeMode.SERVER_STREAM;
            }
        }

        return null;
    }

    /**
     * Parse the method execution type, such as asynchronous, synchronous, streaming, etc.
     *
     * @param method the method to parse
     * @return the execution type
     */
    public static InvokeMode parseInvokeMode(Method method) {
        // due to the introduction of streaming proxy, it may cause two repeated parsing of function types, so this
        // cache is used here
        return invokeModes.computeIfAbsent(method, key -> {
            if (isReturnFutureType(method)) {
                return InvokeMode.ASYNC;
            }
            InvokeMode streamMode = getStreamMode(method);
            return streamMode != null ? streamMode : InvokeMode.SYNC;
        });
    }

    /**
     * In some scenarios where there is no annotation, the default is used, currently the default is null.
     *
     * @param method the method to parse
     * @param defaultName the default name
     * @return the parsed RPC method name
     */
    public static String parseRpcMethodName(Method method, String defaultName) {
        TRpcMethod rpcMeta = method.getAnnotation(TRpcMethod.class);
        return rpcMeta == null ? defaultName : rpcMeta.name();
    }

    /**
     * In some scenarios where there is no annotation, the default is used, currently the default is null.
     *
     * @param method the method to parse
     * @param defaultName the default name
     * @return the parsed RPC method aliases
     */
    public static String[] parseRpcMethodAliases(Method method, String[] defaultName) {
        TRpcMethod rpcMeta = method.getAnnotation(TRpcMethod.class);
        return rpcMeta == null ? defaultName : rpcMeta.alias();
    }

    public static boolean isDefaultRpcMethod(Method method) {
        TRpcMethod rpcMeta = method.getAnnotation(TRpcMethod.class);
        return rpcMeta != null && rpcMeta.isDefault();
    }

    /**
     * In some scenarios where there is no annotation, the default is used, currently the default is null.
     *
     * @param serviceType the service type to parse
     * @param defaultName the default name
     * @return the parsed RPC service name
     */
    public static String parseRpcServiceName(Class<?> serviceType, String defaultName) {
        TRpcService rpcMeta = serviceType.getAnnotation(TRpcService.class);
        return rpcMeta == null ? defaultName : rpcMeta.name();
    }

    public static boolean isGenericClient(Class<?> serviceInstance) {
        return serviceInstance != null && GenericClient.class.isAssignableFrom(serviceInstance);
    }

    /**
     * Check if the current RPC interface contains only one protocol type (stream, standard), and return the protocol
     * type.
     *
     * @param serviceInterface RPC interface
     * @return protocol type
     * @throws IllegalArgumentException if serviceInterface contains multi protocol types.
     */
    public static TRpcProtocolType checkAndGetProtocolType(Class<?> serviceInterface) {
        boolean hasStandardMethods = false;
        boolean hasStreamMethods = false;
        for (Method method : serviceInterface.getDeclaredMethods()) {
            String rpcMethodName = RpcUtils.parseRpcMethodName(method, null);
            if (rpcMethodName == null) {
                continue;
            }

            InvokeMode invokeMode = RpcUtils.parseInvokeMode(method);
            if (InvokeMode.isStream(invokeMode)) {
                hasStreamMethods = true;
            } else {
                hasStandardMethods = true;
            }
        }

        if (hasStandardMethods && hasStreamMethods) {
            throw new IllegalArgumentException("interface(" + serviceInterface + ") contains multi protocol types");
        }
        return hasStreamMethods ? TRpcProtocolType.STREAM : TRpcProtocolType.STANDARD; // default STANDARD
    }

    /**
     * Whether it is a generalized interface.
     *
     * @param method the method to check
     * @return true if it is a generalized method
     */
    public static boolean isGenericMethod(Method method) {
        TRpcMethod rpcMeta = method.getAnnotation(TRpcMethod.class);
        return rpcMeta != null && rpcMeta.isGeneric();
    }

    public static Response newResponse(Request request, Object value, Throwable ex) {
        DefResponse rsp = new DefResponse();
        rsp.setRequestId(request.getRequestId());
        rsp.setValue(value);
        rsp.setException(ex);
        rsp.setRequest(request);
        return rsp;
    }

    /**
     * Parse the result of synchronous mode.
     *
     * @param responseFuture the result Future object
     * @param context request context
     * @param leftTime the remaining timeout for this call
     * @param timeout the timeout
     * @param methodInfo the RpcMethodInfo object
     * @return the result
     */
    public static Object parseSyncInvokeResult(CompletableFuture<?> responseFuture, RpcContext context,
            long leftTime, long timeout, RpcMethodInfo methodInfo) {
        try {
            Object resp = getResponse(responseFuture, (int) leftTime);
            return wrapperIfReturnCommonResult(methodInfo, resp);
        } catch (TimeoutException e) {
            boolean linkTimeout = linkInvokeTimeoutEnable(context);
            logger.error("the leftTime:{}, timeout:{}, isLinkInvokeTimeout:{}, ex:", leftTime, timeout, linkTimeout, e);
            if (linkTimeout) {
                return wrapperWithExceptionIfReturnCommonResult(methodInfo, TRpcException.newFrameException(
                        ErrorCode.TRPC_LINK_INVOKE_TIMEOUT_ERR, "link invoke request timeout > " + timeout + " ms", e));
            }
            return wrapperWithExceptionIfReturnCommonResult(methodInfo, TRpcException.newFrameException(
                    ErrorCode.TRPC_CLIENT_INVOKE_TIMEOUT_ERR, "timeout > " + timeout + " ms", e));
        } catch (ExecutionException e) {
            return wrapperWithExceptionIfReturnCommonResult(methodInfo, e.getCause());
        } catch (Exception e) {
            return wrapperWithExceptionIfReturnCommonResult(methodInfo, e);
        }
    }

    /**
     * Parse the result of synchronous backup request mode.
     *
     * @param responseFuture the result Future object
     * @param backupRequestTimeMs backup request time
     * @param leftTimeout the remaining timeout
     * @param invoker client interface
     * @param request request data
     * @return the result
     */
    public static Object parseSyncInvokeBackupResult(CompletableFuture<?> responseFuture,
            int backupRequestTimeMs, LeftTimeout leftTimeout, ClusterInvoker<?> invoker, Request request) {
        try {
            Object resp = getResponse(responseFuture, backupRequestTimeMs);
            return wrapperIfReturnCommonResult(request.getInvocation().getRpcMethodInfo(), resp);
        } catch (TimeoutException ex) {
            Request backupRequest = buildBackupRequest(request);
            CompletableFuture<Object> finalResultFuture = CompletableFuture.anyOf(responseFuture,
                    invoker.invoke(backupRequest).toCompletableFuture());
            return RpcUtils.parseSyncInvokeResult(finalResultFuture, backupRequest.getContext(),
                    leftTimeout.getLeftTimeout() - backupRequestTimeMs, leftTimeout.getOriginTimeout(),
                    request.getInvocation().getRpcMethodInfo());
        } catch (ExecutionException ex) {
            return wrapperWithExceptionIfReturnCommonResult(request.getInvocation().getRpcMethodInfo(), ex.getCause());
        } catch (Exception ex) {
            return wrapperWithExceptionIfReturnCommonResult(request.getInvocation().getRpcMethodInfo(), ex);
        }
    }

    private static Object getResponse(CompletableFuture<?> responseFuture, int timeout)
            throws InterruptedException, ExecutionException, TimeoutException {
        Response response = (Response) responseFuture.get(timeout, TimeUnit.MILLISECONDS);
        if (response == null) {
            return null;
        }
        if (response.getException() != null) {
            throw TRpcException.trans(response.getException());
        }
        return response.getValue();
    }

    private static Object wrapperIfReturnCommonResult(RpcMethodInfo methodInfo, Object resp) {
        if (isReturnCommonResult(methodInfo)) {
            return RpcResult.success(resp);
        }
        return resp;
    }

    private static Object wrapperWithExceptionIfReturnCommonResult(RpcMethodInfo rpcMethodInfo, Throwable cause) {
        if (isReturnCommonResult(rpcMethodInfo)) {
            return RpcResult.fail(TRpcException.trans(cause));
        }
        throw TRpcException.trans(cause);
    }

    private static boolean isReturnCommonResult(RpcMethodInfo methodInfo) {
        return null != methodInfo && methodInfo.getReturnType() == RpcResult.class;
    }

    /**
     * Return the result of the asynchronous mode.
     *
     * @param responseFuture the result Future object
     * @param context request context
     * @param methodInfo the RpcMethodInfo object
     * @return the result
     */
    public static CompletableFuture<Object> parseAsyncInvokeResult(CompletableFuture<?> responseFuture,
            RpcContext context, RpcMethodInfo methodInfo) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        responseFuture.whenComplete((r, t) -> {
            Throwable ex = ExceptionHelper.parseResponseException((Response) r, t);
            if (isReturnCommonResult(methodInfo)) {
                if (ex != null) {
                    future.complete(RpcResult.fail(TRpcException.trans(ex)));
                    return;
                }
                if (r != null) {
                    future.complete(RpcResult.success(((Response) r).getValue()));
                    return;
                }
                future.complete(RpcResult.success());
                return;
            }
            if (ex != null) {
                if (!linkInvokeTimeoutEnable(context)) {
                    future.completeExceptionally(ex);
                    return;
                }
                if (ex instanceof TRpcException) {
                    TRpcException tRpcException = (TRpcException) ex;
                    // if it is a client-side call timeout, return the full link call timeout here.
                    if (tRpcException.getCode() == ErrorCode.TRPC_CLIENT_INVOKE_TIMEOUT_ERR) {
                        tRpcException = TRpcException.newFrameException(ErrorCode.TRPC_LINK_INVOKE_TIMEOUT_ERR,
                                "link invoke " + tRpcException.getMessage());
                        future.completeExceptionally(tRpcException);
                        return;
                    }
                }
                future.completeExceptionally(ex);
            } else if (r != null) {
                future.complete(((Response) r).getValue());
            } else {
                future.complete(null);
            }
        });
        return future;
    }

    /**
     * Return the result of the streaming mode.
     *
     * @param response response future
     * @param invokeMode method type
     * @return the result
     */
    public static Publisher<?> parseStreamInvokeResult(CompletionStage<Response> response, InvokeMode invokeMode) {
        // based on the method type, return a specific result object
        switch (invokeMode) {
            case CLIENT_STREAM:
                // return Mono
                return Mono.fromCompletionStage(response).flatMap(r -> (Mono<?>) r.getValue());
            case SERVER_STREAM:
            case DUPLEX_STREAM:
                // return Flux
                return Mono.fromCompletionStage(response).flatMapMany(r -> (Publisher<?>) r.getValue());
            default:
                throw new UnsupportedOperationException("unknown method type " + invokeMode);
        }
    }

    /**
     * Build a backup request. Only change requestId.
     *
     * @param request the original request
     * @return the backup request
     */
    private static Request buildBackupRequest(Request request) {
        Request backupRequest = request.clone();
        backupRequest.setRequestId(SeqUtils.genIntegerSeq());
        return backupRequest;
    }

    /**
     * Check if the full link timeout is enabled.
     *
     * @param context the RpcContext
     * @return true if enabled, false if not enabled
     */
    private static boolean linkInvokeTimeoutEnable(RpcContext context) {
        // if null, it means it's the first main call, so it cannot be enabled
        LinkInvokeTimeout linkTimeout = RpcContextUtils.getValueMapValue(context,
                RpcContextValueKeys.CTX_LINK_INVOKE_TIMEOUT);
        return linkTimeout != null && linkTimeout.isServiceEnableLinkTimeout();
    }

}