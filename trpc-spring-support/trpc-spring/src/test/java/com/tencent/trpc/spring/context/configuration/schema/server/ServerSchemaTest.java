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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ServerSchemaTest {

    private ServerSchema schema;

    @Before
    public void setUp() {
        schema = new ServerSchema();
    }

    @Test
    public void testSetServerListener() {
        List<ServerListenerSchema> schemaList = getServerListenerSchemas();
        schema.setServerListener(schemaList);
        Assert.assertEquals(schemaList.size(), schema.getServerListener().size());
    }

    private static List<ServerListenerSchema> getServerListenerSchemas() {
        List<ServerListenerSchema> schemaList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ServerListenerSchema listenerSchema = new ServerListenerSchema();
            listenerSchema.setListenerClass("listener" + i);
            schemaList.add(listenerSchema);
        }
        return schemaList;
    }

    @Test
    public void testSetRunListeners() {
        List<String> runnerListener = Arrays.asList("listener1", "listener2", "listener3");
        schema.setRunListeners(runnerListener);
        Assert.assertEquals(runnerListener.size(), schema.getRunListeners().size());

    }

    @Test
    public void testSetNic() {
        String nic = "myNic";
        schema.setNic(nic);
        Assert.assertEquals(nic, schema.getNic());
    }

    @Test
    public void testSetEnableLinkTimeout() {
        schema.setEnableLinkTimeout(Boolean.TRUE);
        Assert.assertTrue(schema.getEnableLinkTimeout());
    }

    @Test
    public void testSetDisableDefaultFilter() {
        schema.setDisableDefaultFilter(Boolean.FALSE);
        Assert.assertFalse(schema.getDisableDefaultFilter());
    }

    @Test
    public void testSetFilters() {
        List<String> testFilter = Arrays.asList("testFilter");
        schema.setFilters(testFilter);
        Assert.assertEquals(testFilter.size(), schema.getFilters().size());
    }

    @Test
    public void testSetCloseTimeout() {
        schema.setCloseTimeout(1000L);
        Assert.assertEquals(1000L, Long.parseLong(schema.getCloseTimeout() + ""));
    }

    @Test
    public void testSetConfigCenter() {
        schema.setConfigCenter("rainbow-config");
        Assert.assertEquals("rainbow-config", schema.getConfigCenter());
    }

    @Test
    public void testToString() {
        schema.setNic("myNic");

        ServerSchema serverSchema = new ServerSchema();
        serverSchema.setNic("myNic2");
        Assert.assertNotEquals(schema, serverSchema);
        Assert.assertTrue(serverSchema.toString().contains("myNic2"));

        serverSchema.setNic("myNic");
        Assert.assertNotEquals(schema, serverSchema);
        Assert.assertTrue(schema.toString().contains("myNic"));
    }
}
