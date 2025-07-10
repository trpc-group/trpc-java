/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 Tencent.
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.core.rpc;

import org.junit.Assert;
import org.junit.Test;

public class ResponseMetaTest {

    @Test
    public void test() {
        ResponseMeta meta = new ResponseMeta();
        meta.setMessageType(100);
        meta.addMessageType(1);
        meta.getMap().put("a", "b");
        meta.setSize(20);
        ResponseMeta copy = meta.clone();
        Assert.assertEquals(101, copy.getMessageType());
        Assert.assertEquals("b", copy.getMap().get("a"));
        Assert.assertNotSame(copy.getMap(), meta.getMap());
        Assert.assertEquals(20, meta.getSize());
    }
}
