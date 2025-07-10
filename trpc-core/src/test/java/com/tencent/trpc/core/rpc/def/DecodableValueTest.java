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

package com.tencent.trpc.core.rpc.def;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.tencent.trpc.core.compressor.CompressType;
import com.tencent.trpc.core.serialization.SerializationType;
import com.tencent.trpc.core.serialization.User;
import com.tencent.trpc.core.utils.JsonUtils;
import java.lang.reflect.Type;
import org.junit.Before;
import org.junit.Test;

public class DecodableValueTest {

    private DecodableValue decodableValue;

    @Before
    public void setUp() {
        this.decodableValue = new DecodableValue(CompressType.NONE, SerializationType.JSON, null);
    }

    @Test
    public void testDecode() {
        decodableValue.decode(new Type() {
            @Override
            public String getTypeName() {
                return null;
            }
        }, false);
        byte[] rawValue = JsonUtils.toBytes(new User());
        decodableValue = new DecodableValue(CompressType.NONE, SerializationType.JSON, rawValue);
        decodableValue.decode(User.class.getGenericSuperclass(), false);
    }

    @Test
    public void testGetRawValue() {
        assertNull(decodableValue.getRawValue());
    }

    @Test
    public void testGetCompressType() {
        assertEquals(CompressType.NONE, decodableValue.getCompressType());
    }

    @Test
    public void testGetSerializeType() {
        assertEquals(SerializationType.JSON, decodableValue.getSerializeType());

    }
}