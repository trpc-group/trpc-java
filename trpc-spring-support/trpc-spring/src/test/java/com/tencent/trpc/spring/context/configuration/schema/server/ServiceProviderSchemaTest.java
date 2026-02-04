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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServiceProviderSchemaTest {

    private ServiceProviderSchema serviceProviderSchema;

    @BeforeEach
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
        Assertions.assertEquals("a", serviceProviderSchema.getImpl());
    }

    @Test
    public void testGetRequestTimeout() {
        Assertions.assertEquals(20, serviceProviderSchema.getRequestTimeout().intValue());
    }

    @Test
    public void testGetWorkerPool() {
        Assertions.assertEquals("pool", serviceProviderSchema.getWorkerPool());
    }

    @Test
    public void testGetFilters() {
        Assertions.assertEquals("b", serviceProviderSchema.getFilters().get(0));
    }

    @Test
    public void testGetEnableLinkTimeout() {
        Assertions.assertFalse(serviceProviderSchema.getEnableLinkTimeout());
    }

    @Test
    public void testToString() {
        Assertions.assertNotNull(serviceProviderSchema.toString());
    }

    @Test
    public void testSetImpl() {
        serviceProviderSchema.setImpl("");
        Assertions.assertEquals("", serviceProviderSchema.getImpl());
    }

    @Test
    public void testSetRequestTimeout() {
        serviceProviderSchema.setRequestTimeout(100);
        Assertions.assertEquals(100, serviceProviderSchema.getRequestTimeout().intValue());
    }

    @Test
    public void testSetWorkerPool() {
        serviceProviderSchema.setWorkerPool("worker");
        Assertions.assertEquals("worker", serviceProviderSchema.getWorkerPool());
    }

    @Test
    public void testSetFilters() {
        serviceProviderSchema.setFilters(Collections.emptyList());
        Assertions.assertTrue(serviceProviderSchema.getFilters().isEmpty());
    }

    @Test
    public void testSetEnableLinkTimeout() {
        serviceProviderSchema.setEnableLinkTimeout(Boolean.TRUE);
        Assertions.assertTrue(serviceProviderSchema.getEnableLinkTimeout());
    }
}
