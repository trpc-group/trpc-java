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

package com.tencent.trpc.core.rpc.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MethodRouterKeyTest {

    @Test
    public void test() {
        MethodRouterKey key = new MethodRouterKey("method", "service");
        MethodRouterKey key2 = new MethodRouterKey("method", "service");
        assertEquals(key.getNativeFunc(), key2.getNativeFunc());
        assertEquals(key.getRpcMethodName(), key2.getRpcMethodName());
        assertEquals(key.getRpcServiceName(), key2.getRpcServiceName());
        assertEquals(key.getSlashFunc(), key2.getSlashFunc());
    }
}
