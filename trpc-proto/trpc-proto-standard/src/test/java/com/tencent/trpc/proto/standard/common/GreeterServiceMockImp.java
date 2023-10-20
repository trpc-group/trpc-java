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

package com.tencent.trpc.proto.standard.common;

import com.google.protobuf.ByteString;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.utils.FutureUtils;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol.HelloResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class GreeterServiceMockImp implements GreeterClientApi {

    public static final Logger LOGGER = LoggerFactory.getLogger(GreeterServiceImp.class);

    @Override
    public CompletionStage<HelloResponse> asyncSayHello(RpcClientContext context,
            HelloRequest request) {
        CompletableFuture<HelloResponse> newFuture = FutureUtils.newFuture();
        newFuture.complete(sayHello(context, request));
        return newFuture;
    }

    @Override
    public HelloResponse sayHello(RpcClientContext context, HelloRequest request) {
        HelloResponse.Builder response = HelloResponse.newBuilder();
        String msg = request.getMessage().toStringUtf8();
        if (msg.indexOf("normalx") != -1) {
            response.setMessage(ByteString.copyFromUtf8(msg));
        } else if ("sysexception1".equals(msg)) {
            throw TRpcException.newFrameException(ErrorCode.TRPC_INVOKE_UNKNOWN_ERR, "innererror");
        } else if ("bizexception1".equals(msg)) {
            throw TRpcException.newBizException(88, "paramserror");
        } else if (msg.indexOf("timeout1") != -1) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            response.setMessage(ByteString.copyFromUtf8(msg));
        }
        if (context.getReqAttachMap().get("key1") != null) {
            context.getRspAttachMap().put("key1",
                    (String) (context.getReqAttachMap().get("key1")) + "-abc");
        }
        System.out.println(">>>>>>>>>>>>map:" + context.getReqAttachMap());
        return response.build();
    }

    @Override
    public HelloResponse sayHellox(RpcClientContext context, HelloRequest request) {
        throw new UnsupportedOperationException("not support sayHellox");
    }
}
