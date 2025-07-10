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

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.rpc.common.RpcMethodInfoAndInvoker;
import com.tencent.trpc.core.rpc.def.DefMethodInfoRegister;
import com.tencent.trpc.proto.http.common.ErrorResponse;
import com.tencent.trpc.proto.http.common.HttpCodec;
import com.tencent.trpc.proto.http.common.HttpConstants;
import com.tencent.trpc.transport.http.HttpExecutor;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

/**
 * The processing logic of the HTTP module
 */
public class DefaultHttpExecutor extends AbstractHttpExecutor implements HttpExecutor {

    private static final Logger logger = LoggerFactory.getLogger(DefaultHttpExecutor.class);

    private final ProtocolConfig config;

    /**
     * HTTP protocol registered route cache.
     */
    private DefMethodInfoRegister methodRegister = new DefMethodInfoRegister();

    public DefaultHttpExecutor(ProtocolConfig config) {
        this.config = config;
        this.httpCodec = new HttpCodec();
    }

    @Override
    public void execute(HttpServletRequest request, HttpServletResponse response) {
        if (!validateAndSendError(request, response)) {
            return;
        }
        super.execute(request, response, getRpcMethodInfoAndInvoker(request));
    }

    /**
     * Get the mapped internal method.
     *
     * @param object the key to query the mapped internal method, in this class only using {@link HttpServletRequest}
     * @return the mapped internal method
     */
    @Override
    protected RpcMethodInfoAndInvoker getRpcMethodInfoAndInvoker(Object object) {
        if (object instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) object;
            String func = methodRegister.getNativeHttpFunc(request.getPathInfo());

            logger.debug("got http trpc request, func: {}", func);

            RpcMethodInfoAndInvoker methodAndInvoker = methodRegister.route(func);
            if (null == methodAndInvoker) {
                String serviceName = request.getParameter(HttpConstants.RPC_CALL_PARAM_SERVICE);
                String methodName = request.getParameter(HttpConstants.RPC_CALL_PARAM_METHOD);
                methodAndInvoker = router(serviceName, methodName, true);
            }
            setServiceMethod(request, methodAndInvoker);
            return methodAndInvoker;
        }
        throw TRpcException.newFrameException(ErrorCode.TRPC_SERVER_NOFUNC_ERR, "not found rpc invoker %s", object);
    }

    /**
     * Set the service method in the HTTP request attribute.
     *
     * @param request http request
     * @param route internal route mapping
     */
    private void setServiceMethod(HttpServletRequest request, RpcMethodInfoAndInvoker route) {
        if (null != route) {
            request.setAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE,
                    route.getMethodRouterKey().getRpcServiceName());
            request.setAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD,
                    route.getMethodRouterKey().getRpcMethodName());
        }
    }

    /**
     * Validate if the request information is correct: path, request method and content, request payload length.
     *
     * @param request the original http request
     * @param response the original http response
     */
    private boolean validateAndSendError(HttpServletRequest request, HttpServletResponse response) {
        String pathInfo = request.getPathInfo();
        if (!pathExist(pathInfo)) {
            httpErrorReply(request, response,
                    ErrorResponse.create(request, HttpStatus.SC_NOT_FOUND, "not found service"));
            return false;
        }

        String method = request.getMethod();

        // Currently, only GET and POST methods are allowed. For the POST method,
        // perform content format and length validation.
        if (HttpConstants.HTTP_METHOD_POST.equalsIgnoreCase(method)) {

            // Validate content format.
            if (!validateRequestContent(request)) {
                httpErrorReply(request, response,
                        ErrorResponse.create(request, HttpStatus.SC_METHOD_NOT_ALLOWED, "content type error"));
                return false;
            }

            // Validate content length.
            if (request.getContentLength() > config.getPayload()) {
                httpErrorReply(request, response, ErrorResponse.create(request, HttpStatus.SC_REQUEST_TOO_LONG,
                        "content length too long"));
                return false;
            }

        } else if (!HttpConstants.HTTP_METHOD_GET.equalsIgnoreCase(method)) {
            httpErrorReply(request, response, ErrorResponse.create(request, HttpStatus.SC_METHOD_NOT_ALLOWED,
                    "http method is not allow"));
            return false;
        }
        return true;
    }

    private boolean validateRequestContent(HttpServletRequest request) {
        if (StringUtils.isBlank(request.getContentType())) {
            return false;
        }
        String contentType = request.getContentType().toLowerCase();
        return contentType.startsWith(HttpConstants.CONTENT_TYPE_JSON) || contentType
                .startsWith(HttpConstants.CONTENT_TYPE_PROTOBUF);
    }

    /**
     * Check if the path exists.
     *
     * @param path service path
     * @return true if path exists
     */
    private boolean pathExist(String path) {
        return methodRegister.validateNativeHttpPath(path);
    }

    /**
     * Find the registered route by the request information.
     *
     * @param serviceName service name
     * @param methodName method name
     * @param throwException whether an exception should be thrown when the route cannot be found
     * @return router info
     */
    private RpcMethodInfoAndInvoker router(String serviceName, String methodName,
            boolean throwException) {
        if (throwException) {
            return Optional.ofNullable(methodRegister.route(serviceName, methodName))
                    .<TRpcException>orElseThrow(() -> {
                        throw TRpcException.newFrameException(ErrorCode.TRPC_SERVER_NOFUNC_ERR,
                                "not found {rpcServiceName=%s, rpcMethodName=%s}", serviceName,
                                methodName);
                    });
        } else {
            return methodRegister.route(serviceName, methodName);
        }
    }

    public <T> void register(ProviderInvoker<T> invoker) {
        methodRegister.register(invoker);
    }

    public <T> void unregister(ProviderConfig<T> config) {
        methodRegister.unregister(config);
    }

    public void destroy() {
        methodRegister.clear();
    }
}