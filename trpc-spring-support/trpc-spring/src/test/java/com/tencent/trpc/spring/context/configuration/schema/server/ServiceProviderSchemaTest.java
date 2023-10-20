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

package com.tencent.trpc.spring.context.configuration.schema.server;

import java.util.Collections;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ServiceProviderSchemaTest {

    private ServiceProviderSchema serviceProviderSchema;

    @Before
    public void setUp() throws Exception {
        serviceProviderSchema = new ServiceProviderSchema();
        serviceProviderSchema.setImpl("a");
        serviceProviderSchema.setFilters(Lists.newArrayList("b"));
        serviceProviderSchema.setRequestTimeout(20);
        serviceProviderSchema.setEnableLinkTimeout(Boolean.FALSE);
        serviceProviderSchema.setWorkerPool("pool");
    }

    @Test
    public void testGetImpl() {
        Assert.assertEquals("a", serviceProviderSchema.getImpl());
    }

    @Test
    public void testGetRequestTimeout() {
        Assert.assertEquals(20, serviceProviderSchema.getRequestTimeout().intValue());
    }

    @Test
    public void testGetWorkerPool() {
        Assert.assertEquals("pool", serviceProviderSchema.getWorkerPool());
    }

    @Test
    public void testGetFilters() {
        Assert.assertEquals("b", serviceProviderSchema.getFilters().get(0));
    }

    @Test
    public void testGetEnableLinkTimeout() {
        Assert.assertFalse(serviceProviderSchema.getEnableLinkTimeout());
    }

    @Test
    public void testToString() {
        Assert.assertNotNull(serviceProviderSchema.toString());
    }

    @Test
    public void testSetImpl() {
        serviceProviderSchema.setImpl("");
        Assert.assertEquals("", serviceProviderSchema.getImpl());
    }

    @Test
    public void testSetRequestTimeout() {
        serviceProviderSchema.setRequestTimeout(100);
        Assert.assertEquals(100, serviceProviderSchema.getRequestTimeout().intValue());
    }

    @Test
    public void testSetWorkerPool() {
        serviceProviderSchema.setWorkerPool("worker");
        Assert.assertEquals("worker", serviceProviderSchema.getWorkerPool());
    }

    @Test
    public void testSetFilters() {
        serviceProviderSchema.setFilters(Collections.emptyList());
        Assert.assertTrue(serviceProviderSchema.getFilters().isEmpty());
    }

    @Test
    public void testSetEnableLinkTimeout() {
        serviceProviderSchema.setEnableLinkTimeout(Boolean.TRUE);
        Assert.assertTrue(serviceProviderSchema.getEnableLinkTimeout());
    }
}