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

package com.tencent.trpc.spring.cloud.gateway.client;

import com.google.common.base.Preconditions;
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.GenericClient;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.TRpcProxy;
import java.net.URI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

/**
 * The core function of TrpcGatewayClient is to provide Client creation, context supplement, and asynchronous call
 * capabilities.
 */
public class TrpcGatewayClient implements GatewayClient {

    public static final String HEADERS = "headers";
    public static final String COOKIES = "cookies";
    public static final String QUERY = "querystring";

    private GenericClient client;
    private CallInfo callInfo;

    public TrpcGatewayClient(Route route) {
        open(route);
    }

    /**
     * Create a client using URI.
     * The routing format is the standard URI definition, for example:
     * <b>trpc://ServiceName/ServicefullName/methodName</b>
     * When <b>route.getUri() == null</b> is true, or when the corresponding service is not found, it will throw {@link
     * IllegalArgumentException}
     *
     * @param route gateway routing configuration information
     */
    @Override
    public void open(Route route) {
        Preconditions.checkNotNull(route, "Create GenericClient fail: Route is null! Please check route config!");
        if (route.getUri() != null) {
            callInfo = parseCallInfo(route.getUri().getSchemeSpecificPart());
        } else {
            callInfo = new CallInfo();
        }
        client = createClient(route.getUri());
    }

    /**
     * Create a client using URI.
     * The routing format is the standard URI definition, for example:
     * <b>trpc://ServiceName/ServicefullName/methodName</b>
     * When <b>uri == null</b> is true, or when the corresponding service is not found, it will throw @throws {@link
     * IllegalArgumentException}
     *
     * @param uri gateway routing uri configuration information
     * @return GenericClient, the generic client instance
     */
    private GenericClient createClient(URI uri) {
        Preconditions.checkNotNull(uri, "Create GenericClient fail: Route uri is null! Please check route config!");
        String serviceName = TrpcGatewayClient.parseCallInfo(uri.getSchemeSpecificPart()).getCallee();
        GenericClient client = TRpcProxy.getProxy(serviceName);
        Preconditions.checkNotNull(client,
                "Create GenericClient fail: Don't found trpc service by name = '" + serviceName
                        + "'! Please check route config and trpc-java config!");
        return client;
    }

    /**
     * Create context information {@link RpcClientContext}
     * <p></p>
     * Convert TRPC packet header from HTTP request. Since the context handling for each route may be different,
     *
     * @param request http request
     * @param route route information, including metadata information. It can help to implement the requirements
     *         of different configurations corresponding to different contexts.
     * @return RpcClientContext, the converted context after HTTP
     */
    private RpcClientContext createContext(ServerHttpRequest request, Route route) {
        RpcClientContext context = new RpcClientContext();
        context.setRpcServiceName(callInfo.getCalleeService());
        context.setRpcMethodName(callInfo.getCalleeMethod());
        context.getReqAttachMap().put(HEADERS, request.getHeaders());
        context.getReqAttachMap().put(COOKIES, request.getCookies());
        context.getReqAttachMap().put(QUERY, request.getQueryParams());
        return context;
    }

    /**
     * TRPC provides the asyncInvoke method for direct asynchronous calls. It also does the conversion between
     * CompletableFuture and Mono.
     *
     * @param request http request
     * @param route route information, including metadata information. It can help to implement the requirements
     *         of different configurations corresponding to different contexts.
     * @param body the specific data for the call
     * @return Mono, to adapt to the SpringGateway standard. The return type is {@link Mono}.
     */
    public Mono<byte[]> asyncInvoke(ServerHttpRequest request, Route route, byte[] body) {
        RpcClientContext context = createContext(request, route);
        return Mono.fromFuture(client.asyncInvoke(context, body).toCompletableFuture());
    }

    /**
     * Build callInfo using URI.
     * The routing format is the standard TRPC URI definition, for example:
     * <b>trpc://ServiceName/ServicefullName/methodName</b>
     * When the configuration does not match the TRPC three-segment path, it will throw {@link IllegalArgumentException}
     *
     * @param uri gateway routing uri configuration information
     * @return CallInfo an object containing serviceName and method of {@link CallInfo}
     */
    public static CallInfo parseCallInfo(String uri) {
        if (StringUtils.isEmpty(uri)) {
            throw new NullPointerException(
                    "URI is null! Please check route config!");
        }
        // TRPC URI definition three-segment split, TRPC path standard: trpc://ServiceName/ServicefullName/methodName
        String[] uriSplit = uri.split("/");
        // A total of 5 segments
        if (uriSplit.length == 5) {
            CallInfo callInfo = new CallInfo();
            // ServiceName
            callInfo.setCallee(uriSplit[2]);
            // ServicefullName
            callInfo.setCalleeService(uriSplit[3]);
            // methodName
            callInfo.setCalleeMethod(uriSplit[4]);
            return callInfo;
        }
        throw new IllegalArgumentException(
                "Create RpcInvocation fail: URI does not meet path specification, uri = '" + uri
                        + "'! Please check route config!");
    }

}
