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

package com.tencent.trpc.core.rpc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResponseMetaTest {

    @Test
    public void test() {
        ResponseMeta meta = new ResponseMeta();
        meta.setMessageType(100);
        meta.addMessageType(1);
        meta.getMap().put("a", "b");
        meta.setSize(20);
        ResponseMeta copy = meta.clone();
        Assertions.assertEquals(101, copy.getMessageType());
        Assertions.assertEquals("b", copy.getMap().get("a"));
        Assertions.assertNotSame(copy.getMap(), meta.getMap());
        Assertions.assertEquals(20, meta.getSize());
    }
}
