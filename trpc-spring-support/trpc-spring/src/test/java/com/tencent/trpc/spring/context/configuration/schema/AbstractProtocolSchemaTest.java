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

package com.tencent.trpc.spring.context.configuration.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Maps;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class AbstractProtocolSchemaTest {

    /**
     * 使用匿名子类实例化抽象类
     */
    private AbstractProtocolSchema schema;

    @Before
    public void setUp() {
        schema = new AbstractProtocolSchema() {
        };
    }

    @Test
    public void testProtocol() {
        schema.setProtocol("trpc");
        assertEquals("trpc", schema.getProtocol());
    }

    @Test
    public void testProtocolType() {
        schema.setProtocolType("stream");
        assertEquals("stream", schema.getProtocolType());
    }

    @Test
    public void testSerialization() {
        schema.setSerialization("pb");
        assertEquals("pb", schema.getSerialization());
    }

    @Test
    public void testCompressor() {
        schema.setCompressor("gzip");
        assertEquals("gzip", schema.getCompressor());
    }

    @Test
    public void testCompressMinBytes() {
        schema.setCompressMinBytes(1024);
        assertEquals(Integer.valueOf(1024), schema.getCompressMinBytes());
    }

    @Test
    public void testSign() {
        schema.setSign("hmac");
        assertEquals("hmac", schema.getSign());
    }

    @Test
    public void testKeepAlive() {
        schema.setKeepAlive(Boolean.TRUE);
        assertEquals(Boolean.TRUE, schema.getKeepAlive());

        schema.setKeepAlive(Boolean.FALSE);
        assertEquals(Boolean.FALSE, schema.getKeepAlive());
    }

    @Test
    public void testCharset() {
        schema.setCharset("UTF-8");
        assertEquals("UTF-8", schema.getCharset());
    }

    @Test
    public void testTransporter() {
        schema.setTransporter("netty");
        assertEquals("netty", schema.getTransporter());
    }

    @Test
    public void testMaxConns() {
        schema.setMaxConns(100);
        assertEquals(Integer.valueOf(100), schema.getMaxConns());
    }

    @Test
    public void testBacklog() {
        schema.setBacklog(512);
        assertEquals(Integer.valueOf(512), schema.getBacklog());
    }

    @Test
    public void testNetwork() {
        schema.setNetwork("tcp");
        assertEquals("tcp", schema.getNetwork());
    }

    @Test
    public void testReceiveBuffer() {
        schema.setReceiveBuffer(8192);
        assertEquals(Integer.valueOf(8192), schema.getReceiveBuffer());
    }

    @Test
    public void testSendBuffer() {
        schema.setSendBuffer(4096);
        assertEquals(Integer.valueOf(4096), schema.getSendBuffer());
    }

    @Test
    public void testPayload() {
        schema.setPayload(10485760);
        assertEquals(Integer.valueOf(10485760), schema.getPayload());
    }

    @Test
    public void testIdleTimeout() {
        schema.setIdleTimeout(180000);
        assertEquals(Integer.valueOf(180000), schema.getIdleTimeout());
    }

    @Test
    public void testLazyinit() {
        schema.setLazyinit(Boolean.TRUE);
        assertEquals(Boolean.TRUE, schema.getLazyinit());

        schema.setLazyinit(Boolean.FALSE);
        assertEquals(Boolean.FALSE, schema.getLazyinit());
    }

    @Test
    public void testIoThreadGroupShare() {
        schema.setIoThreadGroupShare(Boolean.TRUE);
        assertEquals(Boolean.TRUE, schema.getIoThreadGroupShare());

        schema.setIoThreadGroupShare(Boolean.FALSE);
        assertEquals(Boolean.FALSE, schema.getIoThreadGroupShare());
    }

    @Test
    public void testIoThreads() {
        schema.setIoThreads(8);
        assertEquals(Integer.valueOf(8), schema.getIoThreads());
    }

    @Test
    public void testFlushConsolidation() {
        schema.setFlushConsolidation(Boolean.TRUE);
        assertEquals(Boolean.TRUE, schema.getFlushConsolidation());

        schema.setFlushConsolidation(Boolean.FALSE);
        assertEquals(Boolean.FALSE, schema.getFlushConsolidation());
    }

    @Test
    public void testBatchDecoder() {
        schema.setBatchDecoder(Boolean.TRUE);
        assertEquals(Boolean.TRUE, schema.getBatchDecoder());

        schema.setBatchDecoder(Boolean.FALSE);
        assertEquals(Boolean.FALSE, schema.getBatchDecoder());
    }

    @Test
    public void testExplicitFlushAfterFlushes() {
        schema.setExplicitFlushAfterFlushes(256);
        assertEquals(Integer.valueOf(256), schema.getExplicitFlushAfterFlushes());
    }

    @Test
    public void testAddress() {
        schema.setAddress("127.0.0.1:9092?topics=quickstart-events&group=quickstart-group");
        assertEquals("127.0.0.1:9092?topics=quickstart-events&group=quickstart-group",
                schema.getAddress());
    }

    @Test
    public void testExtMap() {
        // 默认 extMap 不为 null
        assertNotNull(schema.getExtMap());

        Map<String, Object> extMap = Maps.newHashMap();
        extMap.put("key1", "value1");
        extMap.put("key2", 42);
        schema.setExtMap(extMap);

        assertEquals(2, schema.getExtMap().size());
        assertEquals("value1", schema.getExtMap().get("key1"));
        assertEquals(42, schema.getExtMap().get("key2"));
    }

    @Test
    public void testDefaultExtMapNotNull() {
        // 新建实例时 extMap 默认初始化为空 Map，不为 null
        AbstractProtocolSchema newSchema = new AbstractProtocolSchema() {
        };
        assertNotNull(newSchema.getExtMap());
        assertTrue(newSchema.getExtMap().isEmpty());
    }
}
