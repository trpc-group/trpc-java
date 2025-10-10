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

package com.tencent.trpc.proto.http.server;

import com.fasterxml.jackson.core.Base64Variants;
import com.google.protobuf.Message;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.rpc.RequestMeta;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.rpc.common.RpcMethodInfoAndInvoker;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import com.tencent.trpc.proto.http.common.ErrorResponse;
import com.tencent.trpc.proto.http.common.HttpCodec;
import com.tencent.trpc.proto.http.common.HttpConstants;
import com.tencent.trpc.proto.http.common.RpcServerContextWithHttp;
import com.tencent.trpc.proto.http.common.TrpcServletRequestWrapper;
import com.tencent.trpc.proto.http.common.TrpcServletResponseWrapper;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

/**
 * The core processing logic of the HTTP protocol, which is inherited by both the HTTP and Spring MVC modules.
 */
public abstract class AbstractHttpExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AbstractHttpExecutor.class);

    /**
     * Http codec tool
     */
    protected HttpCodec httpCodec;

    protected void execute(HttpServletRequest request, HttpServletResponse response,
            RpcMethodInfoAndInvoker methodInfoAndInvoker) {
        AtomicBoolean responded = new AtomicBoolean(false);
        try {

            DefRequest rpcRequest = buildDefRequest(request, response, methodInfoAndInvoker);

            CompletableFuture<Void> completionFuture = new CompletableFuture<>();

            // use a thread pool for asynchronous processing
            invokeRpcRequest(methodInfoAndInvoker.getInvoker(), rpcRequest, completionFuture, responded);

            // If the request carries a timeout, use this timeout to wait for the request to be processed.
            // If not carried, use the default timeout.
            long requestTimeout = rpcRequest.getMeta().getTimeout();
            if (requestTimeout <= 0) {
                requestTimeout = methodInfoAndInvoker.getInvoker().getConfig().getRequestTimeout();
            }
            if (requestTimeout > 0) {
                try {
                    completionFuture.get(requestTimeout, TimeUnit.MILLISECONDS);
                } catch (TimeoutException ex) {
                    if (responded.compareAndSet(false, true)) {
                        doErrorReply(request, response, TRpcException.newFrameException(ErrorCode.TRPC_SERVER_TIMEOUT_ERR,
                                "wait http request execute timeout"));
                    }
                }
            } else {
                completionFuture.get();
            }
        } catch (Exception ex) {
            logger.error("dispatch request [{}] error", request, ex);
            if (responded.compareAndSet(false, true)) {
                doErrorReply(request, response, ex);
            }
        }
    }

    /**
     * Get the mapped internal method.
     *
     * @param object the key to query the mapped internal method, maby {@link HttpServletRequest} or directly
     * {@link RpcMethodInfoAndInvoker}.
     * @return the mapped internal method
     */
    protected abstract RpcMethodInfoAndInvoker getRpcMethodInfoAndInvoker(Object object);

    /**
     * Request processing
     *
     * @param invoker the invoker
     * @param rpcRequest the rpc request
     * @param completionFuture the completion future
     * @param responded the responded flag
     */
    private void invokeRpcRequest(ProviderInvoker<?> invoker, DefRequest rpcRequest,
            CompletableFuture<Void> completionFuture,
            AtomicBoolean responded) {

        WorkerPool workerPool = invoker.getConfig().getWorkerPoolObj();

        if (null == workerPool) {
            logger.error("dispatch rpcRequest [{}]  error, workerPool is empty", rpcRequest);
            completionFuture.completeExceptionally(
                    TRpcException.newFrameException(ErrorCode.TRPC_SERVER_NOSERVICE_ERR, "not found service, workerPool is empty")
            );
            return;
        }

        workerPool.execute(() -> {
            try {
                HttpServletResponse response = getOriginalResponse(rpcRequest);

                CompletionStage<Response> rpcFuture = invoker.invoke(rpcRequest);

                rpcFuture.whenComplete((result, throwable) -> {
                    try {
                        if (responded.get()) {
                            return;
                        }

                        if (throwable != null) {
                            throw throwable;
                        }

                        if (result.getException() != null) {
                            throw result.getException();
                        }

                        if (responded.compareAndSet(false, true)) {
                            response.setStatus(HttpStatus.SC_OK);
                            httpCodec.writeHttpResponse(response, result);
                            response.flushBuffer();
                        }

                        completionFuture.complete(null);
                    } catch (Throwable t) {
                        handleError(t, rpcRequest, response, responded, completionFuture);
                    }
                });

            } catch (Exception e) {
                handleError(e, rpcRequest, getOriginalResponse(rpcRequest), responded, completionFuture);
            }
        });
    }

    /**
     * Handle error
     */
    private void handleError(Throwable t, DefRequest rpcRequest, HttpServletResponse response,
            AtomicBoolean responded, CompletableFuture<Void> completionFuture) {
        try {
            if (responded.compareAndSet(false, true)) {
                HttpServletRequest request = getOriginalRequest(rpcRequest);
                logger.warn("reply message error, channel: [{}], msg:[{}]", request.getRemoteAddr(), request, t);
                httpErrorReply(request, response, ErrorResponse.create(request, HttpStatus.SC_SERVICE_UNAVAILABLE, t));
            }
        } finally {
            completionFuture.completeExceptionally(t);
        }
    }

    /**
     * Build the context request.
     *
     * @param request the original http request
     * @param response the original http response
     * @param methodInfoAndInvoker mapped method invocation info
     * @return trpc request info
     * @throws Exception if build DefRequest failed
     */
    private DefRequest buildDefRequest(HttpServletRequest request, HttpServletResponse response,
            RpcMethodInfoAndInvoker methodInfoAndInvoker) throws Exception {
        DefRequest rpcRequest = new DefRequest();

        // Setup invocation
        rpcRequest.setInvocation(buildRpcInvocation(request, methodInfoAndInvoker.getMethodInfo()));

        // Fill in the TRPC protocol header information. If there is an exception in the field format,
        // throw it directly.
        setRequestMeta(request, rpcRequest.getMeta());

        // Set the basic information of the request.
        setBasicInfo(request, rpcRequest);

        // Set the context information for the server processing, including the original request and the result.
        setRpcServerContext(request, response, rpcRequest);

        // Transmit the attachments from HTTP headers to inner trpc attachments.
        setAttachments(request, rpcRequest);

        return rpcRequest;
    }

    /**
     * Write error reply to http client
     *
     * @param request the original http request
     * @param response the original http response
     * @param errorResponse Error msg wrpper class
     */
    protected void httpErrorReply(HttpServletRequest request, HttpServletResponse response,
            ErrorResponse errorResponse) {
        if (logger.isWarnEnabled()) {
            logger.warn("http call of {} {}?{} failed, status: {}", request.getMethod(),
                    request.getRequestURI(), request.getQueryString(), errorResponse.getStatus());
        }
        try {
            response.setStatus(errorResponse.getStatus());
            DefResponse rpcResponse = new DefResponse();
            rpcResponse.setValue(errorResponse);
            httpCodec.writeHttpResponse(response, rpcResponse);
            response.flushBuffer();
        } catch (Exception ex) {
            logger.error("http send error response status failed", ex);
        }
    }

    /**
     * Write error reply to http client
     *
     * @param request the original http request
     * @param response the original http response
     * @param ex the invocation exception
     */
    protected void doErrorReply(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        int code = ErrorCode.TRPC_INVOKE_UNKNOWN_ERR;
        if (ex instanceof TRpcException) {
            code = ((TRpcException) ex).getCode();
        }

        switch (code) {
            case ErrorCode.TRPC_SERVER_NOFUNC_ERR:
            case ErrorCode.TRPC_SERVER_NOSERVICE_ERR:
                httpErrorReply(request, response,
                        ErrorResponse.create(request, HttpStatus.SC_NOT_FOUND, ex, "not found service"));
                break;
            case ErrorCode.TRPC_SERVER_TIMEOUT_ERR:
                httpErrorReply(request, response,
                        ErrorResponse.create(request, HttpStatus.SC_REQUEST_TIMEOUT, ex, "request timeout"));
                break;
            case ErrorCode.TRPC_SERVER_VALIDATE_ERR:
                httpErrorReply(request, response, ErrorResponse.create(
                        request, HttpStatus.SC_BAD_REQUEST, ex, "service validate error"));
                break;
            case ErrorCode.TRPC_SERVER_AUTH_ERR:
                httpErrorReply(request, response, ErrorResponse.create(
                        request, HttpStatus.SC_UNAUTHORIZED, ex, "no auth"));
                break;
            case ErrorCode.TRPC_SERVER_OVERLOAD_ERR:
                httpErrorReply(request, response, ErrorResponse.create(
                        request, HttpStatus.SC_INTERNAL_SERVER_ERROR, ex, "too many request"));
                break;
            case ErrorCode.TRPC_SERVER_ENCODE_ERR:
                httpErrorReply(request, response, ErrorResponse.create(
                        request, HttpStatus.SC_INTERNAL_SERVER_ERROR, ex, "server encode error"));
                break;
            case ErrorCode.TRPC_SERVER_SYSTEM_ERR:
                httpErrorReply(request, response, ErrorResponse.create(
                        request, HttpStatus.SC_INTERNAL_SERVER_ERROR, ex, "server system error"));
                break;
            default:
                httpErrorReply(request, response, ErrorResponse.create(request, HttpStatus.SC_SERVICE_UNAVAILABLE, ex));
        }

    }

    /**
     * Setup trpc invocation info
     *
     * @param request the original http request
     * @param methodInfo the mapped method info
     * @return trpc invocation info
     * @throws Exception if build RpcInvocation failed
     */
    private RpcInvocation buildRpcInvocation(HttpServletRequest request, RpcMethodInfo methodInfo) throws Exception {

        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setRpcServiceName((String) request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE));
        rpcInvocation.setRpcMethodName((String) request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD));
        rpcInvocation.setArguments(parseRpcParams(request, methodInfo));
        rpcInvocation.setRpcMethodInfo(methodInfo);
        rpcInvocation.setFunc(String.format("/%s/%s",
                rpcInvocation.getRpcServiceName(), rpcInvocation.getRpcMethodName()));
        return rpcInvocation;

    }

    /**
     * Decode invocation params
     *
     * @param request the original http request
     * @param methodInfo the mapped method info
     * @return the decoded request data
     * @throws Exception if parse RpcParams failed
     */
    @SuppressWarnings("unchecked")
    private Object[] parseRpcParams(HttpServletRequest request, RpcMethodInfo methodInfo) throws Exception {
        Type[] paramsTypes = methodInfo.getParamsTypes();

        //  Currently, TRpc services only support two parameters, the first one is the context
        //  and the second one is the request body.
        if (paramsTypes.length != 2 || !RpcContext.class
                .isAssignableFrom((Class<?>) paramsTypes[0])) {
            throw new UnsupportedOperationException("only support trpc service signature");
        }

        Class<?> reqType = (Class<?>) paramsTypes[1];
        Object[] arguments = new Object[1];
        if (Message.class.isAssignableFrom(reqType)) {
            arguments[0] = httpCodec.convertToPBParam(request, (Class<? extends Message>) reqType);
        } else if (Map.class.isAssignableFrom(reqType)) {
            arguments[0] = httpCodec.convertToJsonParam(request);
        } else {
            // Directly convert to POJO.
            arguments[0] = httpCodec.convertToJavaBean(request, reqType);
        }
        return arguments;
    }


    /**
     * Setup trpc request meta
     *
     * @param request the original http request
     * @param rpcRequestMeta trpc request meta info
     */
    private void setRequestMeta(HttpServletRequest request, RequestMeta rpcRequestMeta) {
        if (StringUtils.isNotBlank(request.getRemoteAddr())) {
            rpcRequestMeta.setRemoteAddress(new InetSocketAddress(request.getRemoteAddr(), request.getRemotePort()));
        }
        CallInfo callInfo = rpcRequestMeta.getCallInfo();
        String caller = request.getHeader(HttpConstants.HTTP_HEADER_TRPC_CALLER);
        if (StringUtils.isNotEmpty(caller)) {
            setCaller(callInfo, caller);
        }
        String callee = request.getHeader(HttpConstants.HTTP_HEADER_TRPC_CALLEE);
        if (StringUtils.isNotEmpty(callee)) {
            setCallee(callInfo, callee);
        } else {
            setDefaultCallee(callInfo, request);
        }

        String messageType = request.getHeader(HttpConstants.HTTP_HEADER_TRPC_MESSAGE_TYPE);
        if (StringUtils.isNotEmpty(messageType)) {
            rpcRequestMeta.addMessageType(Integer.parseInt(messageType));
        }
    }

    /**
     * Setup request basic info
     *
     * @param request the original http request
     * @param rpcRequest trpc request info
     */
    private void setBasicInfo(HttpServletRequest request, DefRequest rpcRequest) {
        String requestId = request.getHeader(HttpConstants.HTTP_HEADER_TRPC_REQUEST_ID);
        if (StringUtils.isNotEmpty(requestId)) {
            rpcRequest.setRequestId(Long.parseLong(requestId.trim()));
        }
        String timeout = request.getHeader(HttpConstants.HTTP_HEADER_TRPC_TIMEOUT);
        if (StringUtils.isNotEmpty(timeout)) {
            rpcRequest.getMeta().setTimeout(Integer.parseInt(timeout.trim()));
        }
    }

    /**
     * Set the context information for the server processing.
     *
     * @param request the original http request
     * @param response the original http response
     * @param rpcRequest trpc request info
     */
    private void setRpcServerContext(HttpServletRequest request, HttpServletResponse response,
            DefRequest rpcRequest) {
        RpcServerContext serverContext = new RpcServerContextWithHttp();
        rpcRequest.setContext(serverContext);

        rpcRequest.getAttachments().put(
                HttpConstants.TRPC_ATTACH_SERVLET_REQUEST, new TrpcServletRequestWrapper(request));
        rpcRequest.getAttachments().put(
                HttpConstants.TRPC_ATTACH_SERVLET_RESPONSE, new TrpcServletResponseWrapper(response));
        Enumeration<String> enums = request.getHeaderNames();
        while (enums.hasMoreElements()) {
            String header = enums.nextElement();
            String value = request.getHeader(header);
            // In the tRPC protocol, the value of the attachment is stored and used as a byte array
            // to maintain consistency.
            rpcRequest.getAttachments().put(header, value.getBytes(StandardCharsets.UTF_8));
        }
        logger.debug("request attachment: {}", JsonUtils.toJson(rpcRequest.getAttachments()));
    }

    /**
     * Transmit the attachments from HTTP headers to inner trpc attachments.
     *
     * @param request the original http request
     * @param rpcRequest trpc request info
     */
    private void setAttachments(HttpServletRequest request, DefRequest rpcRequest)
            throws TRpcException {
        // To transmit the trans_info data in JSON format, the value of trans_info in the tRPC protocol
        // is represented as bytes. In the HTTP scenario, bytes are transmitted through base64 encoding.
        String transInfo = request.getHeader(HttpConstants.HTTP_HEADER_TRPC_TRANS_INFO);
        if (StringUtils.isNotEmpty(transInfo)) {
            Map<String, Object> attachments = rpcRequest.getAttachments();
            Map<String, Object> trans = JsonUtils.fromJson(transInfo, Map.class);
            trans.forEach((key, val) -> {
                byte[] bytes = Base64Variants.getDefaultVariant().decode((String) val);
                attachments.put(key, bytes);
            });
        }
    }

    /**
     * Get the original http request
     *
     * @param request trpc request info
     * @return Native HTTP request proxy that shields non-serializable properties.
     */
    private HttpServletRequest getOriginalRequest(DefRequest request) {
        return (HttpServletRequest) request.getAttachments().get(HttpConstants.TRPC_ATTACH_SERVLET_REQUEST);
    }

    /**
     * Get the native HTTP response.
     *
     * @param request trpc request info
     * @return Native HTTP request proxy that shields non-serializable properties.
     */
    private HttpServletResponse getOriginalResponse(DefRequest request) {
        return (HttpServletResponse) request.getAttachments().get(HttpConstants.TRPC_ATTACH_SERVLET_RESPONSE);
    }

    /**
     * Setup default callee info
     *
     * @param callInfo call info
     * @param request the original http request
     */
    private void setDefaultCallee(CallInfo callInfo, HttpServletRequest request) {
        ServerConfig serverConfig = ConfigManager.getInstance().getServerConfig();
        if (serverConfig != null) {
            callInfo.setCalleeApp(serverConfig.getApp());
            callInfo.setCalleeServer(serverConfig.getServer());
        }
        callInfo.setCalleeService((String) request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE));
        callInfo.setCalleeMethod((String) request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD));
    }

    private void setCallee(CallInfo callInfo, String callee) {
        String[] strings = callee.trim().split("\\.");
        callInfo.setCalleeApp(getApp(strings))
                .setCalleeServer(getServer(strings))
                .setCalleeService(getService(strings))
                .setCalleeMethod(getMethod(strings)).setCallee(callee);
    }

    private void setCaller(CallInfo callInfo, String caller) {
        String[] strings = caller.trim().split("\\.");
        callInfo.setCallerApp(getApp(strings))
                .setCallerServer(getServer(strings))
                .setCallerService(getService(strings))
                .setCaller(caller);
    }

    private String getApp(String[] callInfos) {
        return getString(callInfos, 2, 1);
    }

    private String getServer(String[] callInfos) {
        return getString(callInfos, 3, 2);
    }

    private String getService(String[] callInfos) {
        return getString(callInfos, 4, 3);
    }

    private String getMethod(String[] callInfos) {
        return getString(callInfos, 5, 4);
    }

    private String getString(String[] callInfos, int length, int cursor) {
        return callInfos.length < length ? StringUtils.EMPTY : callInfos[cursor];
    }

}
