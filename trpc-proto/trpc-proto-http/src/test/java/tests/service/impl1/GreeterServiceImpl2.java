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

package tests.service.impl1;

import com.tencent.trpc.core.rpc.RpcContext;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import tests.service.GreeterService;
import tests.service.HelloRequestProtocol;
import tests.service.TestBeanConvertWithGetMethodReq;
import tests.service.TestBeanConvertWithGetMethodRsp;

public class GreeterServiceImpl2 implements GreeterService {

    private static final String TRANSPARENT_TRANSMISSION_KEY = "name";
    private static final String TRANSPARENT_TRANSMISSION_VALUE = "zhangsan";

    private static final String TRANSPARENT_TRANSMISSION_RES_KEY = "code";
    private static final String TRANSPARENT_TRANSMISSION_RES_VALUE = "2012";

    @Override
    public HelloRequestProtocol.HelloResponse sayHello(RpcContext context, HelloRequestProtocol.HelloRequest request) {
        byte[] name = (byte[]) context.getReqAttachMap().get(TRANSPARENT_TRANSMISSION_KEY);
        String str = new String(name, StandardCharsets.UTF_8);

        Assert.assertEquals(str, TRANSPARENT_TRANSMISSION_VALUE);
        String message = request.getMessage();

        context.getRspAttachMap().put(TRANSPARENT_TRANSMISSION_RES_KEY, TRANSPARENT_TRANSMISSION_RES_VALUE);
        return HelloRequestProtocol.HelloResponse.newBuilder().setMessage("hello " + str).build();
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
