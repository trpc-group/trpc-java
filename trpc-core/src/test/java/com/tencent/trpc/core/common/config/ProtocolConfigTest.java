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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.tencent.trpc.core.common.Constants;
import org.junit.Test;

public class ProtocolConfigTest {

    @Test
    public void testDefaultConfig() {
        ProtocolConfig config = new ProtocolConfig();
        config.setIp("127.1.1.1");
        config.setPort(80);
        config.setDefault();
        assertEquals("127.1.1.1", config.getIp());
        assertEquals("127.1.1.1:80:tcp", config.toUniqId());
        assertEquals(null, config.getName());
        assertEquals(80, config.getPort());
        assertEquals(null, config.getNic());
        assertEquals(Constants.DEFAULT_PROTOCOL, config.getProtocol());
        assertEquals(Constants.DEFAULT_SERIALIZATION, config.getSerialization());
        assertEquals(Constants.DEFAULT_COMPRESSOR, config.getCompressor());
        assertEquals(true, config.isKeepAlive());
        assertEquals(Constants.DEFAULT_CHARSET, config.getCharset());
        assertEquals(Constants.DEFAULT_TRANSPORTER, config.getTransporter());
        assertEquals(20480, config.getMaxConns());
        assertEquals(1024, config.getBacklog());
        assertEquals(Constants.DEFAULT_NETWORK_TYPE, config.getNetwork());
        assertEquals(16384, config.getReceiveBuffer());
        assertEquals(16384, config.getSendBuffer());
        assertEquals(10485760, config.getPayload());
        assertEquals(180000, config.getIdleTimeout().intValue());
        assertEquals(false, config.isLazyinit());
        assertEquals(2, config.getConnsPerAddr());
        assertEquals(1000, config.getConnTimeout());
        assertEquals(Constants.DEFAULT_IO_MODE, config.getIoMode());
        assertEquals(Boolean.TRUE, config.isIoThreadGroupShare());
        assertEquals(Constants.DEFAULT_IO_THREADS, config.getIoThreads());
        assertEquals(0, config.getExtMap().size());
        assertNotNull(config.clone());
        assertNotNull(ProtocolConfig.newInstance());
    }

    @Test
    public void testConfig() {
        ProtocolConfig config = new ProtocolConfig();
        config.setIp("127.1.1.1");
        config.setName("");
        config.setPort(8080);
        config.setNic("nic");
        config.setProtocol("trpc");
        config.setSerialization("pb");
        config.setCompressor("gzip");
        config.setKeepAlive(false);
        config.setCharset("charset");
        config.setTransporter("transporter");
        config.setMaxConns(10);
        config.setBacklog(20);
        config.setNetwork("network");
        config.setReceiveBuffer(30);
        config.setSendBuffer(40);
        config.setPayload(50);
        config.setIdleTimeout(60);
        config.setLazyinit(true);
        config.setConnsPerAddr(70);
        config.setConnTimeout(80);
        config.setIoMode("mode");
        config.setIoThreadGroupShare(false);
        config.setIoThreads(90);
        config.setBossThreads(2);
        config.setExtMap(ImmutableMap.of("a", "a1"));
        config.setFlushConsolidation(true);
        config.setBatchDecoder(true);
        config.setExplicitFlushAfterFlushes(1024);
        config.setCompressMinBytes(10);
        config.setDefault();
        assertEquals("127.1.1.1", config.getIp());
        assertEquals("", config.getName());
        assertEquals(8080, config.getPort());
        assertEquals("nic", config.getNic());
        assertEquals("trpc", config.getProtocol());
        assertEquals("pb", config.getSerialization());
        assertEquals("gzip", config.getCompressor());
        assertEquals(false, config.isKeepAlive());
        assertEquals(false, config.getKeepAlive());
        assertEquals("charset", config.getCharset());
        assertEquals("transporter", config.getTransporter());
        assertEquals(10, config.getMaxConns());
        assertEquals(20, config.getBacklog());
        assertEquals("network", config.getNetwork());
        assertEquals(30, config.getReceiveBuffer());
        assertEquals(40, config.getSendBuffer());
        assertEquals(50, config.getPayload());
        assertEquals(60, config.getIdleTimeout().intValue());
        assertEquals(true, config.isLazyinit());
        assertEquals(true, config.getLazyinit());
        assertEquals(70, config.getConnsPerAddr());
        assertEquals(80, config.getConnTimeout());
        assertEquals("mode", config.getIoMode());
        assertEquals(false, config.isIoThreadGroupShare());
        assertEquals(false, config.getIoThreadGroupShare());
        assertEquals(90, config.getIoThreads());
        assertEquals(2, config.getBossThreads());
        assertEquals("a1", config.getExtMap().get("a"));
        assertEquals(true, config.getFlushConsolidation());
        assertEquals(1024, config.getExplicitFlushAfterFlushes());
        assertEquals(true, config.getBatchDecoder());
        assertFalse(config.useEpoll());
        assertEquals(10, config.getCompressMinBytes());
        assertTrue(config.isSetDefault());
    }
}