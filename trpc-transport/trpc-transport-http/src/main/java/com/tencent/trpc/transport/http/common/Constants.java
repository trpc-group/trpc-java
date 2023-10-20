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

package com.tencent.trpc.transport.http.common;

public class Constants {

    /**
     * HTTP protocol
     */
    public static final String HTTP_SCHEME = "http";

    /**
     * HTTP 1.1 protocol
     */
    public static final String HTTP1_SCHEME = "http/1.1";

    /**
     * HTTPS protocol
     */
    public static final String HTTPS_SCHEME = "https";

    /**
     * HTTP/2 protocol based on plaintext
     */
    public static final String HTTP2C_SCHEME = "http2c";

    /**
     * HTTP/2 protocol based on HTTPS
     */
    public static final String HTTP2_SCHEME = "h2";

    /**
     * The keystore path
     */
    public static final String KEYSTORE_PATH = "keystore_path";

    /**
     * The keystore password
     */
    public static final String KEYSTORE_PASS = "keystore_pass";

}
