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

public class ServerAdminSchemaTest {

    private ServerAdminSchema schema;

    private static final String IP = "127.0.0.1";

    private static final Integer PORT = 9527;

    @Before
    public void setUp() {
        schema = new ServerAdminSchema();
        schema.setAdminIp(IP);
        schema.setAdminPort(PORT);
    }

    @Test
    public void testToString() {
        Assert.assertNotNull(schema.toString());
        Assert.assertTrue(schema.toString().contains(IP));
        Assert.assertTrue(schema.toString().contains(PORT.toString()));
    }
}
