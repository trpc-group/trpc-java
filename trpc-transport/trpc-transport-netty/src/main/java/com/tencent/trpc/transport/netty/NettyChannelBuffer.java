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

import com.google.common.base.Preconditions;
import com.tencent.trpc.core.transport.codec.ChannelBuffer;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Encapsulating Netty's buffer.
 */
public class NettyChannelBuffer extends ChannelBuffer {

    private final ByteBuf ioBuffer;

    public NettyChannelBuffer(ByteBuf ioBuffer) {
        Preconditions.checkArgument(ioBuffer != null, "buffer == null");
        this.ioBuffer = ioBuffer;
    }

    @Override
    public int capacity() {
        return ioBuffer.capacity();
    }

    @Override
    public ChannelBuffer capacity(int newCapacity) {
        ioBuffer.capacity(newCapacity);
        return this;
    }

    @Override
    public int maxCapacity() {
        return ioBuffer.maxCapacity();
    }

    @Override
    public boolean isDirect() {
        return ioBuffer.isDirect();
    }

    @Override
    public boolean isReadOnly() {
        return ioBuffer.isReadOnly();
    }

    @Override
    public ChannelBuffer asReadOnly() {
        return new NettyChannelBuffer(ioBuffer.asReadOnly());
    }

    @Override
    public int readerIndex() {
        return ioBuffer.readerIndex();
    }

    @Override
    public ChannelBuffer readerIndex(int readerIndex) {
        ioBuffer.readerIndex(readerIndex);
        return this;
    }

    @Override
    public int writerIndex() {
        return ioBuffer.writerIndex();
    }

    @Override
    public ChannelBuffer writerIndex(int writerIndex) {
        ioBuffer.writerIndex(writerIndex);
        return this;
    }

    @Override
    public ChannelBuffer setIndex(int readerIndex, int writerIndex) {
        ioBuffer.setIndex(readerIndex, writerIndex);
        return this;
    }

    @Override
    public int readableBytes() {
        return ioBuffer.readableBytes();
    }

    @Override
    public int writableBytes() {
        return ioBuffer.writableBytes();
    }

    @Override
    public int maxWritableBytes() {
        return ioBuffer.maxWritableBytes();
    }

    @Override
    public boolean isReadable() {
        return ioBuffer.isReadable();
    }

    @Override
    public boolean isReadable(int size) {
        return ioBuffer.isReadable(size);
    }

    @Override
    public boolean isWritable() {
        return ioBuffer.isWritable();
    }

    @Override
    public boolean isWritable(int size) {
        return ioBuffer.isWritable(size);
    }

    @Override
    public ChannelBuffer clear() {
        ioBuffer.clear();
        return this;
    }

    @Override
    public ChannelBuffer markReaderIndex() {
        ioBuffer.markReaderIndex();
        return this;
    }

    @Override
    public ChannelBuffer resetReaderIndex() {
        ioBuffer.resetReaderIndex();
        return this;
    }

    @Override
    public ChannelBuffer markWriterIndex() {
        ioBuffer.markWriterIndex();
        return this;
    }

    @Override
    public ChannelBuffer resetWriterIndex() {
        ioBuffer.resetWriterIndex();
        return this;
    }

    @Override
    public ChannelBuffer discardReadBytes() {
        ioBuffer.discardReadBytes();
        return this;
    }

    public ChannelBuffer ensureWritable(int minWritableBytes) {
        ioBuffer.ensureWritable(minWritableBytes);
        return this;
    }

    @Override
    public boolean getBoolean(int index) {
        return ioBuffer.getBoolean(index);
    }

    @Override
    public byte getByte(int index) {
        return ioBuffer.getByte(index);
    }

    @Override
    public short getUnsignedByte(int index) {
        return ioBuffer.getUnsignedByte(index);
    }

    @Override
    public short getShort(int index) {
        return ioBuffer.getShort(index);
    }

    @Override
    public short getShortLE(int index) {
        return ioBuffer.getShortLE(index);
    }

    @Override
    public int getUnsignedShort(int index) {
        return ioBuffer.getUnsignedShort(index);
    }

    @Override
    public int getUnsignedShortLE(int index) {
        return ioBuffer.getUnsignedShortLE(index);
    }

    @Override
    public int getInt(int index) {
        return ioBuffer.getInt(index);
    }

    @Override
    public int getIntLE(int index) {
        return ioBuffer.getIntLE(index);
    }

    @Override
    public long getUnsignedInt(int index) {
        return ioBuffer.getUnsignedInt(index);
    }

    @Override
    public long getUnsignedIntLE(int index) {
        return ioBuffer.getUnsignedIntLE(index);
    }

    @Override
    public long getLong(int index) {
        return ioBuffer.getLong(index);
    }

    @Override
    public long getLongLE(int index) {
        return ioBuffer.getLongLE(index);
    }

    @Override
    public char getChar(int index) {
        return ioBuffer.getChar(index);
    }

    @Override
    public float getFloat(int index) {
        return ioBuffer.getFloat(index);
    }

    @Override
    public double getDouble(int index) {
        return ioBuffer.getDouble(index);
    }

    @Override
    public ChannelBuffer getBytes(int index, ChannelBuffer dst) {
        getBytes(index, dst, dst.writableBytes());
        return this;
    }

    @Override
    public ChannelBuffer getBytes(int index, ChannelBuffer dst, int length) {
        if (length > dst.writableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        getBytes(index, dst, dst.writerIndex(), length);
        dst.writerIndex(dst.writerIndex() + length);
        return this;
    }

    @Override
    public ChannelBuffer getBytes(int index, ChannelBuffer dst, int dstIndex, int length) {
        byte[] data = new byte[length];
        ioBuffer.getBytes(index, data, 0, length);
        dst.setBytes(dstIndex, data, 0, length);
        return this;
    }

    @Override
    public ChannelBuffer getBytes(int index, byte[] dst) {
        ioBuffer.getBytes(index, dst);
        return this;
    }

    @Override
    public ChannelBuffer getBytes(int index, byte[] dst, int dstIndex, int length) {
        ioBuffer.getBytes(index, dst, dstIndex, length);
        return this;
    }

    @Override
    public ChannelBuffer getBytes(int index, ByteBuffer dst) {
        ioBuffer.getBytes(index, dst);
        return this;
    }

    @Override
    public ChannelBuffer getBytes(int index, OutputStream out, int length) throws IOException {
        ioBuffer.getBytes(index, out, length);
        return this;
    }

    @Override
    public CharSequence getCharSequence(int index, int length, Charset charset) {
        return ioBuffer.getCharSequence(index, length, charset);
    }

    @Override
    public ChannelBuffer setBoolean(int index, boolean value) {
        ioBuffer.setBoolean(index, value);
        return this;
    }

    @Override
    public ChannelBuffer setByte(int index, int value) {
        ioBuffer.setByte(index, value);
        return this;
    }

    @Override
    public ChannelBuffer setShort(int index, int value) {
        ioBuffer.setShort(index, value);
        return this;
    }

    @Override
    public ChannelBuffer setShortLE(int index, int value) {
        ioBuffer.setShortLE(index, value);
        return this;
    }

    @Override
    public ChannelBuffer setInt(int index, int value) {
        ioBuffer.setInt(index, value);
        return this;
    }

    @Override
    public ChannelBuffer setIntLE(int index, int value) {
        ioBuffer.setIntLE(index, value);
        return this;
    }

    @Override
    public ChannelBuffer setLong(int index, long value) {
        ioBuffer.setLong(index, value);
        return this;
    }

    @Override
    public ChannelBuffer setLongLE(int index, long value) {
        ioBuffer.setLongLE(index, value);
        return this;
    }

    @Override
    public ChannelBuffer setChar(int index, int value) {
        ioBuffer.setChar(index, value);
        return this;
    }

    @Override
    public ChannelBuffer setFloat(int index, float value) {
        ioBuffer.setFloat(index, value);
        return this;
    }

    @Override
    public ChannelBuffer setDouble(int index, double value) {
        ioBuffer.setDouble(index, value);
        return this;
    }

    @Override
    public ChannelBuffer setBytes(int index, ChannelBuffer src) {
        setBytes(index, src, src.readableBytes());
        return this;
    }

    @Override
    public ChannelBuffer setBytes(int index, ChannelBuffer src, int length) {
        int readableBytes = src.readableBytes();
        if (length > readableBytes) {
            throw new IndexOutOfBoundsException(
                    "src readableBytes[" + readableBytes + "] < " + length);
        }
        setBytes(index, src, src.readerIndex(), length);
        src.readerIndex(src.readerIndex() + length);
        return this;
    }

    @Override
    public ChannelBuffer setBytes(int index, ChannelBuffer src, int srcIndex, int length) {
        byte[] data = new byte[length];
        src.getBytes(srcIndex, data, 0, length);
        setBytes(index, data, 0, length);
        return this;
    }

    @Override
    public ChannelBuffer setBytes(int index, byte[] src) {
        ioBuffer.setBytes(index, src);
        return this;
    }

    @Override
    public ChannelBuffer setBytes(int index, byte[] src, int srcIndex, int length) {
        ioBuffer.setBytes(index, src, srcIndex, length);
        return this;
    }

    @Override
    public ChannelBuffer setBytes(int index, ByteBuffer src) {
        ioBuffer.setBytes(index, src);
        return this;
    }

    @Override
    public int setBytes(int index, InputStream in, int length) throws IOException {
        return ioBuffer.setBytes(index, in, length);
    }

    @Override
    public int setCharSequence(int index, CharSequence sequence, Charset charset) {
        return ioBuffer.setCharSequence(index, sequence, charset);
    }

    @Override
    public boolean readBoolean() {
        return ioBuffer.readBoolean();
    }

    @Override
    public byte readByte() {
        return ioBuffer.readByte();
    }

    @Override
    public short readUnsignedByte() {
        return ioBuffer.readUnsignedByte();
    }

    @Override
    public short readShort() {
        return ioBuffer.readShort();
    }

    @Override
    public short readShortLE() {
        return ioBuffer.readShortLE();
    }

    @Override
    public int readUnsignedShort() {
        return ioBuffer.readUnsignedShort();
    }

    @Override
    public int readUnsignedShortLE() {
        return ioBuffer.readUnsignedShortLE();
    }

    @Override
    public int readInt() {
        return ioBuffer.readInt();
    }

    @Override
    public int readIntLE() {
        return ioBuffer.readIntLE();
    }

    @Override
    public long readUnsignedInt() {
        return ioBuffer.readUnsignedInt();
    }

    @Override
    public long readUnsignedIntLE() {
        return ioBuffer.readUnsignedIntLE();
    }

    @Override
    public long readLong() {
        return ioBuffer.readLong();
    }

    @Override
    public long readLongLE() {
        return ioBuffer.readLongLE();
    }

    @Override
    public char readChar() {
        return ioBuffer.readChar();
    }

    @Override
    public float readFloat() {
        return ioBuffer.readFloat();
    }

    @Override
    public double readDouble() {
        return ioBuffer.readDouble();
    }

    @Override
    public ChannelBuffer readBytes(int length) {
        return new NettyChannelBuffer(ioBuffer.readBytes(length));
    }

    @Override
    public ChannelBuffer readBytes(ChannelBuffer dst) {
        // Note: The semantics of Netty's readBytes method for ByteBuf is to fill the destination ByteBuf (dst)
        // with data from the source ByteBuf (src) until there is no more space to write in dst.
        // It reads data from the current ByteBuf's readerIndex until the target ByteBuf has no more writable space,
        // and writes the data starting from the target ByteBuf's writeIndex. After reading, the current ByteBuf's
        // readerIndex is incremented by the number of bytes read, and the target ByteBuf's writeIndex is incremented
        // by the number of bytes read.
        // It is not recommended to use this method, and it is recommended to use readBytes(dst, src.readableBytes())
        // instead.
        readBytes(dst, dst.writableBytes());
        return this;
    }

    @Override
    public ChannelBuffer readBytes(ChannelBuffer dst, int length) {
        if (length > dst.writableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        readBytes(dst, dst.writerIndex(), length);
        dst.writerIndex(dst.writerIndex() + length);
        return this;
    }

    @Override
    public ChannelBuffer readBytes(ChannelBuffer dst, int dstIndex, int length) {
        if (readableBytes() < length) {
            throw new IndexOutOfBoundsException();
        }
        byte[] data = new byte[length];
        ioBuffer.readBytes(data, 0, length);
        dst.setBytes(dstIndex, data, 0, length);
        return this;
    }

    @Override
    public ChannelBuffer readBytes(byte[] dst) {
        ioBuffer.readBytes(dst);
        return this;
    }

    @Override
    public ChannelBuffer readBytes(byte[] dst, int dstIndex, int length) {
        ioBuffer.readBytes(dst, dstIndex, length);
        return this;
    }

    @Override
    public ChannelBuffer readBytes(ByteBuffer dst) {
        ioBuffer.readBytes(dst);
        return this;
    }

    @Override
    public ChannelBuffer readBytes(OutputStream out, int length) throws IOException {
        ioBuffer.readBytes(out, length);
        return this;
    }

    @Override
    public CharSequence readCharSequence(int length, Charset charset) {
        return ioBuffer.readCharSequence(length, charset);
    }

    @Override
    public ChannelBuffer skipBytes(int length) {
        ioBuffer.skipBytes(length);
        return this;
    }

    @Override
    public ChannelBuffer writeBoolean(boolean value) {
        ioBuffer.writeBoolean(value);
        return this;
    }

    @Override
    public ChannelBuffer writeByte(int value) {
        ioBuffer.writeByte(value);
        return this;
    }

    @Override
    public ChannelBuffer writeShort(int value) {
        ioBuffer.writeShort(value);
        return this;
    }

    @Override
    public ChannelBuffer writeShortLE(int value) {
        ioBuffer.writeShortLE(value);
        return this;
    }

    @Override
    public ChannelBuffer writeInt(int value) {
        ioBuffer.writeInt(value);
        return this;
    }

    @Override
    public ChannelBuffer writeIntLE(int value) {
        ioBuffer.writeIntLE(value);
        return this;
    }

    @Override
    public ChannelBuffer writeLong(long value) {
        ioBuffer.writeLong(value);
        return this;
    }

    @Override
    public ChannelBuffer writeLongLE(long value) {
        ioBuffer.writeLongLE(value);
        return this;
    }

    @Override
    public ChannelBuffer writeChar(int value) {
        ioBuffer.writeChar(value);
        return this;
    }

    @Override
    public ChannelBuffer writeFloat(float value) {
        ioBuffer.writeFloat(value);
        return this;
    }

    @Override
    public ChannelBuffer writeDouble(double value) {
        ioBuffer.writeDouble(value);
        return this;
    }

    @Override
    public ChannelBuffer writeBytes(ChannelBuffer src) {
        writeBytes(src, src.readableBytes());
        return this;
    }

    @Override
    public ChannelBuffer writeBytes(ChannelBuffer src, int length) {
        if (length > src.readableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        writeBytes(src, src.readerIndex(), length);
        src.readerIndex(src.readerIndex() + length);
        return this;
    }

    @Override
    public ChannelBuffer writeBytes(ChannelBuffer src, int srcIndex, int length) {
        byte[] data = new byte[length];
        src.getBytes(srcIndex, data, 0, length);
        writeBytes(data, 0, length);
        return this;
    }

    @Override
    public ChannelBuffer writeBytes(byte[] src) {
        ioBuffer.writeBytes(src);
        return this;
    }

    @Override
    public ChannelBuffer writeBytes(byte[] src, int srcIndex, int length) {
        ioBuffer.writeBytes(src, srcIndex, length);
        return this;
    }

    @Override
    public ChannelBuffer writeBytes(ByteBuffer src) {
        ioBuffer.writeBytes(src);
        return this;
    }

    @Override
    public int writeBytes(InputStream in, int length) throws IOException {
        return ioBuffer.writeBytes(in, length);
    }

    @Override
    public int writeCharSequence(CharSequence sequence, Charset charset) {
        return ioBuffer.writeCharSequence(sequence, charset);
    }

    @Override
    public ChannelBuffer copy() {
        return new NettyChannelBuffer(ioBuffer.copy());
    }

    @Override
    public ChannelBuffer copy(int index, int length) {
        return new NettyChannelBuffer(ioBuffer.copy(index, length));
    }

    @Override
    public ByteBuffer nioBuffer() {
        return ioBuffer.nioBuffer();
    }

    @Override
    public ByteBuffer nioBuffer(int index, int length) {
        return ioBuffer.nioBuffer(index, length);
    }

    @Override
    public boolean hasArray() {
        return ioBuffer.hasArray();
    }

    @Override
    public byte[] array() {
        return ioBuffer.array();
    }

    @Override
    public int arrayOffset() {
        return ioBuffer.arrayOffset();
    }

    @Override
    public int compareTo(ChannelBuffer target) {
        ChannelBuffer bufferA = this;
        ChannelBuffer bufferB = target;
        final int aLen = bufferA.readableBytes();
        final int bLen = bufferB.readableBytes();
        final int minLength = Math.min(aLen, bLen);

        int aIndex = bufferA.readerIndex();
        int bIndex = bufferB.readerIndex();

        for (int i = minLength; i > 0; i--) {
            byte va = bufferA.getByte(aIndex);
            byte vb = bufferB.getByte(bIndex);
            if (va > vb) {
                return 1;
            } else if (va < vb) {
                return -1;
            }
            aIndex++;
            bIndex++;
        }
        return aLen - bLen;
    }

    @Override
    public String toString() {
        return ioBuffer.toString();
    }
}
