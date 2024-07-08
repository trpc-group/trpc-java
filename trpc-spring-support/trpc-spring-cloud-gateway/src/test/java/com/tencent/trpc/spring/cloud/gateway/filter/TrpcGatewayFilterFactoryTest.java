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

package com.tencent.trpc.spring.cloud.gateway.filter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tencent.trpc.spring.cloud.gateway.filter.TrpcGatewayFilterFactory.Config;
import com.tencent.trpc.spring.cloud.gateway.filter.request.MyRequestRewriter;
import com.tencent.trpc.spring.cloud.gateway.filter.request.MyRequestRewriterTest;
import com.tencent.trpc.spring.cloud.gateway.filter.response.MyResponseRewriter;
import com.tencent.trpc.spring.cloud.gateway.filter.response.MyResponseRewriterTest;
import com.tencent.trpc.spring.cloud.gateway.rewriter.TrpcRequestRewriter;
import com.tencent.trpc.spring.cloud.gateway.rewriter.TrpcResponseRewriter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.test.util.ReflectionTestUtils;

public class TrpcGatewayFilterFactoryTest {

    private TrpcGatewayFilterFactory factory;

    private TrpcGatewayFilterFactory.Config config;

    @Before
    public void setUp() {
        config = mock(Config.class);
        factory = new TrpcGatewayFilterFactory();
        ReflectionTestUtils.setField(factory, "requestRewriter", mock(TrpcRequestRewriter.class));
        ReflectionTestUtils.setField(factory, "responseRewriter", mock(TrpcResponseRewriter.class));
    }

    @Test
    public void testApply() {
        when(config.isEnabled()).thenReturn(Boolean.TRUE);
        GatewayFilter filter = factory.apply(config);
        Assert.assertNotNull(filter);
        Assert.assertTrue(filter instanceof TrpcRoutingFilter);
    }

    @Test
    public void testLoadRequestRewriter() {
        setRequest();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadRequestRewriterWithException() {
        String className = "com.tencent.MyRequestRewriter";
        Mockito.when(config.getRequestRewriter()).thenReturn(className);
        factory.apply(config);
    }

    @Test
    public void testLoadRequestRewriterWithNull() {
        String className = "com.tencent.trpc.spring.cloud.gateway.filter.request.MyRequestRewriterTest";
        Mockito.when(config.getRequestRewriter()).thenReturn(className);
        factory.apply(config);
        Object requestRewriter = ReflectionTestUtils.getField(factory, "requestRewriter");
        Assert.assertFalse(requestRewriter instanceof MyRequestRewriterTest);
    }

    @Test
    public void testLoadResponseRewriter() {
        setRequest();
        String respClassName = "com.tencent.trpc.spring.cloud.gateway.filter.response.MyResponseRewriter";
        Mockito.when(config.getResponseRewriter()).thenReturn(respClassName);
        factory.apply(config);
        Object requestRewriter = ReflectionTestUtils.getField(factory, "responseRewriter");
        Assert.assertTrue(requestRewriter instanceof MyResponseRewriter);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testLoadResponseRewriterWithException() {
        setRequest();
        String className = "com.tencent.MyRequestRewriter";
        Mockito.when(config.getRequestRewriter()).thenReturn(className);
        factory.apply(config);
    }

    @Test
    public void testLoadResponseRewriterWithNull() {
        setRequest();
        String className = "com.tencent.trpc.spring.cloud.gateway.filter.response.MyResponseRewriterTest";
        Mockito.when(config.getResponseRewriter()).thenReturn(className);
        factory.apply(config);
        Object requestRewriter = ReflectionTestUtils.getField(factory, "responseRewriter");
        System.out.println(requestRewriter);
        Assert.assertFalse(requestRewriter instanceof MyResponseRewriterTest);
    }

    private void setRequest() {
        String className = "com.tencent.trpc.spring.cloud.gateway.filter.request.MyRequestRewriter";
        Mockito.when(config.getRequestRewriter()).thenReturn(className);
        factory.apply(config);
        Object requestRewriter = ReflectionTestUtils.getField(factory, "requestRewriter");
        Assert.assertTrue(requestRewriter instanceof MyRequestRewriter);
    }

    @Test
    public void testConfig() {
        TrpcGatewayFilterFactory.Config newConfig = new Config();
        newConfig.setEnabled(true);
        Assert.assertTrue(newConfig.isEnabled());

        String requestWriter = "com.tencent.trpc.spring.cloud.gateway.filter.request.MyRequestRewriter";
        newConfig.setRequestRewriter(requestWriter);
        Assert.assertEquals(requestWriter, newConfig.getRequestRewriter());

        String metaData = "k1:v1;k2:v2";
        newConfig.setMetadata(metaData);
        Assert.assertEquals(metaData, newConfig.getMetadata());
    }
}
