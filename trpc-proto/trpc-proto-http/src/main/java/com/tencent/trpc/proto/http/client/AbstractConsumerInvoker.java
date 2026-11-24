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

package com.tencent.trpc.proto.http.client;


import static com.tencent.trpc.proto.http.common.HttpConstants.URI_SEPARATOR;
import static com.tencent.trpc.transport.http.common.Constants.HTTPS_SCHEME;
import static com.tencent.trpc.transport.http.common.Constants.HTTP_SCHEME;
import static com.tencent.trpc.transport.http.common.Constants.KEYSTORE_PASS;
import static com.tencent.trpc.transport.http.common.Constants.KEYSTORE_PATH;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.protobuf.Message;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.AbstractRpcClient;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.TimeoutManager;
import com.tencent.trpc.core.rpc.def.DefTimeoutManager;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.core.utils.ProtoJsonConverter;
import com.tencent.trpc.core.utils.RpcUtils;
import com.tencent.trpc.core.utils.StringUtils;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.hc.core5.net.URIBuilder;

/**
 * An abstract ConsumerInvoker used for http client invocation. It mainly implements the preparation of
 * the request before invocation and the processing of the result after invocation. Subclasses need to
 * implement the send method to support invocation of different HTTP protocols.
 */
public abstract class AbstractConsumerInvoker<T> implements ConsumerInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractConsumerInvoker.class);

    /**
     * The request timeout manager, used to handle the result of client requests in timeout scenarios.
     */
    private static final TimeoutManager TIMEOUT_MANAGER = new DefTimeoutManager(10);

    /**
     * Http client for the request
     */
    protected final AbstractRpcClient client;
    /**
     * Client configuration
     */
    protected final ConsumerConfig<T> config;
    /**
     * Protocol configuration
     */
    protected final ProtocolConfig protocolConfig;

    /**
     * The protocol used for client requests. if a keystore certificate is configured, HTTPS (same as H2)
     * is used. Otherwise, HTTP (same as HTTP2C) is used by default.
     */
    private String scheme = HTTP_SCHEME;

    public AbstractConsumerInvoker(AbstractRpcClient client,
            ConsumerConfig<T> config, ProtocolConfig protocolConfig) {
        this.client = client;
        this.config = config;
        this.protocolConfig = protocolConfig;

        // determine if HTTPS protocol is enabled
        Map<String, Object> extMap = protocolConfig.getExtMap();
        if (extMap.containsKey(KEYSTORE_PATH) && extMap.containsKey(KEYSTORE_PASS)) {
            scheme = HTTPS_SCHEME;
        }
    }

    /**
     * The actual invocation of the client's request. Subclasses need to implement this method
     * to support different HTTP protocols.
     *
     * @param request client request
     * @return Response
     * @throws Exception if send request failed
     */
    public abstract Response send(Request request) throws Exception;

    /**
     * Client request method, differentiate between one-way requests and normal requests.
     *
     * @param request client request
     * @return Response
     */
    @Override
    public CompletionStage<Response> invoke(Request request) {
        RpcClientContext clientContext = (RpcClientContext) (request.getContext());

        if (clientContext.isOneWay()) {
            return oneWayRequest(request);
        } else {
            return normalRequest(request);
        }
    }

    /**
     * The main process of normal requests.
     *
     * @param request TRPC request
     * @return The CompletionStage of TRPC response
     */
    private CompletionStage<Response> normalRequest(Request request) {
        // 1. get the client worker pool
        WorkerPool workerPool = config.getBackendConfig().getWorkerPoolObj();

        // 2. get the request timeout
        RpcInvocation invocation = request.getInvocation();
        String rpcMethodName = invocation.getRpcMethodName();
        long timeoutMills = getConfig().getMethodTimeout(rpcMethodName);

        CompletableFuture<Response> responseFuture = new CompletableFuture<>();
        // 3. immediately return when request timeout.
        TIMEOUT_MANAGER.watch(() -> {
            String msg = String
                    .format("request timeout > %s ms, service=%s, method=%s, remoteAddr=%s",
                            timeoutMills, request.getInvocation().getRpcServiceName(),
                            request.getInvocation().getRpcMethodName(),
                            request.getMeta().getRemoteAddress());

            responseFuture.complete(RpcUtils.newResponse(request, null,
                    TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_INVOKE_TIMEOUT_ERR, msg)));
        }, timeoutMills);

        // 4. submit to the workerPool for execution
        workerPool.execute(() -> {
            try {
                responseFuture.complete(send(request));
            } catch (Exception ex) {
                responseFuture.complete(RpcUtils.newResponse(request, null, ex));
            }
        });

        return responseFuture;
    }

    /**
     * The process of one-way requests, which is return immediately without waiting for the request to complete.
     *
     * @param request TRPC request
     * @return The CompletionStage of TRPC response
     */
    private CompletionStage<Response> oneWayRequest(Request request) {
        WorkerPool workerPool = config.getBackendConfig().getWorkerPoolObj();
        workerPool.execute(() -> {
            try {
                send(request);
            } catch (Exception ex) {
                logger.error("send request to " + request.getMeta().getRemoteAddress() + " failed", ex);
            }
        });
        return CompletableFuture.completedFuture(RpcUtils.newResponse(request, null, null));
    }

    /**
     * Encode the request params in JSON format.
     *
     * @param request client request
     * @return Params in JSON format
     */
    protected String encodeToJson(Request request) {
        RpcInvocation invocation = request.getInvocation();
        Object[] arguments = invocation.getArguments();
        if (arguments == null || arguments.length <= 0) {
            return null;
        }
        Object param = arguments[0];

        Type[] paramsTypes = invocation.getRpcMethodInfo().getParamsTypes();
        if (paramsTypes.length <= 1) {
            return null;
        }
        Class<?> reqType = (Class<?>) paramsTypes[1];

        if (Message.class.isAssignableFrom(reqType)) {
            Map<String, Object> jsonData = ProtoJsonConverter.messageToMap((Message) param);
            return JsonUtils.toJson(jsonData);
        } else {
            return JsonUtils.toJson(param);
        }
    }

    /**
     * Decode the result from a JSON string and support generic deserialization.
     *
     * @param returnType the actual return type
     * @param jsonStr json string
     * @return decoded param
     * @throws Exception if decode json param failed
     */
    protected Object decodeFromJson(Type returnType, String jsonStr) throws Exception {
        // generic deserialization
        boolean isGeneric = returnType instanceof ParameterizedType || returnType instanceof TypeVariable
                || returnType instanceof GenericArrayType
                || returnType instanceof WildcardType;

        if (isGeneric) {
            return JsonUtils.fromBytes(jsonStr, new TypeReference<Object>() {
                @Override
                public Type getType() {
                    return returnType;
                }
            });
        }

        Class<?> type = (Class<?>) returnType;
        if (Message.class.isAssignableFrom(type)) {
            Method getDefaultInstance = type.getDeclaredMethod("getDefaultInstance");
            Message pbMsg = (Message) getDefaultInstance.invoke(null);

            return ProtoJsonConverter.jsonToMessage(jsonStr, pbMsg);
        } else {
            return JsonUtils.fromJson(jsonStr, type);
        }
    }

    /**
     * Get the URI of the request.
     *
     * @param request client request
     * @return the constructed URI
     * @throws URISyntaxException if build uri failed
     */
    protected URI getUri(Request request) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme(this.scheme);
        uriBuilder.setHost(protocolConfig.getIp());
        uriBuilder.setPort(protocolConfig.getPort());

        String basePath = config.getBackendConfig().getBasePath();
        RpcInvocation invocation = request.getInvocation();

        StringBuilder stringBuilder = new StringBuilder();
        if (!StringUtils.isEmpty(basePath) && !URI_SEPARATOR.equals(basePath)) {
            stringBuilder.append(basePath);
        }
        stringBuilder.append(invocation.getFunc());

        uriBuilder.setPath(stringBuilder.toString());
        return uriBuilder.build();
    }

    @Override
    public ConsumerConfig<T> getConfig() {
        return config;
    }

    @Override
    public ProtocolConfig getProtocolConfig() {
        return protocolConfig;
    }

    @Override
    public Class<T> getInterface() {
        return config.getServiceInterface();
    }
}
