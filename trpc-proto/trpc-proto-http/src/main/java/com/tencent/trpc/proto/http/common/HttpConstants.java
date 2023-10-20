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

package com.tencent.trpc.proto.http.common;

public class HttpConstants {

    /**
     * HTTP protocol
     */
    public static final String HTTP_SCHEME = "http";

    /**
     * HTTPS protocol
     */
    public static final String HTTPS_SCHEME = "https";

    /**
     * HTTP/2 protocol based on plaintext.
     */
    public static final String HTTP2C_SCHEME = "http2c";

    /**
     * HTTP/2 protocol based on HTTPS.
     */
    public static final String HTTP2_SCHEME = "h2";

    /**
     * HTTP GET method
     */
    public static final String HTTP_METHOD_GET = "GET";

    /**
     * HTTP POST method
     */
    public static final String HTTP_METHOD_POST = "POST";

    /**
     * HTTP response result with content-type in PB format.
     */
    public static final String CONTENT_TYPE_PROTOBUF = "application/x-protobuf";

    /**
     * HTTP response result with content-type in JSON format.
     */
    public static final String CONTENT_TYPE_JSON = "application/json";

    /**
     * HTTP response result with content-type in JSON format and charset as UTF-8.
     */
    public static final String CONTENT_TYPE_JSON_WITH_CHARSET = "application/json; charset=UTF-8";

    /**
     * URI path seperator
     */
    public static final String URI_SEPARATOR = "/";

    /**
     * The default trpc path name
     */
    public static final String TRPC_NAME = "trpc";

    /**
     * Default root path of the URI for HTTP requests.
     */
    public static final String RPC_CALL_DEFAULT_PATH = URI_SEPARATOR + TRPC_NAME;

    /**
     * Key for the routing service name in RPC requests (in this case, for HTTP).
     */
    public static final String RPC_CALL_PARAM_SERVICE = "service";

    /**
     * Key for the routing method name in RPC requests (in this case, for HTTP).
     */
    public static final String RPC_CALL_PARAM_METHOD = "method";

    /**
     * Key for the HTTP native request.
     */
    public static final String TRPC_ATTACH_SERVLET_REQUEST = "trpc-attach-servlet-request";

    /**
     * Key for the HTTP native result.
     */
    public static final String TRPC_ATTACH_SERVLET_RESPONSE = "trpc-attach-servlet-response";

    /**
     * Key for the routing service name carried in the HTTP native request.
     */
    public static final String REQUEST_ATTRIBUTE_TRPC_SERVICE = "trpc-service";

    /**
     * Key for the routing service method carried in the HTTP native request.
     */
    public static final String REQUEST_ATTRIBUTE_TRPC_METHOD = "trpc-method";

    /**
     * Key for the request sequence number carried in the HTTP native request.
     */
    public static final String HTTP_HEADER_TRPC_REQUEST_ID = "Trpc-Request-Id";

    /**
     * Key for the request timeout carried in the HTTP native request.
     */
    public static final String HTTP_HEADER_TRPC_TIMEOUT = "Trpc-Timeout";

    /**
     * Key for the caller information carried in the HTTP native request.
     */
    public static final String HTTP_HEADER_TRPC_CALLER = "Trpc-Caller";

    /**
     * Key for the callee information carried in the HTTP native request.
     */
    public static final String HTTP_HEADER_TRPC_CALLEE = "Trpc-Callee";

    /**
     * Key for the message type carried in the HTTP native request.
     */
    public static final String HTTP_HEADER_TRPC_MESSAGE_TYPE = "Trpc-Message-Type";

    /**
     * Key for the request attachment carried in the HTTP native request.
     */
    public static final String HTTP_HEADER_TRPC_TRANS_INFO = "Trpc-Trans-Info";

    /**
     * Key for the HTTP handshake timeout configuration in the Trpc extension configuration.
     */
    public static final String CONNECTION_REQUEST_TIMEOUT = "connection_request_timeout";

}
