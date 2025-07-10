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

package com.tencent.trpc.integration.test.stub;

import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.integration.test.stub.jpb.EchoRequestPojo;
import com.tencent.trpc.integration.test.stub.jpb.EchoResponsePojo;
import org.springframework.stereotype.Service;

@Service
public class JpbEchoAPIImpl implements JpbEchoAPI {
    @Override
    public EchoResponsePojo echo(RpcContext context, EchoRequestPojo request) {
        EchoResponsePojo response = new EchoResponsePojo();
        response.setMessage(request.getMessage());
        return response;
    }
}
