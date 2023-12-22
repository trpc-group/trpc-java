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
import static com.tencent.trpc.proto.http.common.HttpConstants.HTTP_HEADER_TRPC_CALLEE;
import static com.tencent.trpc.proto.http.common.HttpConstants.HTTP_HEADER_TRPC_CALLER;

import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.utils.RpcUtils;
import com.tencent.trpc.proto.http.common.HttpConstants;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;

/**
 * HTTP protocol client invoker.
 */
public class HttpConsumerInvoker<T> extends AbstractConsumerInvoker<T> {

    public HttpConsumerInvoker(HttpRpcClient client, ConsumerConfig<T> config,
            ProtocolConfig protocolConfig) {
        super(client, config, protocolConfig);
    }

    /**
     * All HTTP calls are blocking calls.
     *
     * @param request TRPC request
     * @return TRPC response
     * @throws Exception if send request failed
     */
    @Override
    public Response send(Request request) throws Exception {
        HttpPost httpPost = buildRequest(request);

        CloseableHttpClient httpClient = ((HttpRpcClient) client).getHttpClient();

        try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
            return handleResponse(request, httpResponse);
        } catch (Exception ex) {
            return RpcUtils.newResponse(request, null, ex);
        }
    }

    /**
     * Convert the HTTP response to inner TRPC response.
     *
     * @param request TRPC request
     * @param httpResponse HTTP response
     * @return TRPC response
     * @throws Exception if parse HTTP response failed
     */
    private Response handleResponse(Request request, CloseableHttpResponse httpResponse) throws Exception {
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            throw TRpcException.newBizException(statusCode,
                    httpResponse.getStatusLine().getReasonPhrase());
        }
        //handle http header
        //Parse the data passed through from the server to the client.
        Map<String, Object> respAttachments = new HashMap<>();
        for (Header header : httpResponse.getAllHeaders()) {
            String name = header.getName();
            for (HeaderElement element : header.getElements()) {
                String value = element.getName();
                respAttachments.put(name, value);
            }
        }

        Header contentLengthHdr = httpResponse.getFirstHeader(HttpHeaders.CONTENT_LENGTH);

        if (contentLengthHdr != null) {
            int contentLength = Integer.parseInt(contentLengthHdr.getValue().trim());
            // NOTE: By default, the HTTP implementation must return the content length.
            // However, other HTTP implementations may not return the content length,
            // so strong validation is not performed here.
            if (contentLength == 0) {
                Response response = RpcUtils.newResponse(request, null, null);
                response.setAttachments(respAttachments);
                return response;
            }
        }
        // Decoded response result.
        InputStream in = httpResponse.getEntity().getContent();
        String decodeIn = IOUtils.toString(in, StandardCharsets.UTF_8);
        Object value = decodeFromJson(
                request.getInvocation().getRpcMethodInfo().getActualReturnType(), decodeIn);
        Response response = RpcUtils.newResponse(request, value, null);
        response.setAttachments(respAttachments);
        return response;
    }

    /**
     * Wrap the TRPC request as an HTTP request and remove conflicting Header headers.
     * See {@link org.apache.http.protocol.RequestContent#process(HttpRequest, HttpContext)}
     *
     * @param request TRPC request
     * @return HttpPost, the wrapped HTTP request
     * @throws Exception if an error occurs during the request building process
     */
    private HttpPost buildRequest(Request request) throws Exception {
        BackendConfig backendConfig = config.getBackendConfig();
        int connectionRequestTimeout = Integer.parseInt(backendConfig.getExtMap()
                .getOrDefault(CONNECTION_REQUEST_TIMEOUT, DEFAULT_CLIENT_REQUEST_TIMEOUT_MS).toString());
        int connectTimeout = backendConfig.getConnTimeout();
        int socketTimeout = backendConfig.getRequestTimeout();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(connectionRequestTimeout)
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout)
                .build();

        HttpPost httpPost = new HttpPost(getUri(request));
        httpPost.setConfig(requestConfig);
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON);
        // set custom business headers, consistent with the TRPC protocol, only process String and byte[]
        request.getAttachments().forEach((k, v) -> {
            if (Objects.equals(k, HttpHeaders.TRANSFER_ENCODING) || Objects.equals(k, HttpHeaders.CONTENT_LENGTH)) {
                return;
            }
            if (v instanceof String) {
                httpPost.setHeader(k, String.valueOf(v));
            } else if (v instanceof byte[]) {
                httpPost.setHeader(k, new String((byte[]) v));
            }
        });
        // set caller and callee information
        CallInfo callInfo = request.getMeta().getCallInfo();
        httpPost.setHeader(HTTP_HEADER_TRPC_CALLER, callInfo.getCaller());
        httpPost.setHeader(HTTP_HEADER_TRPC_CALLEE, callInfo.getCallee());

        // encode request parameters
        String jsonString = encodeToJson(request);
        if (jsonString != null) {
            httpPost.setEntity(new StringEntity(jsonString, StandardCharsets.UTF_8));
        }
        return httpPost;
    }

}
