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

package com.tencent.trpc.core.common.config;

import java.util.HashMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BaseProtocolConfigTest {

    private BaseProtocolConfig bpc;

    /**
     * Init BaseProtocolConfig
     */
    @Before
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
        Assert.assertEquals("trpc", bpc.getProtocol());
    }

    @Test
    public void testSetProtocol() {
        bpc.setProtocol("http");
        Assert.assertEquals("http", bpc.getProtocol());
    }

    @Test
    public void testGetSerialization() {
        Assert.assertEquals("json", bpc.getSerialization());
    }

    @Test
    public void testSetSerialization() {
        bpc.setSerialization("pb");
        Assert.assertEquals("pb", bpc.getSerialization());
    }

    @Test
    public void testGetCompressor() {
        Assert.assertEquals("gzip", bpc.getCompressor());
    }

    @Test
    public void testSetCompressor() {
        bpc.setCompressor("snappy");
        Assert.assertEquals("snappy", bpc.getCompressor());
    }

    @Test
    public void testGetCompressMinBytes() {
        Assert.assertEquals(10, bpc.getCompressMinBytes());
    }

    @Test
    public void testSetCompressMinBytes() {
        bpc.setCompressMinBytes(20);
        Assert.assertEquals(20, bpc.getCompressMinBytes());
    }

    @Test
    public void testGetKeepAlive() {
        Assert.assertTrue(bpc.getKeepAlive());
    }

    @Test
    public void testSetKeepAlive() {
        bpc.setKeepAlive(false);
        Assert.assertFalse(bpc.getKeepAlive());
    }

    @Test
    public void testGetCharset() {
        Assert.assertEquals("utf8", bpc.getCharset());
    }

    @Test
    public void testSetCharset() {
        bpc.setCharset("gbk");
        Assert.assertEquals("gbk", bpc.getCharset());
    }

    @Test
    public void testGetTransporter() {
        Assert.assertEquals("netty", bpc.getTransporter());
    }

    @Test
    public void testSetTransporter() {
        bpc.setTransporter("jetty");
        Assert.assertEquals("jetty", bpc.getTransporter());
    }

    @Test
    public void testGetMaxConns() {
        Assert.assertEquals(10, bpc.getMaxConns());
    }

    @Test
    public void testSetMaxConns() {
        bpc.setMaxConns(20);
        Assert.assertEquals(20, bpc.getMaxConns());
    }

    @Test
    public void testGetBacklog() {
        Assert.assertEquals(10, bpc.getBacklog());
    }

    @Test
    public void testSetBacklog() {
        bpc.setBacklog(20);
        Assert.assertEquals(20, bpc.getBacklog());
    }

    @Test
    public void testGetNetwork() {
        Assert.assertEquals("tcp", bpc.getNetwork());
    }

    @Test
    public void testSetNetwork() {
        bpc.setNetwork("udp");
        Assert.assertEquals("udp", bpc.getNetwork());
    }

    @Test
    public void testGetReceiveBuffer() {
        Assert.assertEquals(10, bpc.getReceiveBuffer());
    }

    @Test
    public void testSetReceiveBuffer() {
        bpc.setReceiveBuffer(20);
        Assert.assertEquals(20, bpc.getReceiveBuffer());
    }

    @Test
    public void testGetSendBuffer() {
        Assert.assertEquals(10, bpc.getSendBuffer());
    }

    @Test
    public void testSetSendBuffer() {
        bpc.setSendBuffer(20);
        Assert.assertEquals(20, bpc.getSendBuffer());
    }

    @Test
    public void testGetPayload() {
        Assert.assertEquals(10, bpc.getPayload());
    }

    @Test
    public void testSetPayload() {
        bpc.setPayload(20);
        Assert.assertEquals(20, bpc.getPayload());
    }

    @Test
    public void testGetIdleTimeout() {
        Assert.assertEquals(10, bpc.getIdleTimeout().intValue());
    }

    @Test
    public void testSetIdleTimeout() {
        bpc.setIdleTimeout(20);
        Assert.assertEquals(20, bpc.getIdleTimeout().intValue());
    }

    @Test
    public void testGetLazyinit() {
        Assert.assertTrue(bpc.getLazyinit());
    }

    @Test
    public void testSetLazyinit() {
        bpc.setLazyinit(false);
        Assert.assertFalse(bpc.getLazyinit());
    }

    @Test
    public void testGetConnsPerAddr() {
        Assert.assertEquals(10, bpc.getConnsPerAddr());
    }

    @Test
    public void testSetConnsPerAddr() {
        bpc.setConnsPerAddr(20);
        Assert.assertEquals(20, bpc.getConnsPerAddr());
    }

    @Test
    public void testGetConnTimeout() {
        Assert.assertEquals(10, bpc.getConnTimeout());
    }

    @Test
    public void testSetConnTimeout() {
        bpc.setConnTimeout(20);
        Assert.assertEquals(20, bpc.getConnTimeout());
    }

    @Test
    public void testGetIoMode() {
        Assert.assertEquals("epoll", bpc.getIoMode());
    }

    @Test
    public void testSetIoMode() {
        bpc.setIoMode("poll");
        Assert.assertEquals("poll", bpc.getIoMode());
    }

    @Test
    public void testGetIoThreadGroupShare() {
        Assert.assertTrue(bpc.getIoThreadGroupShare());
    }

    @Test
    public void testSetIoThreadGroupShare() {
        bpc.setIoThreadGroupShare(false);
        Assert.assertFalse(bpc.getIoThreadGroupShare());
    }

    @Test
    public void testGetIoThreads() {
        Assert.assertEquals(10, bpc.getIoThreads());
    }

    @Test
    public void testSetIoThreads() {
        bpc.setIoThreads(20);
        Assert.assertEquals(20, bpc.getIoThreads());
    }

    @Test
    public void testSetBossThreads() {
        bpc.setBossThreads(2);
        Assert.assertEquals(2, bpc.getBossThreads());
    }

    @Test
    public void testGetFlushConsolidation() {
        Assert.assertTrue(bpc.getFlushConsolidation());
    }

    @Test
    public void testSetFlushConsolidation() {
        bpc.setFlushConsolidation(false);
        Assert.assertFalse(bpc.getFlushConsolidation());
    }

    @Test
    public void testGetBatchDecoder() {
        Assert.assertTrue(bpc.getBatchDecoder());
    }

    @Test
    public void testSetBatchDecoder() {
        bpc.setBatchDecoder(false);
        Assert.assertFalse(bpc.getBatchDecoder());
    }

    @Test
    public void testGetExplicitFlushAfterFlushes() {
        Assert.assertEquals(10, bpc.getExplicitFlushAfterFlushes());
    }

    @Test
    public void testSetExplicitFlushAfterFlushes() {
        bpc.setExplicitFlushAfterFlushes(20);
        Assert.assertEquals(20, bpc.getExplicitFlushAfterFlushes());
    }

    @Test
    public void testGetReusePort() {
        Assert.assertTrue(bpc.getReusePort());
    }

    @Test
    public void testSetReusePort() {
        bpc.setReusePort(false);
        Assert.assertFalse(bpc.getReusePort());
    }

    @Test
    public void testGetExtMap() {
        Assert.assertEquals(0, bpc.getExtMap().size());
    }

    @Test
    public void testSetExtMap() {
        bpc.setExtMap(null);
        Assert.assertNull(bpc.getExtMap());
    }

}
