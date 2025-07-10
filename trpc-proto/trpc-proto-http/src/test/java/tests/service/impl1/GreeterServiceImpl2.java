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

package tests.service.impl1;

import static com.tencent.trpc.proto.http.constant.Constant.TEST_BYTES_REQ_KEY;
import static com.tencent.trpc.proto.http.constant.Constant.TEST_BYTES_REQ_VALUE;
import static com.tencent.trpc.proto.http.constant.Constant.TEST_BYTES_RSP_KEY;
import static com.tencent.trpc.proto.http.constant.Constant.TEST_BYTES_RSP_VALUE;
import static com.tencent.trpc.proto.http.constant.Constant.TEST_MESSAGE;
import static com.tencent.trpc.proto.http.constant.Constant.TEST_RSP_MESSAGE;
import static com.tencent.trpc.proto.http.constant.Constant.TEST_STRING_REQ_KEY;
import static com.tencent.trpc.proto.http.constant.Constant.TEST_STRING_REQ_VALUE;
import static com.tencent.trpc.proto.http.constant.Constant.TEST_STRING_RSP_KEY;
import static com.tencent.trpc.proto.http.constant.Constant.TEST_STRING_RSP_VALUE;

import com.tencent.trpc.core.rpc.RpcContext;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import tests.service.GreeterService;
import tests.service.HelloRequestProtocol;
import tests.service.TestBeanConvertWithGetMethodReq;
import tests.service.TestBeanConvertWithGetMethodRsp;

public class GreeterServiceImpl2 implements GreeterService {

    @Override
    public HelloRequestProtocol.HelloResponse sayHello(RpcContext context, HelloRequestProtocol.HelloRequest request) {
        byte[] stringReqValue = (byte[]) context.getReqAttachMap().get(TEST_STRING_REQ_KEY);
        Assert.assertEquals(new String(stringReqValue, StandardCharsets.UTF_8), TEST_STRING_REQ_VALUE);

        byte[] bytesReqValue = (byte[]) context.getReqAttachMap().get(TEST_BYTES_REQ_KEY);
        Assert.assertArrayEquals(TEST_BYTES_REQ_VALUE, bytesReqValue);

        String message = request.getMessage();
        Assert.assertEquals(TEST_MESSAGE, message);

        context.getRspAttachMap().put(TEST_BYTES_RSP_KEY, TEST_BYTES_RSP_VALUE);
        context.getRspAttachMap().put(TEST_STRING_RSP_KEY, TEST_STRING_RSP_VALUE);
        return HelloRequestProtocol.HelloResponse.newBuilder().setMessage(TEST_RSP_MESSAGE).build();
    }

    @Override
    public String sayBlankHello(RpcContext context, HelloRequestProtocol.HelloRequest request) {
        return null;
    }

    @Override
    public TestBeanConvertWithGetMethodRsp sayHelloNonPbType(RpcContext context,
            TestBeanConvertWithGetMethodReq request) {
        return null;
    }
}
