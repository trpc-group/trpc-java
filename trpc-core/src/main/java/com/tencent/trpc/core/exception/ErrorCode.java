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

package com.tencent.trpc.core.exception;

public interface ErrorCode {

    /**
     * Successful invocation
     */
    int TRPC_INVOKE_SUCCESS = 0;

    /* Server error codes
     * Main categories:
     * 1. Protocol, 2. Service or func routing, 3. Queue timeout or overload
     * Other categories to be supplemented
     */

    /**
     * Server decoding error
     */
    int TRPC_SERVER_DECODE_ERR = 1;
    /**
     * Server encoding error
     */
    int TRPC_SERVER_ENCODE_ERR = 2;

    /**
     * Server does not call the corresponding service implementation
     */
    int TRPC_SERVER_NOSERVICE_ERR = 11;
    /**
     * Server does not call the corresponding interface implementation
     */
    int TRPC_SERVER_NOFUNC_ERR = 12;

    /**
     * Request timeout on the server
     */
    int TRPC_SERVER_TIMEOUT_ERR = 21;
    /**
     * Request overload on the server
     */
    int TRPC_SERVER_OVERLOAD_ERR = 22;

    /**
     * Server throttling
     */
    int TRPC_SERVER_LIMITED_ERR = 23;

    /**
     * Server system error
     */
    int TRPC_SERVER_SYSTEM_ERR = 31;

    /**
     * Server authentication failure error
     */
    int TRPC_SERVER_AUTH_ERR = 41;

    /**
     * Server request parameter automatic validation failure error
     */
    int TRPC_SERVER_VALIDATE_ERR = 51;

    /* Client error codes
     * Main categories:
     * 1. Timeout, 2. Network, 3. Protocol, 4. Routing
     * Other categories to be supplemented
     */

    /**
     * Request invocation timeout on the client
     */
    int TRPC_CLIENT_INVOKE_TIMEOUT_ERR = 101;

    /**
     * Request full link invocation timeout
     */
    int TRPC_LINK_INVOKE_TIMEOUT_ERR = 102;
    /**
     * Client connection error
     */
    int TRPC_CLIENT_CONNECT_ERR = 111;

    /**
     * Client encoding error
     */
    int TRPC_CLIENT_ENCODE_ERR = 121;
    /**
     * Client decoding error
     */
    int TRPC_CLIENT_DECODE_ERR = 122;

    /**
     * Client throttling
     */
    int TRPC_CLIENT_LIMITED_ERR = 123;

    /**
     * Client overload
     */
    int TRPC_CLIENT_OVERLOAD_ERR = 124;

    /**
     * Client IP routing error
     */
    int TRPC_CLIENT_ROUTER_ERR = 131;

    /**
     * Client network error
     */
    int TRPC_CLIENT_NETWORK_ERR = 141;

    /**
     * Client response parameter automatic validation failure error
     */
    int TRPC_CLIENT_VALIDATE_ERR = 151;

    /**
     * Undefined error
     */
    int TRPC_INVOKE_UNKNOWN_ERR = 999;

    /**
     * JSON serialization error
     */
    int JSON_SERIALIZATION_ERR = 2000;
    /**
     * JSON deserialization error
     */
    int JSON_DESERIALIZATION_ERR = 2001;

    /**
     * Signature verification failed
     */
    int SIGNATURE_VERIFY_FAILURE = 3000;

    /**
     * Get auth info error
     */
    int GET_AUTH_INFO_ERR = 5000;
    /**
     * Get acl info error
     */
    int GET_ACL_INFO_ERR = 5001;

    enum Stream {

        /**
         * Frame decoding error
         */
        FRAME_DECODE_MAGIC_ERR(201, "frame magic decode error"),
        ;

        private final int statusCode;

        private final String message;

        Stream(int statusCode, String message) {
            this.statusCode = statusCode;
            this.message = message;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getMessage() {
            return message;
        }
    }

}
