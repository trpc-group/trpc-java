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

package com.tencent.trpc.integration.test.stub;

import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.integration.test.stub.EchoService.DelayedEchoRequest;
import com.tencent.trpc.integration.test.stub.EchoService.EchoRequest;
import com.tencent.trpc.integration.test.stub.EchoService.EchoResponse;
import org.springframework.stereotype.Service;

@Service
public class EchoAPIImpl implements EchoAPI {
    @Override
    public EchoResponse echo(RpcContext context, EchoRequest request) {
        return EchoResponse.newBuilder().setMessage(request.getMessage()).build();
    }

    @Override
    public EchoResponse delayedEcho(RpcContext context, DelayedEchoRequest request) {
        try {
            Thread.sleep(request.getDelaySeconds() * 1000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return EchoResponse.newBuilder().setMessage(request.getMessage()).build();
    }
}
