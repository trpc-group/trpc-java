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

package com.tencent.trpc.core.common.config;

import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BaseProtocolConfigTest {

    private BaseProtocolConfig bpc;

    /**
     * Init BaseProtocolConfig
     */
    @BeforeEach
    public void setUp() {
        this.bpc = new BaseProtocolConfig();
        bpc.setProtocol("trpc");
        bpc.setSerialization("json");
        bpc.setCompressor("gzip");
        bpc.setCompressMinBytes(10);
        bpc.setBacklog(10);
        bpc.setBatchDecoder(true);
        bpc.setCharset("utf8");
        bpc.setConnsPerAddr(10);
        bpc.setConnTimeout(10);
        bpc.setExplicitFlushAfterFlushes(10);
        bpc.setExtMap(new HashMap<>());
        bpc.setFlushConsolidation(true);
        bpc.setIdleTimeout(10);
        bpc.setTransporter("netty");
        bpc.setSendBuffer(10);
        bpc.setReceiveBuffer(10);
        bpc.setPayload(10);
        bpc.setReusePort(true);
        bpc.setNetwork("tcp");
        bpc.setMaxConns(10);
        bpc.setLazyinit(true);
        bpc.setKeepAlive(true);
        bpc.setIoThreads(10);
        bpc.setIoThreadGroupShare(true);
        bpc.setIoMode("epoll");
    }

    @Test
    public void testGetProtocol() {
        Assertions.assertEquals("trpc", bpc.getProtocol());
    }

    @Test
    public void testSetProtocol() {
        bpc.setProtocol("http");
        Assertions.assertEquals("http", bpc.getProtocol());
    }

    @Test
    public void testGetSerialization() {
        Assertions.assertEquals("json", bpc.getSerialization());
    }

    @Test
    public void testSetSerialization() {
        bpc.setSerialization("pb");
        Assertions.assertEquals("pb", bpc.getSerialization());
    }

    @Test
    public void testGetCompressor() {
        Assertions.assertEquals("gzip", bpc.getCompressor());
    }

    @Test
    public void testSetCompressor() {
        bpc.setCompressor("snappy");
        Assertions.assertEquals("snappy", bpc.getCompressor());
    }

    @Test
    public void testGetCompressMinBytes() {
        Assertions.assertEquals(10, bpc.getCompressMinBytes());
    }

    @Test
    public void testSetCompressMinBytes() {
        bpc.setCompressMinBytes(20);
        Assertions.assertEquals(20, bpc.getCompressMinBytes());
    }

    @Test
    public void testGetKeepAlive() {
        Assertions.assertTrue(bpc.getKeepAlive());
    }

    @Test
    public void testSetKeepAlive() {
        bpc.setKeepAlive(false);
        Assertions.assertFalse(bpc.getKeepAlive());
    }

    @Test
    public void testGetCharset() {
        Assertions.assertEquals("utf8", bpc.getCharset());
    }

    @Test
    public void testSetCharset() {
        bpc.setCharset("gbk");
        Assertions.assertEquals("gbk", bpc.getCharset());
    }

    @Test
    public void testGetTransporter() {
        Assertions.assertEquals("netty", bpc.getTransporter());
    }

    @Test
    public void testSetTransporter() {
        bpc.setTransporter("jetty");
        Assertions.assertEquals("jetty", bpc.getTransporter());
    }

    @Test
    public void testGetMaxConns() {
        Assertions.assertEquals(10, bpc.getMaxConns());
    }

    @Test
    public void testSetMaxConns() {
        bpc.setMaxConns(20);
        Assertions.assertEquals(20, bpc.getMaxConns());
    }

    @Test
    public void testGetBacklog() {
        Assertions.assertEquals(10, bpc.getBacklog());
    }

    @Test
    public void testSetBacklog() {
        bpc.setBacklog(20);
        Assertions.assertEquals(20, bpc.getBacklog());
    }

    @Test
    public void testGetNetwork() {
        Assertions.assertEquals("tcp", bpc.getNetwork());
    }

    @Test
    public void testSetNetwork() {
        bpc.setNetwork("udp");
        Assertions.assertEquals("udp", bpc.getNetwork());
    }

    @Test
    public void testGetReceiveBuffer() {
        Assertions.assertEquals(10, bpc.getReceiveBuffer());
    }

    @Test
    public void testSetReceiveBuffer() {
        bpc.setReceiveBuffer(20);
        Assertions.assertEquals(20, bpc.getReceiveBuffer());
    }

    @Test
    public void testGetSendBuffer() {
        Assertions.assertEquals(10, bpc.getSendBuffer());
    }

    @Test
    public void testSetSendBuffer() {
        bpc.setSendBuffer(20);
        Assertions.assertEquals(20, bpc.getSendBuffer());
    }

    @Test
    public void testGetPayload() {
        Assertions.assertEquals(10, bpc.getPayload());
    }

    @Test
    public void testSetPayload() {
        bpc.setPayload(20);
        Assertions.assertEquals(20, bpc.getPayload());
    }

    @Test
    public void testGetIdleTimeout() {
        Assertions.assertEquals(10, bpc.getIdleTimeout().intValue());
    }

    @Test
    public void testSetIdleTimeout() {
        bpc.setIdleTimeout(20);
        Assertions.assertEquals(20, bpc.getIdleTimeout().intValue());
    }

    @Test
    public void testGetLazyinit() {
        Assertions.assertTrue(bpc.getLazyinit());
    }

    @Test
    public void testSetLazyinit() {
        bpc.setLazyinit(false);
        Assertions.assertFalse(bpc.getLazyinit());
    }

    @Test
    public void testGetConnsPerAddr() {
        Assertions.assertEquals(10, bpc.getConnsPerAddr());
    }

    @Test
    public void testSetConnsPerAddr() {
        bpc.setConnsPerAddr(20);
        Assertions.assertEquals(20, bpc.getConnsPerAddr());
    }

    @Test
    public void testGetConnTimeout() {
        Assertions.assertEquals(10, bpc.getConnTimeout());
    }

    @Test
    public void testSetConnTimeout() {
        bpc.setConnTimeout(20);
        Assertions.assertEquals(20, bpc.getConnTimeout());
    }

    @Test
    public void testGetIoMode() {
        Assertions.assertEquals("epoll", bpc.getIoMode());
    }

    @Test
    public void testSetIoMode() {
        bpc.setIoMode("poll");
        Assertions.assertEquals("poll", bpc.getIoMode());
    }

    @Test
    public void testGetIoThreadGroupShare() {
        Assertions.assertTrue(bpc.getIoThreadGroupShare());
    }

    @Test
    public void testSetIoThreadGroupShare() {
        bpc.setIoThreadGroupShare(false);
        Assertions.assertFalse(bpc.getIoThreadGroupShare());
    }

    @Test
    public void testGetIoThreads() {
        Assertions.assertEquals(10, bpc.getIoThreads());
    }

    @Test
    public void testSetIoThreads() {
        bpc.setIoThreads(20);
        Assertions.assertEquals(20, bpc.getIoThreads());
    }

    @Test
    public void testSetBossThreads() {
        bpc.setBossThreads(2);
        Assertions.assertEquals(2, bpc.getBossThreads());
    }

    @Test
    public void testGetFlushConsolidation() {
        Assertions.assertTrue(bpc.getFlushConsolidation());
    }

    @Test
    public void testSetFlushConsolidation() {
        bpc.setFlushConsolidation(false);
        Assertions.assertFalse(bpc.getFlushConsolidation());
    }

    @Test
    public void testGetBatchDecoder() {
        Assertions.assertTrue(bpc.getBatchDecoder());
    }

    @Test
    public void testSetBatchDecoder() {
        bpc.setBatchDecoder(false);
        Assertions.assertFalse(bpc.getBatchDecoder());
    }

    @Test
    public void testGetExplicitFlushAfterFlushes() {
        Assertions.assertEquals(10, bpc.getExplicitFlushAfterFlushes());
    }

    @Test
    public void testSetExplicitFlushAfterFlushes() {
        bpc.setExplicitFlushAfterFlushes(20);
        Assertions.assertEquals(20, bpc.getExplicitFlushAfterFlushes());
    }

    @Test
    public void testGetReusePort() {
        Assertions.assertTrue(bpc.getReusePort());
    }

    @Test
    public void testSetReusePort() {
        bpc.setReusePort(false);
        Assertions.assertFalse(bpc.getReusePort());
    }

    @Test
    public void testGetExtMap() {
        Assertions.assertEquals(0, bpc.getExtMap().size());
    }

    @Test
    public void testSetExtMap() {
        bpc.setExtMap(null);
        Assertions.assertNull(bpc.getExtMap());
    }

}
