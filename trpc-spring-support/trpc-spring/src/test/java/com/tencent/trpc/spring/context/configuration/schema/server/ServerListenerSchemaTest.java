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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ServerListenerSchemaTest {

    private ServerListenerSchema serverListenerSchema;

    private static final String listenerClass = "myListenerClass";

    @Before
    public void setUp() {
        serverListenerSchema = new ServerListenerSchema();
    }

    @Test
    public void testGetGetListenerClass() {
        serverListenerSchema.setListenerClass(listenerClass);
        String result = serverListenerSchema.getListenerClass();
        Assert.assertEquals(listenerClass, result);
    }

    @Test
    public void testToString() {
        serverListenerSchema.setListenerClass(listenerClass);
        String result = serverListenerSchema.toString();
        Assert.assertTrue(result.contains(listenerClass));
    }
}
