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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ServerServiceSchemaTest {

    private ServerServiceSchema schema;

    @Before
    public void setUp() {
        schema = new ServerServiceSchema();
    }

    @Test
    public void setVersion() {
        String v = "version-01";
        schema.setVersion(v);
        String version = schema.getVersion();
        Assert.assertEquals(v, version);
    }

    @Test
    public void testSetGroup() {
        String group = "group";
        schema.setGroup(group);
        String schemaGroup = schema.getGroup();
        Assert.assertEquals(group, schemaGroup);
    }

    @Test
    public void testSetIp() {
        String ip = "127.0.0.1";
        schema.setIp(ip);
        String schemaIp = schema.getIp();
        Assert.assertEquals(ip, schemaIp);
    }

    @Test
    public void testSetNic() {
        String nic = "nic1";
        schema.setNic(nic);
        String schemaNic = schema.getNic();
        Assert.assertEquals(nic, schemaNic);
    }

    @Test
    public void testSetWorkerPool() {
        String workerPoll = "workerPoll";
        schema.setWorkerPool(workerPoll);
        String schemaWorkerPool = schema.getWorkerPool();
        Assert.assertEquals(workerPoll, schemaWorkerPool);
    }

    @Test
    public void testSetExtMap() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("k1", new Object());
        extMap.put("k2", new Object());

        schema.setExtMap(extMap);
        Map<String, Object> schemaExtMap = schema.getExtMap();
        Assert.assertEquals(extMap.size(), schemaExtMap.size());
    }

    @Test
    public void testSetReusePort() {
        schema.setReusePort(Boolean.TRUE);
        Boolean reusePort = schema.getReusePort();
        Assert.assertTrue(reusePort);
    }

    @Test
    public void testSetRequestTimeout() {
        schema.setRequestTimeout(100);
        String reqTime = schema.getRequestTimeout() + "";
        Assert.assertEquals(100L, Long.parseLong(reqTime));
    }

    @Test
    public void testSetDisableDefaultFilter() {
        schema.setDisableDefaultFilter(Boolean.TRUE);
        Boolean defaultFilter = schema.getDisableDefaultFilter();
        Assert.assertTrue(defaultFilter);
    }

    @Test
    public void testSetFilters() {
        List<String> list = Arrays.asList("filter1", "filter2", "filter3");
        schema.setFilters(list);
        Assert.assertEquals(list.size(), schema.getFilters().size());
    }

    @Test
    public void testSetRegistrys() {
        Map<String, Map<String, Object>> map = new HashMap<>();
        map.put("k1", new HashMap<>());
        schema.setRegistrys(map);
        Assert.assertEquals(map.size(), schema.getRegistrys().size());
    }

    @Test
    public void testSetBasePath() {
        String path = "/usr/local/basePath";
        schema.setBasePath(path);
        Assert.assertEquals(path, schema.getBasePath());
    }

    @Test
    public void testSetEnableLinkTimeout() {
        schema.setEnableLinkTimeout(Boolean.TRUE);
        Assert.assertTrue(schema.getEnableLinkTimeout());
    }

    @Test
    public void testSetIoMode() {
        schema.setIoMode(IoMode.epoll);
        Assert.assertEquals(IoMode.epoll, schema.getIoMode());
    }

    @Test
    public void testToString() {
        String v = "version-01";
        schema.setVersion(v);
        String group = "group";
        schema.setGroup(group);
        Assert.assertTrue(schema.toString().contains("version='version-01'"));
        Assert.assertTrue(schema.toString().contains("ServerServiceSchema"));
    }
}
