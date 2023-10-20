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

package com.tencent.trpc.core.rpc.def;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.tencent.trpc.core.compressor.spi.Compressor;
import com.tencent.trpc.core.compressor.support.NoneCompressor;
import com.tencent.trpc.core.compressor.support.SnappyCompressor;
import com.tencent.trpc.core.serialization.User;
import com.tencent.trpc.core.serialization.support.JSONSerialization;
import com.tencent.trpc.core.utils.JsonUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EncodableValueTest {

    private EncodableValue encodableValue;

    @Before
    public void setUp() throws Exception {
        encodableValue = new EncodableValue(new NoneCompressor(), 20,
                new JSONSerialization(), false, null);
    }

    @Test
    public void testEncode() {
        byte[] encode = encodableValue.encode();
        assertNull(encode);
        this.encodableValue = new EncodableValue(new NoneCompressor(), 20,
                new JSONSerialization(), true, JsonUtils.toBytes(new User()));

        assertNotNull(encodableValue.encode());

        this.encodableValue = new EncodableValue(new SnappyCompressor(), 20,
                new JSONSerialization(), false, new User());
        assertNotNull(encodableValue.encode());
    }

    @Test
    public void testGetRawValue() {
        Assert.assertNull(encodableValue.getRawValue());
    }

    @Test
    public void testGetCompressed() {
        Assert.assertFalse(encodableValue.getCompressed());
    }

    @Test
    public void testGetCompressor() {
        Compressor compressor = encodableValue.getCompressor();
        assertNotNull(compressor);
    }
}