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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ClientServiceSchemaTest {

    private ClientServiceSchema clientServiceSchema;

    @Before
    public void setUp() {
        clientServiceSchema = new ClientServiceSchema();
        clientServiceSchema.setVersion("1024-version");
    }

    @Test
    public void testSetRequestTimeout() {
        Integer reqTimeout = 100;
        clientServiceSchema.setRequestTimeout(reqTimeout);
        Integer requestTimeout = clientServiceSchema.getRequestTimeout();
        long l = Long.parseLong(requestTimeout.toString());
        Assert.assertEquals(100L, l);
    }

    @Test
    public void testSetFilters() {
        List<String> list = Arrays.asList("filter1", "filter2", "filter3");
        clientServiceSchema.setFilters(list);
        List<String> filters = clientServiceSchema.getFilters();
        Assert.assertEquals(list.size(), filters.size());
    }

    @Test
    public void testSetWorkerPool() {
        String name = "workPool1";
        clientServiceSchema.setWorkerPool(name);
        String workerPool = clientServiceSchema.getWorkerPool();
        Assert.assertEquals(name, workerPool);
    }

    @Test
    public void testSetNamingMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("k1", new Object());
        map.put("k2", new Object());
        clientServiceSchema.setNamingMap(map);
        Map<String, Object> namingMap = clientServiceSchema.getNamingMap();
        Assert.assertEquals(map.size(), namingMap.size());
        Assert.assertTrue(namingMap.keySet().contains("k1"));
        Assert.assertTrue(namingMap.keySet().contains("k2"));
    }

    @Test
    public void testSetNamespace() {
        String ns = "public";
        clientServiceSchema.setNamespace(ns);
        String namespace = clientServiceSchema.getNamespace();
        Assert.assertEquals(ns, namespace);
    }

    @Test
    public void testSetGroup() {
        String group = "group";
        clientServiceSchema.setGroup(group);
        String schemaGroup = clientServiceSchema.getGroup();
        Assert.assertEquals(group, schemaGroup);
    }

    @Test
    public void testSetBackupRequestTimeMs() {
        Integer backReqTime = 3000;
        clientServiceSchema.setBackupRequestTimeMs(backReqTime);
        Integer backupRequestTimeMs = clientServiceSchema.getBackupRequestTimeMs();
        Assert.assertEquals(3000L, Long.parseLong(backupRequestTimeMs + ""));
    }

    @Test
    public void testSetCallerServiceName() {
        String callServiceName = "callName";
        clientServiceSchema.setCallerServiceName(callServiceName);
        String callerServiceName = clientServiceSchema.getCallerServiceName();
        Assert.assertEquals(callServiceName, callerServiceName);
    }

    @Test
    public void testToString() {
        Assert.assertNotNull(clientServiceSchema.toString());
        Assert.assertTrue(clientServiceSchema.toString().contains("1024-version"));
    }
}
