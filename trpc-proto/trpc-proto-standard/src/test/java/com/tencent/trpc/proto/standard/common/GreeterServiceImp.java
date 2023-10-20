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
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.utils.Charsets;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol.HelloResponse;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.StringUtils;

public class GreeterServiceImp implements GreeterService {

    public static final Logger LOGGER = LoggerFactory.getLogger(GreeterServiceImp.class);

    @Override
    public HelloResponse sayHello(RpcContext context, HelloRequest request) {
        HelloResponse.Builder response = HelloResponse.newBuilder();
        String msg = request.getMessage().toStringUtf8();
        if (msg.contains("normal")) {
            response.setMessage(ByteString.copyFromUtf8(msg));
        } else if (msg.contains("alias")) {
            CallInfo callInfo = context.getCallInfo();
            response.setMessage(ByteString.copyFromUtf8(callInfo.getCalleeMethod()));
        } else if ("sysexception".equals(msg)) {
            throw TRpcException.newFrameException(ErrorCode.TRPC_INVOKE_UNKNOWN_ERR, "innererror");
        } else if ("bizexception".equals(msg)) {
            throw TRpcException.newBizException(88, "paramserror");
        } else if (msg.contains("timeout")) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            response.setMessage(ByteString.copyFromUtf8(msg));
        } else if (msg.equals("attachment")) {
            byte[] requestAttachment = context.getRequestUncodecDataSegment();
            System.out.println("requestAttachment: " + StringUtils.newStringUtf8(requestAttachment));
            context.setResponseUncodecDataSegment("responseAttachment".getBytes(StandardCharsets.UTF_8));
            response.setMessage(ByteString.copyFromUtf8(msg));
        } else if (msg.equals("requestAttachment")) {
            byte[] requestAttachment = context.getRequestUncodecDataSegment();
            System.out.println("requestAttachment: " + StringUtils.newStringUtf8(requestAttachment));
            response.setMessage(ByteString.copyFromUtf8(msg));
        }
        if (context.getReqAttachMap().get("key") != null) {
            context.getRspAttachMap().put("key",
                    (new String((byte[]) (context.getReqAttachMap().get("key")), Charsets.UTF_8)
                            + "-abc")
                            .getBytes());
        }
        System.out.println(">>>>>>>>>>>>map:" + context.getReqAttachMap());
        return response.build();
    }
}
