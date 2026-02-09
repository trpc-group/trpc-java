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

package com.tencent.trpc.integration.test.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.TRpcProxy;
import com.tencent.trpc.integration.test.TrpcServerApplication;
import com.tencent.trpc.integration.test.stub.EchoAPI;
import com.tencent.trpc.integration.test.stub.EchoService.EchoRequest;
import com.tencent.trpc.integration.test.stub.EchoService.EchoResponse;
import com.tencent.trpc.integration.test.stub.JpbEchoAPI;
import com.tencent.trpc.integration.test.stub.jpb.EchoRequestPojo;
import com.tencent.trpc.integration.test.stub.jpb.EchoResponsePojo;
import com.tencent.trpc.spring.annotation.TRpcClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Codec related integration tests. Related configuration files:
 * <ul>
 *     <li>application-codec.yml</li>
 *     <li>application-codec-compressor-error.yml</li>
 *     <li>application-codec-serialization-error.yml</li>
 * </ul>
 */
@ActiveProfiles("codec")
@SpringBootTest(classes = TrpcServerApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class CodecIntegrationTest {

    @TRpcClient(id = "no-compress-client")
    private EchoAPI noCompressEchoAPI;
    @TRpcClient(id = "gzip-client")
    private EchoAPI gzipEchoAPI;
    @TRpcClient(id = "snappy-client")
    private EchoAPI snappyEchoAPI;
    @TRpcClient(id = "pb-client")
    private EchoAPI pbEchoAPI;
    @TRpcClient(id = "jpb-client")
    private JpbEchoAPI jpbEchoAPI;
    @TRpcClient(id = "json-client")
    private EchoAPI jsonEchoAPI;
    @TRpcClient(id = "pb-json-client")
    private EchoAPI pbToJsonEchoAPI;
    @TRpcClient(id = "pb-jpb-client")
    private EchoAPI pbToJpbEchoAPI;
    @TRpcClient(id = "jpb-pb-client")
    private JpbEchoAPI jpbToPbEchoAPI;
    @TRpcClient(id = "jpb-json-client")
    private JpbEchoAPI jpbToJsonEchoAPI;
    @TRpcClient(id = "json-pb-client")
    private EchoAPI jsonToPbEchoAPI;
    @TRpcClient(id = "json-jpb-client")
    private EchoAPI jsonToJpbEchoAPI;

    /**
     * Test for 'none' compressor
     */
    @Test
    public void noCompressTest() {
        assertResponseOK(noCompressEchoAPI, "1"); // send request size lower than compress_min_bytes
        assertResponseOK(noCompressEchoAPI, "greater_than_compress_min_bytes");
    }

    /**
     * Test for 'gzip' compressor
     */
    @Test
    public void gzipTest() {
        assertResponseOK(gzipEchoAPI, "1");
        assertResponseOK(gzipEchoAPI, "greater_than_compress_min_bytes");
    }

    /**
     * Test for 'snappy' compressor
     */
    @Test
    public void snappyTest() {
        assertResponseOK(snappyEchoAPI, "1");
        assertResponseOK(snappyEchoAPI, "greater_than_compress_min_bytes");
    }

    @Test
    public void testNonExistClientCompressor() {
        assertThrows(Exception.class, () -> {
            TRpcProxy.getProxy("illegal-compressor-client", EchoAPI.class)
                    .echo(new RpcClientContext(), EchoRequest.newBuilder().setMessage("test").build());
        });
    }

    /**
     * Test for 'pb' serialization
     */
    @Test
    public void testPbSerialization() {
        assertResponseOK(pbEchoAPI, "hello");
    }

    /**
     * Test for 'jpb' serialization
     */
    @Test
    public void testJpbSerialization() {
        assertResponseOK(jpbEchoAPI, "hello");
    }

    /**
     * Test for 'json' serialization
     */
    @Test
    public void testJsonSerialization() {
        assertResponseOK(jsonEchoAPI, "hello");
    }

    /**
     * Test for 'pb' serialization client against 'json' serialization server
     */
    @Test
    public void testPbClientToJsonServer() {
        assertResponseOK(pbToJsonEchoAPI, "hello");
    }

    /**
     * Test for 'pb' serialization client against 'jpb' serialization server
     */
    @Test
    public void testPbClientToJpbServer() {
        assertResponseOK(pbToJpbEchoAPI, "hello");
    }

    /**
     * Test for 'jpb' serialization client against 'pb' serialization server
     */
    @Test
    public void testJpbClientToPbServer() {
        assertResponseOK(jpbToPbEchoAPI, "hello");
    }

    /**
     * Test for 'jpb' serialization client against 'json' serialization server
     */
    @Test
    public void testJpbClientToJsonServer() {
        assertResponseOK(jpbToJsonEchoAPI, "hello");
    }

    /**
     * Test for 'json' serialization client against 'pb' serialization server
     */
    @Test
    public void testJsonClientToPbServer() {
        assertResponseOK(jsonToPbEchoAPI, "hello");
    }

    /**
     * Test for 'json' serialization client against 'jpb' serialization server
     */
    @Test
    public void testJsonClientToJpbServer() {
        assertResponseOK(jsonToJpbEchoAPI, "hello");
    }

    @Test
    public void testNonExistClientSerial() {
        assertThrows(Exception.class, () -> {
            TRpcProxy.getProxy("illegal-serialization-client", EchoAPI.class)
                    .echo(new RpcClientContext(), EchoRequest.newBuilder().setMessage("test").build());
        });
    }

    /**
     * Send request to echo server and assert the same response
     */
    private void assertResponseOK(EchoAPI echoAPI, String message) {
        EchoResponse echoResponse = echoAPI.echo(new RpcClientContext(), EchoRequest.newBuilder()
                .setMessage(message)
                .build());
        assertEquals(message, echoResponse.getMessage());
    }

    /**
     * Send request to echo server and assert the same response
     */
    private void assertResponseOK(JpbEchoAPI echoAPI, String message) {
        EchoRequestPojo echoRequest = new EchoRequestPojo();
        echoRequest.setMessage(message);
        EchoResponsePojo echoResponse = echoAPI.echo(new RpcClientContext(), echoRequest);
        assertEquals(message, echoResponse.getMessage());
    }
}