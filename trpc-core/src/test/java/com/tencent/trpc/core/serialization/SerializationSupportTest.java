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

package com.tencent.trpc.core.serialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class SerializationSupportTest {

    @Test
    public void test() {
        SerializationSupport.preLoadSerialization();
        assertEquals(SerializationType.PB, SerializationSupport.ofName("pb").type());
        assertSame("pb", SerializationSupport.ofType(SerializationType.PB).name());
    }
}