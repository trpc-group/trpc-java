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

package com.tencent.trpc.proto.standard.client;

import com.tencent.trpc.core.common.TRpcProtocolType;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.rpc.RpcClient;
import com.tencent.trpc.core.rpc.spi.RpcClientFactory;
import com.tencent.trpc.proto.standard.common.StandardClientCodec;
import com.tencent.trpc.proto.standard.stream.client.TRpcStreamClient;
import com.tencent.trpc.proto.standard.stream.codec.TRpcStreamFrameDecoder;
import com.tencent.trpc.proto.support.DefRpcClient;

/**
 * TRPC protocol rpc client factory
 */
@Extension("trpc")
public class StandardRpcClientFactory implements RpcClientFactory {

    /**
     * Impl of {@link RpcClientFactory#createRpcClient(ProtocolConfig)}
     *
     * @param config protocol config
     * @return common rpc client or stream client
     * @throws TRpcException create client exception
     * @see RpcClientFactory#createRpcClient(ProtocolConfig)
     */
    @Override
    public RpcClient createRpcClient(ProtocolConfig config) throws TRpcException {
        // determine whether to enable the streaming client according to the configuration
        switch (TRpcProtocolType.valueOfName(config.getProtocolType())) {
            case STREAM:
                return new TRpcStreamClient(config, TRpcStreamFrameDecoder::new);
            case STANDARD:
            default:
                break;
        }
        return new DefRpcClient(config, new StandardClientCodec());
    }

}
