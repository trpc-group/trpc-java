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

package com.tencent.trpc.core.transport.codec;

import com.tencent.trpc.core.compressor.CompressorSupport;
import com.tencent.trpc.core.compressor.support.SnappyCompressor;
import com.tencent.trpc.core.serialization.SerializationSupport;
import com.tencent.trpc.core.serialization.support.JSONSerialization;
import com.tencent.trpc.core.sign.SignSupport;
import com.tencent.trpc.core.transport.Channel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AbstractCodecTest {

    private AbstractCodec abstractCodec;

    @Before
    public void setUp() {
        SerializationSupport.preLoadSerialization();
        CompressorSupport.preLoadCompressors();
        SignSupport.preLoadSign();
        abstractCodec = new AbstractCodec() {
            @Override
            public void encode(Channel channel, ChannelBuffer channelBuffer, Object message) {

            }

            @Override
            public Object decode(Channel channel, ChannelBuffer channelBuffer) {
                return null;
            }
        };
    }

    @Test
    public void testCheckAndGetSerialization() {
        Assert.assertNotNull(abstractCodec.checkAndGetSerialization("json"));
        Assert.assertNotNull(abstractCodec.checkAndGetSerialization(new JSONSerialization().type()));
    }

    @Test
    public void testCheckAndGetCompressor() {
        Assert.assertNotNull(abstractCodec.checkAndGetCompressor("gzip"));
    }

    @Test
    public void testGetEncodableValue() {
        Assert.assertNotNull(abstractCodec.getEncodableValue(10, new JSONSerialization(),
                new SnappyCompressor(), false, null));
    }

    @Test
    public void testGetContentEncoding() {
        abstractCodec.getContentEncoding(abstractCodec.getEncodableValue(10, new JSONSerialization(),
                new SnappyCompressor(), false, null));
    }
}