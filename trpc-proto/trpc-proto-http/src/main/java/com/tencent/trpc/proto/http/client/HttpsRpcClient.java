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

import com.tencent.trpc.core.common.config.ProtocolConfig;
import org.apache.hc.core5.http2.HttpVersionPolicy;

/**
 * HTTPS protocol client. This capability inherits from {@link Http2RpcClient} and is only used
 * when the interaction protocol is inconsistent. HTTPS forces the use of HTTP1 to communicate
 * with the server.
 */
public class HttpsRpcClient extends Http2RpcClient {

    public HttpsRpcClient(ProtocolConfig config) {
        super(config);
        this.clientVersionPolicy = HttpVersionPolicy.FORCE_HTTP_1;
    }
}
