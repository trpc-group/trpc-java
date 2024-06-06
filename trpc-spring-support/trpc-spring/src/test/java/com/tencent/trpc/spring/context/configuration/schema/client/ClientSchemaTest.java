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

package com.tencent.trpc.spring.context.configuration.schema.client;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ClientSchemaTest {

    private ClientSchema schema;

    @Before
    public void setUp() {
        schema = new ClientSchema();
    }

    @Test
    public void testSetNamespace() {
        String ns = "ns-1";
        schema.setNamespace(ns);
        Assert.assertEquals(ns, schema.getNamespace());
    }

    @Test
    public void testSetRequestTimeout() {
        Integer timeout = 2000;
        schema.setRequestTimeout(timeout);
        Assert.assertEquals(Long.parseLong(timeout + ""), Long.parseLong(schema.getRequestTimeout() + ""));
    }

    @Test
    public void testSetProxyType() {
        String proxyType = "proxy-type";
        schema.setProxyType(proxyType);
        Assert.assertEquals(proxyType, schema.getProxyType());
    }

    @Test
    public void testSetFilters() {
        List<String> list = Arrays.asList("filter1", "filter2", "filter3");
        schema.setFilters(list);
        Assert.assertEquals(list.size(), schema.getFilters().size());
    }

    @Test
    public void setInterceptors() {
        List<String> list = Arrays.asList("incerceptor1", "incerceptor2", "incerceptor3");
        schema.setInterceptors(list);
        Assert.assertEquals(list.size(), schema.getInterceptors().size());
    }

    @Test
    public void setCallerServiceName() {
        String callerServiceName = "serviceName-001";
        schema.setCallerServiceName(callerServiceName);
        Assert.assertEquals(callerServiceName, schema.getCallerServiceName());
    }

    @Test
    public void testToString() {
        schema.setNamespace("ns-001");
        ClientSchema clientSchema = new ClientSchema();
        clientSchema.setNamespace("ns-002");
        Assert.assertNotEquals(schema, clientSchema);
        clientSchema.setNamespace("ns-001");
        Assert.assertNotEquals(schema.toString(), clientSchema.toString());
        Assert.assertTrue(schema.toString().contains("ns-001"));
        Assert.assertTrue(clientSchema.toString().contains("ns-001"));
    }
}
