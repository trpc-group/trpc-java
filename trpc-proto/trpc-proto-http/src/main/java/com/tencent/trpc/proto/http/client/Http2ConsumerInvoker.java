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

package com.tencent.trpc.proto.http.client;


import static com.tencent.trpc.core.common.Constants.DEFAULT_CLIENT_REQUEST_TIMEOUT_MS;
import static com.tencent.trpc.proto.http.common.HttpConstants.CONNECTION_REQUEST_TIMEOUT;

import autovalue.shaded.com.google.common.common.base.Objects;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.utils.RpcUtils;
import com.tencent.trpc.proto.http.common.HttpConstants;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequests;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;

/**
 * HTTP/2 protocol client invoker, supporting both h2 and http2c.
 */
public class Http2ConsumerInvoker<T> extends AbstractConsumerInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(Http2ConsumerInvoker.class);

    public Http2ConsumerInvoker(Http2cRpcClient client, ConsumerConfig<T> config,
            ProtocolConfig protocolConfig) {
        super(client, config, protocolConfig);
    }

    /**
     * The actual invocation of the client to initiate a request.
     * Implementation may vary depending on the HTTP protocol used.
     *
     * @param request client request
     * @return Response
     * @throws Exception if send request failed
     */
    @Override
    public Response send(Request request) throws Exception {
        int requestTimeout = config.getBackendConfig().getRequestTimeout();
        SimpleHttpRequest simpleHttpRequest = buildRequest(request, requestTimeout);

        try {
            SimpleHttpResponse simpleHttpResponse = execute(request, requestTimeout,
                    simpleHttpRequest);

            return handleResponse(request, simpleHttpResponse);
        } catch (Exception e) {
            return RpcUtils.newResponse(request, null, e);
        }

    }

    /**
     * Convert the HTTP response to inner TRPC response.
     *
     * @param request TRPC request
     * @param simpleHttpResponse HTTP response
     * @return TRPC response
     * @throws Exception if parse HTTP response failed
     */
    private Response handleResponse(Request request, SimpleHttpResponse simpleHttpResponse) throws Exception {
        // 1. check Http status == 200
        int statusCode = simpleHttpResponse.getCode();
        if (simpleHttpResponse.getCode() != HttpStatus.SC_OK) {
            throw TRpcException
                    .newBizException(statusCode, simpleHttpResponse.getReasonPhrase());
        }
        // handle http header
        // Parse the data passed through from the server to the client.
        Map<String, Object> respAttachments = new HashMap<>();
        for (Header header : simpleHttpResponse.getHeaders()) {
            String name = header.getName();
            String value = header.getValue();
            respAttachments.put(name, value);
        }

        Header contentLengthHdr = simpleHttpResponse.getFirstHeader(HttpHeaders.CONTENT_LENGTH);
        if (contentLengthHdr != null) {
            int contentLength = Integer.parseInt(contentLengthHdr.getValue().trim());
            // NOTE: The default HTTP implementation always returns the contentLength,
            // but other HTTP implementations may not return it. Therefore,
            // no strict validation is performed here.
            if (contentLength == 0) {
                Response response = RpcUtils.newResponse(request, null, null);
                response.setAttachments(respAttachments);
                return response;
            }
        }

        // 2. decode the returned result
        Object value = decodeFromJson(
                request.getInvocation().getRpcMethodInfo().getActualReturnType(),
                simpleHttpResponse.getBodyText());

        Response response = RpcUtils.newResponse(request, value, null);
        response.setAttachments(respAttachments);
        return response;
    }

    /**
     * The actual request invocation for the http2c protocol.
     *
     * @param request TRPC request
     * @param requestTimeout request timeout
     * @param simpleHttpRequest HTTP request
     * @return HTTP response
     * @throws Exception if do HTTP request failed
     */
    private SimpleHttpResponse execute(Request request, int requestTimeout,
            SimpleHttpRequest simpleHttpRequest) throws Exception {
        CloseableHttpAsyncClient httpAsyncClient = ((Http2cRpcClient) client).getHttpAsyncClient();
        Future<SimpleHttpResponse> httpResponseFuture = httpAsyncClient.execute(simpleHttpRequest,
                new FutureCallback<SimpleHttpResponse>() {
                    @Override
                    public void completed(SimpleHttpResponse result) {
                        if (logger.isDebugEnabled()) {
                            logger.debug(result.getBodyText());
                        }
                    }

                    @Override
                    public void failed(Exception ex) {
                        String msg = String
                                .format("request has exception > %s ms, service=%s, "
                                                + "method=%s, remoteAddr=%s, exception=%s",
                                        requestTimeout,
                                        request.getInvocation().getRpcServiceName(),
                                        request.getInvocation().getRpcMethodName(),
                                        request.getMeta().getRemoteAddress(),
                                        ex.getMessage());
                        logger.error(msg);
                    }

                    @Override
                    public void cancelled() {
                        String msg = String
                                .format("request cancel > %s ms, service=%s, "
                                                + "method=%s, remoteAddr=%s",
                                        requestTimeout,
                                        request.getInvocation().getRpcServiceName(),
                                        request.getInvocation().getRpcMethodName(),
                                        request.getMeta().getRemoteAddress());
                        logger.error(msg);
                    }
                });
        return httpResponseFuture.get(requestTimeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Wrap the TRPC request into an HTTP request.
     *
     * @param request TRPC request
     * @param requestTimeout request timeout
     * @return HTTP request
     * @throws Exception if build HTTP request failed
     */
    private SimpleHttpRequest buildRequest(Request request, int requestTimeout) throws Exception {
        BackendConfig backendConfig = config.getBackendConfig();
        int connectionRequestTimeout = Integer.parseInt(backendConfig.getExtMap()
                .getOrDefault(CONNECTION_REQUEST_TIMEOUT, DEFAULT_CLIENT_REQUEST_TIMEOUT_MS).toString());
        int connectTimeout = backendConfig.getConnTimeout();

        RequestConfig requestConfig = RequestConfig
                .custom()
                .setConnectionRequestTimeout(connectionRequestTimeout, TimeUnit.MILLISECONDS)
                .setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .setResponseTimeout(requestTimeout, TimeUnit.MILLISECONDS)
                .build();

        SimpleHttpRequest simpleHttpRequest = SimpleHttpRequests.post(getUri(request));
        simpleHttpRequest.setConfig(requestConfig);
        simpleHttpRequest.setHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON);
        // encode request
        String jsonString = encodeToJson(request);
        if (jsonString != null) {
            simpleHttpRequest.setBody(jsonString, ContentType.APPLICATION_JSON);
        }
        // set custom business headers, consistent with the TRPC protocol, only process String and byte[]
        request.getAttachments().forEach((k, v) -> {
            if (Objects.equal(k, HttpHeaders.TRANSFER_ENCODING) || Objects.equal(k, HttpHeaders.CONTENT_LENGTH)) {
                return;
            }
            if (v instanceof String) {
                simpleHttpRequest.setHeader(k, String.valueOf(v));
            } else if (v instanceof byte[]) {
                simpleHttpRequest.setHeader(k, new String((byte[]) v));
            }
        });
        return simpleHttpRequest;
    }

}
