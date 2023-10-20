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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.registry.MockRegistry;
import com.tencent.trpc.core.rpc.GenericClient;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.junit.Test;

public class ServiceConfigTest {

    @Test
    public void testRandomPort() {
        ServiceConfig config = new ServiceConfig();
        config.setPort(-1);
        config.setIp("localhost");
        config.setDefault();
        assertNotEquals(-1, config.getPort());
    }

    @Test
    public void testDefaultConfig() {
        ServiceConfig config = new ServiceConfig();
        config.setDisableDefaultFilter(false);
        config.setName("name");
        config.setIp("127.1.1.1");
        config.setPort(80);
        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setServiceConfig(config);
        config.getProviderConfigs().add(providerConfig);
        config.setDefault();
        config.overrideConfigDefault(new ServerConfig());
        assertFalse(config.getEnableLinkTimeout());
        assertEquals("127.1.1.1", config.getIp());
        assertEquals(80, config.getPort());
        assertEquals(null, config.getNic());
        assertEquals("name", config.getName());
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
        assertEquals(240000, config.getIdleTimeout().intValue());
        assertEquals(false, config.isLazyinit());
        assertEquals(Constants.DEFAULT_IO_MODE, config.getIoMode());
        assertEquals(true, config.isIoThreadGroupShare());
        assertEquals(Constants.DEFAULT_IO_THREADS, config.getIoThreads());
        assertEquals(0, config.getExtMap().size());

        ProtocolConfig protocolConfig = (ProtocolConfig) (config.getProtocolConfig());
        assertEquals("127.1.1.1", protocolConfig.getIp());
        assertEquals(80, protocolConfig.getPort());

        assertEquals(null, protocolConfig.getNic());
        assertEquals(Constants.DEFAULT_PROTOCOL, protocolConfig.getProtocol());
        assertEquals(Constants.DEFAULT_SERIALIZATION, protocolConfig.getSerialization());
        assertEquals(Constants.DEFAULT_COMPRESSOR, protocolConfig.getCompressor());
        assertEquals(true, protocolConfig.isKeepAlive());
        assertEquals(Constants.DEFAULT_CHARSET, protocolConfig.getCharset());
        assertEquals(Constants.DEFAULT_TRANSPORTER, protocolConfig.getTransporter());
        assertEquals(20480, protocolConfig.getMaxConns());
        assertEquals(1024, protocolConfig.getBacklog());
        assertEquals(Constants.DEFAULT_NETWORK_TYPE, protocolConfig.getNetwork());
        assertEquals(16384, protocolConfig.getReceiveBuffer());
        assertEquals(16384, protocolConfig.getSendBuffer());
        assertEquals(10485760, protocolConfig.getPayload());
        assertEquals(240000, protocolConfig.getIdleTimeout().intValue());
        assertEquals(false, protocolConfig.isLazyinit());
        assertEquals(Constants.DEFAULT_IO_MODE, protocolConfig.getIoMode());
        assertEquals(Boolean.TRUE, protocolConfig.isIoThreadGroupShare());
        assertEquals(Constants.DEFAULT_IO_THREADS, protocolConfig.getIoThreads());
        assertEquals(0, protocolConfig.getExtMap().size());
    }

    @Test
    public void testConfig() {
        ServiceConfig config = new ServiceConfig();
        config.setName("name");
        config.setIp("127.1.1.1");
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
        config.setFlushConsolidation(true);
        config.setBatchDecoder(true);
        config.setExplicitFlushAfterFlushes(1024);
        config.setNetwork("network");
        config.setReceiveBuffer(30);
        config.setSendBuffer(40);
        config.setPayload(50);
        config.setIdleTimeout(60);
        config.setLazyinit(true);
        config.setIoMode("mode");
        config.setFilters(Lists.newArrayList("serviceConfigFilter"));
        config.setIoThreadGroupShare(false);
        config.setIoThreads(90);
        config.setBossThreads(2);
        config.setExtMap(ImmutableMap.of("a", "a1"));
        ProviderConfig providerConfig = new ProviderConfig();
        config.getProviderConfigs().add(providerConfig);
        config.setEnableLinkTimeout(Boolean.TRUE);
        config.setWorkerPool("a");
        config.setVersion("1");
        config.setGroup("b");
        config.setCompressMinBytes(10);
        config.setRegistryConfigs(new ArrayList<>());
        config.setDefault();
        assertEquals("a", config.getWorkerPool());
        assertEquals("1", config.getVersion());
        assertEquals("b", config.getGroup());
        assertFalse(config.isRegisted());
        assertTrue(config.getEnableLinkTimeout());
        assertTrue(config.getRegistryConfigs().isEmpty());
        assertTrue(config.getRegistries().isEmpty());
        assertEquals("name", config.getName());
        assertEquals("127.1.1.1", config.getIp());
        assertEquals(8080, config.getPort());
        assertEquals(10, config.getCompressMinBytes());
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
        assertEquals(true, config.getFlushConsolidation());
        assertEquals(true, config.getBatchDecoder());
        assertEquals(1024, config.getExplicitFlushAfterFlushes());
        assertEquals("network", config.getNetwork());
        assertEquals(30, config.getReceiveBuffer());
        assertEquals(40, config.getSendBuffer());
        assertEquals(50, config.getPayload());
        assertEquals(60, config.getIdleTimeout().intValue());
        assertEquals(true, config.isLazyinit());
        assertEquals(true, config.getLazyinit());
        assertEquals(true, config.isSetDefault());
        assertEquals("mode", config.getIoMode());
        assertEquals(false, config.isIoThreadGroupShare());
        assertEquals(false, config.isIoThreadGroupShare());
        assertEquals(90, config.getIoThreads());
        assertEquals(2, config.getBossThreads());
        assertEquals("a1", config.getExtMap().get("a"));
        assertEquals("serviceConfigFilter", config.getFilters().get(0));
        ProtocolConfig protocolConfig = config.getProtocolConfig();
        assertEquals("127.1.1.1", protocolConfig.getIp());
        assertEquals(8080, protocolConfig.getPort());
        assertEquals("nic", protocolConfig.getNic());
        assertEquals("trpc", protocolConfig.getProtocol());
        assertEquals("pb", protocolConfig.getSerialization());
        assertEquals("gzip", protocolConfig.getCompressor());
        assertEquals(false, protocolConfig.isKeepAlive());
        assertEquals("charset", protocolConfig.getCharset());
        assertEquals("transporter", protocolConfig.getTransporter());
        assertEquals(10, protocolConfig.getMaxConns());
        assertEquals(20, protocolConfig.getBacklog());
        assertEquals("network", protocolConfig.getNetwork());
        assertEquals(30, protocolConfig.getReceiveBuffer());
        assertEquals(40, protocolConfig.getSendBuffer());
        assertEquals(50, protocolConfig.getPayload());
        assertEquals(60, protocolConfig.getIdleTimeout().intValue());
        assertEquals(true, protocolConfig.isLazyinit());
        assertEquals("mode", protocolConfig.getIoMode());
        assertEquals(false, protocolConfig.isIoThreadGroupShare());
        assertEquals(false, protocolConfig.isIoThreadGroupShare());
        assertEquals(90, protocolConfig.getIoThreads());
        assertEquals("a1", protocolConfig.getExtMap().get("a"));
    }

    @Test
    public void testRegister() {
        WorkerPoolManager.registDefaultPluginConfig();
        ServiceConfig config = new ServiceConfig();
        config.setName("name");
        config.setIp("127.1.1.1");
        config.setPort(80);
        config.getRegistries().put("mock", Maps.newHashMap());
        List<PluginConfig> registries = Lists.newArrayList(new PluginConfig("mock", MockRegistry.class));
        config.setRegistryConfigs(registries);
        ProviderConfig<GenericClient> providerConfig = new ProviderConfig<>();
        providerConfig.setServiceConfig(config);
        providerConfig.setServiceInterface(GenericClient.class);
        providerConfig.setRef(new GenericClient() {
            @Override
            public CompletionStage<byte[]> asyncInvoke(RpcClientContext context, byte[] body) {
                return null;
            }

            @Override
            public byte[] invoke(RpcClientContext context, byte[] body) {
                return new byte[0];
            }
        });
        config.getProviderConfigs().add(providerConfig);
        config.setDefault();
        config.overrideConfigDefault(new ServerConfig());
        config.register();
        config.unRegister();
    }
}
