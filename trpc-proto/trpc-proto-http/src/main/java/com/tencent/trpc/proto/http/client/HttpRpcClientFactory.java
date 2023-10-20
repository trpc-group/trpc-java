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

import static com.tencent.trpc.transport.http.common.Constants.HTTP2C_SCHEME;
import static com.tencent.trpc.transport.http.common.Constants.HTTP2_SCHEME;
import static com.tencent.trpc.transport.http.common.Constants.HTTPS_SCHEME;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.rpc.RpcClient;
import com.tencent.trpc.core.rpc.spi.RpcClientFactory;
import com.tencent.trpc.transport.http.util.HttpUtils;

@Extension("http")
public class HttpRpcClientFactory implements RpcClientFactory {

    /**
     * Create an HTTP client based on different HTTP protocols.
     *
     * @param config protocol config
     * @return http client
     * @throws TRpcException if create RpcClient failed
     */
    @Override
    public RpcClient createRpcClient(ProtocolConfig config) throws TRpcException {
        switch (HttpUtils.getScheme(config)) {
            case HTTP2C_SCHEME:
                return new Http2cRpcClient(config);
            case HTTP2_SCHEME:
                return new Http2RpcClient(config);
            case HTTPS_SCHEME:
                return new HttpsRpcClient(config);
            default:
                return new HttpRpcClient(config);
        }
    }
}
