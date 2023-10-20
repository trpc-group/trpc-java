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

package com.tencent.trpc.proto.standard.server;

import com.tencent.trpc.core.common.TRpcProtocolType;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.rpc.AbstractRpcServerFactory;
import com.tencent.trpc.core.rpc.RpcServer;
import com.tencent.trpc.proto.standard.common.StandardServerCodec;
import com.tencent.trpc.proto.standard.stream.codec.TRpcStreamFrameDecoder;
import com.tencent.trpc.proto.standard.stream.server.TRpcStreamServer;
import com.tencent.trpc.proto.support.DefRpcServer;

/**
 * Standard RPC server factory, used to create trpc protocol RPC Server. According to the specific protocol type,
 * the corresponding service implementation will be created. Protocol types include standard trpc protocol and
 * streaming trpc protocol.
 */
@Extension("trpc")
public class StandardRpcServerFactory extends AbstractRpcServerFactory {

    @Override
    public RpcServer createRpcServer(ProtocolConfig config) throws TRpcException {
        // determine whether to enable the streaming client according to the configuration
        switch (TRpcProtocolType.valueOfName(config.getProtocolType())) {
            case STREAM:
                return new TRpcStreamServer(config, TRpcStreamFrameDecoder::new);
            case STANDARD:
            default:
                break;
        }
        return new DefRpcServer(config, new StandardServerCodec());
    }

}


