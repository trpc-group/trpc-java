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

package com.tencent.trpc.transport.netty;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Test Netty Decoder
 */
public class AbstractBatchDecoderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBatchDecoderTest.class);

    @Test
    public void setSingleDecode() {
        DecoderTest decoderTest = new DecoderTest();
        decoderTest.setSingleDecode(true);
        Assert.assertTrue(decoderTest.isSingleDecode());
    }

    @Test
    public void setCumulator() {
        DecoderTest decoderTest = new DecoderTest();
        try {
            decoderTest.setCumulator(null);
        } catch (Exception e) {
            LOGGER.warn("setCumulator error {}", e);
        }
        decoderTest.setCumulator(AbstractBatchDecoder.COMPOSITE_CUMULATOR);
    }

    @Test
    public void setDiscardAfterReads() {
        DecoderTest decoderTest = new DecoderTest();
        try {
            decoderTest.setDiscardAfterReads(-1);
        } catch (Exception e) {
            LOGGER.warn("setDiscardAfterReads error {}", e);
        }
        decoderTest.setDiscardAfterReads(1);
    }

    @Test
    public void actualReadableBytes() {
        DecoderTest decoderTest = new DecoderTest();
        decoderTest.actualReadableBytes();
    }

    @Test
    public void internalBuffer() {
        DecoderTest decoderTest = new DecoderTest();
        decoderTest.setCumulator(AbstractBatchDecoder.COMPOSITE_CUMULATOR);
        ByteBuf byteBuf = decoderTest.internalBuffer();
        Assert.assertEquals(Unpooled.EMPTY_BUFFER, byteBuf);
    }

    @Test
    public void handlerRemoved0() {
        DecoderTest decoderTest = new DecoderTest();
        try {
            decoderTest.handlerRemoved0(null);
        } catch (Exception e) {
            LOGGER.warn("handlerRemoved0 error {}", e);
        }
    }

    @Test
    public void handlerRemoved() {
        DecoderTest decoderTest = new DecoderTest();
        try {
            decoderTest.handlerRemoved(null);
        } catch (Exception e) {
            LOGGER.warn("handlerRemoved error {}", e);
        }
    }

    @Test
    public void discardSomeReadBytes() {
        DecoderTest decoderTest = new DecoderTest();
        decoderTest.discardSomeReadBytes();
    }

    @Test
    public void expandCumulation() {
        AbstractBatchDecoder.expandCumulation(ByteBufAllocator.DEFAULT,
                new EmptyByteBuf(ByteBufAllocator.DEFAULT), 1);
    }

    @Test
    public void testMERGECUMULATOR() {
        DecoderTest.MERGE_CUMULATOR.cumulate(ByteBufAllocator.DEFAULT,
                new EmptyByteBuf(ByteBufAllocator.DEFAULT), new EmptyByteBuf(ByteBufAllocator.DEFAULT));
    }

    @Test
    public void testCOMPOSITECUMULATOR() {
        DecoderTest.COMPOSITE_CUMULATOR.cumulate(ByteBufAllocator.DEFAULT,
                new EmptyByteBuf(ByteBufAllocator.DEFAULT), new EmptyByteBuf(ByteBufAllocator.DEFAULT));
    }

    private class DecoderTest extends AbstractBatchDecoder {

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        }
    }
}