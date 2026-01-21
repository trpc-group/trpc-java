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

package com.tencent.trpc.transport.netty;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.transport.codec.ChannelBuffer;
import io.netty.buffer.UnpooledByteBufAllocator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ChannelBufferTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelBufferTest.class);

    @Test
    public void test() {
        NettyChannelBuffer channelBuffer =
                new NettyChannelBuffer(UnpooledByteBufAllocator.DEFAULT.buffer(65535));
        channelBuffer.writeByte(255);
        System.out.println(channelBuffer.readByte());
        channelBuffer.writeByte(255);
        System.out.println(channelBuffer.readUnsignedByte());

        ChannelBuffer copy = channelBuffer.copy();
        copy.capacity(1000);
        Assertions.assertTrue(copy.capacity() == 1000, "capacity = 1000");
        Assertions.assertTrue(copy.maxCapacity() != 0);
        Assertions.assertTrue(copy.isDirect());
        copy.clear();
        copy.writeByte(1);
        Assertions.assertTrue(copy.isReadable());
        copy.readByte();
        Assertions.assertTrue(!copy.isReadOnly());
        Assertions.assertTrue(copy.asReadOnly().isReadOnly());

        copy.clear();
        copy.writeCharSequence("abcdefg", Charset.defaultCharset());
        Assertions.assertTrue(copy.readerIndex() == 0);
        Assertions.assertTrue(copy.writerIndex() == "abcdefg".length());
        copy.readerIndex(3);
        copy.writerIndex(3);
        Assertions.assertTrue(copy.readerIndex() == 3);
        Assertions.assertTrue(copy.writerIndex() == 3);
        copy.setIndex(4, 4);
        Assertions.assertTrue(copy.readerIndex() == 4);
        Assertions.assertTrue(copy.writerIndex() == 4);
        //
        copy.clear();
        copy.writeCharSequence("abcd", Charset.defaultCharset());
        Assertions.assertTrue(copy.readableBytes() == 4);
        Assertions.assertTrue(copy.writableBytes() == copy.capacity() - "abcd".length());
        Assertions.assertTrue(copy.maxWritableBytes() == copy.maxCapacity() - "abcd".length());
        Assertions.assertTrue(copy.isReadable());
        Assertions.assertTrue(!copy.isReadable(5));
        Assertions.assertTrue(copy.isReadable(3));
        Assertions.assertTrue(copy.isWritable());
        Assertions.assertTrue(copy.isWritable(3));
        Assertions.assertTrue(!copy.isWritable(Integer.MAX_VALUE));
        //
        copy.clear();
        copy.writeCharSequence("abcd", Charset.defaultCharset());
        copy.markReaderIndex();
        copy.readByte();
        Assertions.assertTrue(copy.readerIndex() == 1);
        copy.resetReaderIndex();
        Assertions.assertTrue(copy.readerIndex() == 0);
        copy.markWriterIndex();
        copy.writeBoolean(false);
        Assertions.assertTrue(copy.writerIndex() == "abcd".length() + 1);
        copy.resetWriterIndex();
        Assertions.assertTrue(copy.writerIndex() == "abcd".length());

        //
        copy.clear();
        copy.writeCharSequence("abcd", Charset.defaultCharset());
        copy.readByte();
        Assertions.assertTrue(copy.readerIndex() == 1);
        copy.discardReadBytes();
        Assertions.assertTrue(copy.readerIndex() == 0);

        copy.clear();
        copy.capacity(10);
        copy.writeCharSequence("abcd", Charset.defaultCharset());
        copy.ensureWritable(10);
        Assertions.assertTrue(copy.capacity() > 14, copy.capacity() + "");

        copy.clear();
        copy.writeByte(1);
        copy.writeBoolean(true);
        Assertions.assertTrue(copy.getBoolean(1));
        Assertions.assertTrue(copy.readBoolean());
        copy.setBoolean(1, false);
        Assertions.assertTrue(!copy.getBoolean(1));

        copy.clear();
        copy.writeByte(1);
        copy.writeByte(2);
        Assertions.assertTrue(copy.getByte(1) == 2);
        Assertions.assertTrue(copy.readByte() == 1);
        copy.setByte(1, 3);
        Assertions.assertTrue(copy.getByte(1) == 3);

        copy.clear();
        copy.writeByte(1);
        copy.writeByte(254);
        Assertions.assertTrue(copy.getUnsignedByte(1) == 254);
        Assertions.assertTrue(copy.readUnsignedByte() == 1);

        copy.clear();
        copy.writeByte(1);
        copy.writeShort(11);
        Assertions.assertTrue(copy.getShort(1) == 11);
        copy.readByte();
        Assertions.assertTrue(copy.readShort() == 11);
        copy.setShort(0, 22);
        Assertions.assertTrue(copy.getShort(0) == 22);

        copy.clear();
        copy.writeShortLE(11);
        Assertions.assertTrue(copy.getShortLE(0) == 11, copy.getShortLE(0) + "");
        Assertions.assertTrue(copy.getShort(0) != 11);
        Assertions.assertTrue(copy.readShortLE() == 11);
        copy.setShortLE(0, 22);
        Assertions.assertTrue(copy.getShortLE(0) == 22);

        copy.clear();
        copy.writeShort(Short.MAX_VALUE);
        Assertions.assertTrue(copy.getUnsignedShort(0) == Short.MAX_VALUE);
        Assertions.assertTrue(copy.readUnsignedShort() == Short.MAX_VALUE);
        copy.setShort(0, Short.MAX_VALUE - 1);
        Assertions.assertTrue(copy.getUnsignedShort(0) == Short.MAX_VALUE - 1);

        copy.clear();
        copy.writeShortLE(Short.MAX_VALUE);
        Assertions.assertTrue(copy.getUnsignedShortLE(0) == Short.MAX_VALUE);
        Assertions.assertTrue(copy.readUnsignedShortLE() == Short.MAX_VALUE);
        copy.setShortLE(0, Short.MAX_VALUE - 1);
        Assertions.assertTrue(copy.getUnsignedShortLE(0) == Short.MAX_VALUE - 1);

        copy.clear();
        copy.writeInt(Integer.MAX_VALUE);
        Assertions.assertTrue(copy.getInt(0) == Integer.MAX_VALUE);
        Assertions.assertTrue(copy.readInt() == Integer.MAX_VALUE);
        copy.setInt(0, Integer.MAX_VALUE - 1);
        Assertions.assertTrue(copy.getInt(0) == Integer.MAX_VALUE - 1);

        copy.clear();
        copy.writeIntLE(Integer.MAX_VALUE);
        Assertions.assertTrue(copy.getIntLE(0) == Integer.MAX_VALUE);
        Assertions.assertTrue(copy.readIntLE() == Integer.MAX_VALUE);
        copy.setIntLE(0, Integer.MAX_VALUE - 1);
        Assertions.assertTrue(copy.getIntLE(0) == Integer.MAX_VALUE - 1);

        copy.clear();
        copy.writeInt(Integer.MAX_VALUE);
        Assertions.assertTrue(copy.getUnsignedInt(0) == Integer.MAX_VALUE);
        Assertions.assertTrue(copy.readUnsignedInt() == Integer.MAX_VALUE);

        copy.clear();
        copy.writeIntLE(Integer.MAX_VALUE);
        Assertions.assertTrue(copy.getUnsignedIntLE(0) == Integer.MAX_VALUE);
        Assertions.assertTrue(copy.readUnsignedIntLE() == Integer.MAX_VALUE);
        copy.setIntLE(0, Integer.MAX_VALUE - 1);
        Assertions.assertTrue(copy.getUnsignedIntLE(0) == Integer.MAX_VALUE - 1);

        copy.clear();
        copy.writeLong(Long.MAX_VALUE);
        Assertions.assertTrue(copy.getLong(0) == Long.MAX_VALUE);
        Assertions.assertTrue(copy.readLong() == Long.MAX_VALUE);
        copy.setLong(0, Long.MAX_VALUE - 1);
        Assertions.assertTrue(copy.getLong(0) == Long.MAX_VALUE - 1);

        copy.clear();
        copy.writeLongLE(Long.MAX_VALUE);
        Assertions.assertTrue(copy.getLongLE(0) == Long.MAX_VALUE);
        Assertions.assertTrue(copy.readLongLE() == Long.MAX_VALUE);
        copy.setLongLE(0, Long.MAX_VALUE - 1);
        Assertions.assertTrue(copy.getLongLE(0) == Long.MAX_VALUE - 1);

        copy.clear();
        copy.writeChar('A');
        Assertions.assertTrue(copy.getChar(0) == 'A');
        Assertions.assertTrue(copy.readChar() == 'A');
        copy.setChar(0, 'B');
        Assertions.assertTrue(copy.getChar(0) == 'B');

        copy.clear();
        copy.writeCharSequence("A", Charset.defaultCharset());
        Assertions.assertTrue(copy.getCharSequence(0, 1, Charset.defaultCharset()).equals("A"),
                copy.getCharSequence(0, 1, Charset.defaultCharset()) + "");
        Assertions.assertTrue(copy.readCharSequence(1, Charset.defaultCharset()).equals("A"));
        copy.setCharSequence(0, "B", Charset.defaultCharset());
        Assertions.assertTrue(copy.getCharSequence(0, 1, Charset.defaultCharset()).equals("B"));

        copy.clear();
        copy.writeFloat(Float.valueOf("1.0"));
        Assertions.assertTrue(String.valueOf(copy.getFloat(0)).equals("1.0"),
                String.valueOf(copy.getFloat(0)));
        Assertions.assertTrue(String.valueOf(copy.readFloat()).equals("1.0"));
        copy.setFloat(0, Float.valueOf("1.0"));
        Assertions.assertTrue(String.valueOf(copy.getFloat(0)).equals("1.0"));

        copy.clear();
        copy.writeDouble(Float.valueOf("1.0"));
        Assertions.assertTrue(String.valueOf(copy.getDouble(0)).equals("1.0"),
                String.valueOf(copy.getDouble(0)));
        Assertions.assertTrue(String.valueOf(copy.readDouble()).equals("1.0"));
        copy.setDouble(0, Float.valueOf("1.0"));
        Assertions.assertTrue(String.valueOf(copy.getDouble(0)).equals("1.0"));

        copy.clear();
        copy.capacity(1000);
        ChannelBuffer copy2 = copy.copy();
        copy2.capacity(1000);
        copy.writeByte(1);
        ChannelBuffer copy3 = copy.copy();
        copy.getBytes(0, copy2);
        Assertions.assertTrue(copy2.readByte() == 1);
        Assertions.assertTrue(copy3.capacity() == 1);
        Assertions.assertTrue(copy3.readerIndex() == 0);
        Assertions.assertTrue(copy3.writerIndex() == 1);
        Assertions.assertTrue(copy3.getByte(0) == 1);
        IndexOutOfBoundsException ex = null;
        try {
            copy.getBytes(0, copy2, Integer.MAX_VALUE);
        } catch (IndexOutOfBoundsException e) {
            ex = e;
        }
        Assertions.assertTrue(ex != null);

        copy.clear();
        copy.capacity(1000);
        copy.writeByte(1);
        byte[] b = new byte[99];
        copy.getBytes(0, b);
        Assertions.assertTrue(b[0] == 1);
        copy.setByte(0, 2);
        copy.getBytes(0, b, 0, 1);
        Assertions.assertTrue(b[0] == 2);

        copy.setByte(0, 3);
        copy.getBytes(0, ByteBuffer.wrap(b));
        Assertions.assertTrue(b[0] == 3);
        try {
            copy.setByte(0, 4);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            copy.getBytes(0, out, 1);
            Assertions.assertTrue(out.toByteArray()[0] == 4);
        } catch (IOException e) {
            LOGGER.error("error:", e);
        }

        copy.clear();
        copy.capacity(1000);
        copy2 = copy.copy();
        copy2.capacity(1000);
        copy2.writeByte(1);
        copy.setBytes(0, copy2);
        try {
            copy.setBytes(0, copy2, Integer.MAX_VALUE);
        } catch (IndexOutOfBoundsException e) {
            ex = e;
        }
        Assertions.assertTrue(ex != null);
        Assertions.assertTrue(copy2.readerIndex() == 1);
        Assertions.assertTrue(copy.getByte(0) == 1);

        copy.clear();
        b = new byte[]{1};
        copy.setBytes(0, b);
        Assertions.assertTrue(copy.getByte(0) == 1);

        copy.clear();
        b = new byte[]{1};
        copy.setBytes(0, ByteBuffer.wrap(b));
        Assertions.assertTrue(copy.getByte(0) == 1);

        copy.clear();
        b = new byte[]{1};
        try {
            copy.setBytes(0, new ByteArrayInputStream(b), 1);
        } catch (IOException e) {
            LOGGER.error("error:", e);
        }
        Assertions.assertTrue(copy.getByte(0) == 1);

        copy.clear();
        copy.capacity(100);
        copy.writeCharSequence("abcdefghijklmn", Charset.defaultCharset());
        copy.readerIndex(0);
        copy.setByte(0, 1);
        b = new byte[10];
        copy.readBytes(b, 0, 1);
        Assertions.assertTrue(b[0] == 1);

        copy.setByte(0, 2);
        copy.readerIndex(0);
        copy.readBytes(ByteBuffer.wrap(b));
        Assertions.assertTrue(b[0] == 2, b[0] + "");

        copy.setByte(0, 3);
        copy.readerIndex(0);
        copy.readBytes(ByteBuffer.wrap(b));
        Assertions.assertTrue(b[0] == 3);

        copy.setByte(0, 4);
        copy.readerIndex(0);
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        try {
            copy.readBytes(s, 1);
        } catch (IOException e) {
            LOGGER.error("error:", e);
        }
        Assertions.assertTrue(s.toByteArray()[0] == 4);

        copy.clear();
        copy.capacity(100);
        copy.writeCharSequence("abcd", Charset.defaultCharset());
        copy.skipBytes(1);
        Assertions.assertTrue(copy.readCharSequence(1, Charset.defaultCharset()).equals("b"));

        ChannelBuffer copyC1 = channelBuffer.copy();
        ChannelBuffer copyC2 = channelBuffer.copy();
        copyC1.capacity(1000);
        copyC1.writeByte(1);
        copyC2.capacity(1000);
        copyC2.writeByte(1);
        copyC2.toString();
        Assertions.assertTrue(0 == copyC2.compareTo(copyC1));

        copy.clear();
        copy.capacity(100);
        copy.writeByte(1);
        b = new byte[]{1};
        copy.writeBytes(b, 0, 1);
        Assertions.assertTrue(copy.readByte() == 1);
        copy.writeBytes(ByteBuffer.wrap(b));
        Assertions.assertTrue(copy.readByte() == 1);
        try {
            copy.writeBytes(new ByteArrayInputStream(b), 1);
        } catch (IOException e1) {
            LOGGER.error("error:", e1);
        }
        Assertions.assertTrue(copy.readByte() == 1);

        copy.clear();
        copy.capacity(100);
        ChannelBuffer writeCopy = copy.copy();
        writeCopy.capacity(100);
        writeCopy.writeByte(1);
        copy.writeBytes(writeCopy);
        Assertions.assertTrue(copy.readerIndex() == 0);
        Assertions.assertTrue(copy.readableBytes() == 1);
        Assertions.assertTrue(copy.readByte() == 1);

        try {
            copy.clear();
            copy.capacity(100);
            copy.writeCharSequence("abcd", Charset.defaultCharset());
            Assertions.assertTrue(copy.arrayOffset() == 0);
        } catch (Exception e) {
            LOGGER.error("error:", e);
        }

        copy.clear();
        copy.capacity(101);
        ChannelBuffer readCopy = copy.copy();
        readCopy.capacity(1);
        copy.writeByte(1);
        copy.readBytes(readCopy);
        Assertions.assertTrue(readCopy.readerIndex() == 0);
        Assertions.assertTrue(readCopy.readableBytes() == 1);
        Assertions.assertTrue(readCopy.readByte() == 1);
    }
}
