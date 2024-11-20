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

package com.tencent.trpc.integration.test.transport.slf4j;

import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.integration.test.TrpcServerApplication;
import com.tencent.trpc.integration.test.stub.EchoAPI;
import com.tencent.trpc.integration.test.stub.EchoService;
import com.tencent.trpc.spring.annotation.TRpcClient;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.TrpcMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TrpcServerApplication.class)
@ActiveProfiles("transport")
public class TrpcMDCAdapterTest {

    @TRpcClient(id = "http-client-7072")
    private EchoAPI http7072EchoAPI;

    @TRpcClient(id = "http-client-7073")
    private EchoAPI http7073EchoAPI;

    /**
     * Test for testTrpcMDCAdapter
     */
    @Test
    public void testTrpcMDCAdapter() {

            String message = "http-hello";
            EchoService.EchoResponse response = http7072EchoAPI.echo(new RpcClientContext(), EchoService.EchoRequest.newBuilder()
                    .setMessage(message)
                    .build());
            assertEquals(message, response.getMessage());
            response = http7073EchoAPI.echo(new RpcClientContext(), EchoService.EchoRequest.newBuilder()
                    .setMessage(message)
                    .build());
            assertEquals(message, response.getMessage());
    }
}
