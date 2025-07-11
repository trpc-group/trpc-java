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
import static org.junit.Assert.assertTrue;

import com.tencent.trpc.core.common.Constants;
import org.junit.Test;

public class ClientConfigTest {

    /**
     * Test purpose: To test if setDefault is correct and to test the default value.
     */
    @Test
    public void testDefault() {
        ClientConfig config = new ClientConfig();
        config.setDefault();
        assertEquals(null, config.getNamespace());
        assertEquals(Constants.DEFAULT_CHARSET, config.getCharset());
        assertEquals(Constants.DEFAULT_PROTOCOL, config.getProtocol());
        assertEquals(Constants.DEFAULT_SERIALIZATION, config.getSerialization());
        assertEquals(Constants.DEFAULT_COMPRESSOR, config.getCompressor());
        assertEquals(true, config.isKeepAlive());
        assertEquals(Constants.DEFAULT_TRANSPORTER, config.getTransporter());
        assertEquals(Constants.DEFAULT_NETWORK_TYPE, config.getNetwork());
        assertEquals(16384, config.getReceiveBuffer());
        assertEquals(16384, config.getSendBuffer());
        assertEquals(180000, config.getIdleTimeout().intValue());
        assertEquals(false, config.isLazyinit());
        assertEquals(2, config.getConnsPerAddr());
        assertEquals(1000, config.getConnTimeout());
        assertEquals(true, config.isIoThreadGroupShare());
        assertEquals(Constants.DEFAULT_IO_THREADS, config.getIoThreads());
        assertEquals(1000, config.getRequestTimeout());
        assertTrue(config.isSetDefault());
    }

    /**
     * Test purpose: To test that setDefault will fail validation when backendConfig is empty.
     */
    @Test
    public void checkBackendEmpty() {
        // 测试backend为空值
        ClientConfig config = new ClientConfig();
        config.getBackendConfigMap().put("", new BackendConfig());
        Exception exResult = null;
        try {
            config.setDefault();
        } catch (Exception ex) {
            exResult = ex;
        }
        assertTrue(exResult != null && exResult instanceof IllegalArgumentException);
        // 测试backend为空值
        ClientConfig config2 = new ClientConfig();
        config.getBackendConfigMap().put("a", new BackendConfig());
        Exception exResult2 = null;
        try {
            config2.setDefault();
        } catch (Exception ex) {
            exResult2 = ex;
        }
        assertTrue(exResult != null && exResult instanceof IllegalArgumentException);
    }

    /**
     * Test purpose: To test if setDefault is correct and to test if the default value will override the scenario.
     */
    @Test
    public void testConfig() {
        ClientConfig config = new ClientConfig();
        config.setNamespace("namespace");
        config.setRequestTimeout(10);
        config.setCharset("charset");
        config.setProtocol("trpc");
        config.setSerialization("pb");
        config.setCompressor("gzip");
        config.setKeepAlive(false);
        config.setTransporter("transporter");
        config.setNetwork("network");
        config.setReceiveBuffer(30);
        config.setSendBuffer(40);
        config.setIdleTimeout(60);
        config.setLazyinit(true);
        config.setConnsPerAddr(70);
        config.setConnTimeout(80);
        config.setIoThreadGroupShare(true);
        config.setIoThreads(100);
        config.setCallerServiceName("callerServiceName");
        config.setDefault();
        assertEquals("namespace", config.getNamespace());
        assertEquals("charset", config.getCharset());
        assertEquals("trpc", config.getProtocol());
        assertEquals("pb", config.getSerialization());
        assertEquals("gzip", config.getCompressor());
        assertEquals(false, config.isKeepAlive());
        assertEquals("transporter", config.getTransporter());
        assertEquals("network", config.getNetwork());
        assertEquals(30, config.getReceiveBuffer());
        assertEquals(40, config.getSendBuffer());
        assertEquals(60, config.getIdleTimeout().intValue());
        assertEquals(true, config.isLazyinit());
        assertEquals(70, config.getConnsPerAddr());
        assertEquals(80, config.getConnTimeout());
        assertEquals(10, config.getRequestTimeout());
        assertEquals(true, config.isIoThreadGroupShare());
        assertEquals(100, config.getIoThreads());
        assertEquals("callerServiceName", config.getCallerServiceName());
        assertTrue(config.isSetDefault());
    }

    /**
     * Test purpose: To test if setting default values for config and backendConfig is correct.
     */
    @Test
    public void testClientConfigEmptyBackendConfig() {
        ClientConfig config = new ClientConfig();
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("a://b");
        backendConfig.setName("name");
        config.addBackendConfig(backendConfig);
        config.setDefault();
        assertEquals(Constants.DEFAULT_CHARSET, config.getCharset());
        assertEquals(Constants.DEFAULT_PROTOCOL, config.getProtocol());
        assertEquals(Constants.DEFAULT_SERIALIZATION, config.getSerialization());
        assertEquals(Constants.DEFAULT_COMPRESSOR, config.getCompressor());
        assertEquals(true, config.isKeepAlive());
        assertEquals(Constants.DEFAULT_TRANSPORTER, config.getTransporter());
        assertEquals(Constants.DEFAULT_NETWORK_TYPE, config.getNetwork());
        assertEquals(16384, config.getReceiveBuffer());
        assertEquals(16384, config.getSendBuffer());
        assertEquals(180000, config.getIdleTimeout().intValue());
        assertEquals(false, config.isLazyinit());
        assertEquals(2, config.getConnsPerAddr());
        assertEquals(1000, config.getConnTimeout());
        assertEquals(1000, config.getRequestTimeout());
        assertTrue(config.isSetDefault());
        assertEquals(null, config.getNamespace());
        assertEquals(Constants.DEFAULT_CHARSET, backendConfig.getCharset());
        assertEquals(Constants.DEFAULT_PROTOCOL, backendConfig.getProtocol());
        assertEquals(Constants.DEFAULT_SERIALIZATION, backendConfig.getSerialization());
        assertEquals(Constants.DEFAULT_COMPRESSOR, backendConfig.getCompressor());
        assertEquals(true, backendConfig.isKeepAlive());
        assertEquals(Constants.DEFAULT_TRANSPORTER, backendConfig.getTransporter());
        assertEquals(Constants.DEFAULT_NETWORK_TYPE, backendConfig.getNetwork());
        assertEquals(16384, backendConfig.getReceiveBuffer());
        assertEquals(16384, backendConfig.getSendBuffer());
        assertEquals(180000, backendConfig.getIdleTimeout().intValue());
        assertEquals(false, backendConfig.isLazyinit());
        assertEquals(2, backendConfig.getConnsPerAddr());
        assertEquals(1000, backendConfig.getConnTimeout());
        assertEquals(1000, backendConfig.getRequestTimeout());
        assertTrue(backendConfig.isSetDefault());
    }

    /**
     * To test if the default values set for config are overridden and to test
     * if the default value for backendConfig is correct.
     */
    @Test
    public void testClientConfigNotEmptryBackendConfig() {
        ClientConfig config = new ClientConfig();
        config.setNamespace("namespace");
        config.setRequestTimeout(10);
        config.setCharset("charset");
        config.setProtocol("trpc");
        config.setSerialization("pb");
        config.setCompressor("gzip");
        config.setKeepAlive(false);
        config.setTransporter("transporter");
        config.setNetwork("network");
        config.setReceiveBuffer(30);
        config.setSendBuffer(40);
        config.setIdleTimeout(60);
        config.setLazyinit(true);
        config.setConnsPerAddr(70);
        config.setConnTimeout(80);
        config.setIoThreadGroupShare(true);
        config.setIoThreads(100);
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setName("name");
        backendConfig.setNamingUrl("a://b");
        config.addBackendConfig(backendConfig);
        config.setIoThreadGroupShare(false);
        config.setIoThreads(1000);
        config.setDefault();
        assertEquals("namespace", config.getNamespace());
        assertEquals("trpc", config.getProtocol());
        assertEquals("pb", config.getSerialization());
        assertEquals("gzip", config.getCompressor());
        assertEquals(false, config.isKeepAlive());
        assertEquals("transporter", config.getTransporter());
        assertEquals("network", config.getNetwork());
        assertEquals(30, config.getReceiveBuffer());
        assertEquals(40, config.getSendBuffer());
        assertEquals(60, config.getIdleTimeout().intValue());
        assertEquals(true, config.isLazyinit());
        assertEquals(70, config.getConnsPerAddr());
        assertEquals(80, config.getConnTimeout());
        assertEquals(10, config.getRequestTimeout());
        assertEquals("charset", config.getCharset());
        assertEquals("namespace", backendConfig.getNamespace());
        assertEquals("charset", backendConfig.getCharset());
        assertEquals("trpc", backendConfig.getProtocol());
        assertEquals("pb", backendConfig.getSerialization());
        assertEquals("gzip", backendConfig.getCompressor());
        assertEquals(false, backendConfig.isKeepAlive());
        assertEquals("transporter", backendConfig.getTransporter());
        assertEquals("network", backendConfig.getNetwork());
        assertEquals(30, backendConfig.getReceiveBuffer());
        assertEquals(40, backendConfig.getSendBuffer());
        assertEquals(60, backendConfig.getIdleTimeout().intValue());
        assertEquals(true, backendConfig.isLazyinit());
        assertEquals(70, backendConfig.getConnsPerAddr());
        assertEquals(80, backendConfig.getConnTimeout());
        assertEquals(10, backendConfig.getRequestTimeout());
        assertEquals(false, backendConfig.isIoThreadGroupShare());
        assertEquals(1000, backendConfig.getIoThreads());
    }
}