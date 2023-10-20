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

package com.tencent.trpc.transport.http.util;


import static com.tencent.trpc.transport.http.common.Constants.HTTP2C_SCHEME;
import static com.tencent.trpc.transport.http.common.Constants.HTTP2_SCHEME;
import static com.tencent.trpc.transport.http.common.Constants.HTTPS_SCHEME;
import static com.tencent.trpc.transport.http.common.Constants.HTTP_SCHEME;
import static com.tencent.trpc.transport.http.common.Constants.KEYSTORE_PASS;
import static com.tencent.trpc.transport.http.common.Constants.KEYSTORE_PATH;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import java.util.Map;

public class HttpUtils {

    /**
     * Get the protocol of the server. If a keystore is configured, HTTPS or h2 is enabled;
     * otherwise, HTTP or http2c is used.
     *
     * @param config protocol config
     * @return the protocol scheme
     */
    public static String getScheme(ProtocolConfig config) {
        String protocol = config.getProtocol();
        Map<String, Object> extMap = config.getExtMap();
        boolean useHttps = extMap.containsKey(KEYSTORE_PATH) && extMap.containsKey(KEYSTORE_PASS);
        if (HTTP_SCHEME.equals(protocol)) {
            return useHttps ? HTTPS_SCHEME : HTTP_SCHEME;
        } else if (HTTP2_SCHEME.equals(protocol)) {
            return useHttps ? HTTP2_SCHEME : HTTP2C_SCHEME;
        } else {
            return HTTP_SCHEME;
        }
    }

}
