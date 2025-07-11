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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * refer to {bytebuf} of netty 4
 */
public abstract class ChannelBuffer implements Comparable<ChannelBuffer> {

    /**
     * Returns the number of bytes (octets) this buffer can contain.
     */
    public abstract int capacity();

    /**
     * Adjusts the capacity of this buffer. If the {@code newCapacity} is less than the current
     * capacity, the content of this buffer is truncated. If the {@code newCapacity} is greater than
     * the current capacity, the buffer is appended with unspecified data whose length is {@code
     * (newCapacity - currentCapacity)}.
     *
     * @return this
     */
    public abstract ChannelBuffer capacity(int newCapacity);

    /**
     * Returns the maximum allowed capacity of this buffer. If a user attempts to increase the
     * capacity of this buffer beyond the maximum capacity using {@link #capacity(int)} or {@link
     * #ensureWritable(int)}, those methods will raise an {@link IllegalArgumentException}.
     */
    public abstract int maxCapacity();

    /**
     * Returns {@code true} if and only if this buffer is backed by an NIO direct buffer.
     */
    public abstract boolean isDirect();

    /**
     * Returns {@code true} if and only if this buffer is read-only.
     */
    public abstract boolean isReadOnly();

    /**
     * Returns a read-only version of this buffer.
     */
    public abstract ChannelBuffer asReadOnly();

    /**
     * Returns the {@code readerIndex} of this buffer.
     */
    public abstract int readerIndex();

    /**
     * Sets the {@code readerIndex} of this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code readerIndex} is less than {@code 0}
     *         or greater than {@code this.writerIndex}
     */
    public abstract ChannelBuffer readerIndex(int readerIndex);

    /**
     * Returns the {@code writerIndex} of this buffer.
     */
    public abstract int writerIndex();

    /**
     * Sets the {@code writerIndex} of this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code writerIndex} is less than {@code
     *         this.readerIndex} or greater than {@code this.capacity}
     */
    public abstract ChannelBuffer writerIndex(int writerIndex);

    /**
     * <pre>
     * if (readerIndex < 0 || readerIndex > writerIndex || writerIndex > capacity()) {
     *   throw new IndexOutOfBoundsException();
     * }
     * </pre>
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code readerIndex} is less than 0, if the
     *         specified {@code writerIndex} is less than the specified {@code readerIndex}
     *         or if the specified {@code writerIndex} is greater than {@code this.capacity}
     */
    public abstract ChannelBuffer setIndex(int readerIndex, int writerIndex);

    /**
     * Returns the number of readable bytes which is equal to {@code (this.writerIndex -
     * this.readerIndex)}.
     */
    public abstract int readableBytes();

    /**
     * Returns the number of writable bytes which is equal to {@code (this.capacity -
     * this.writerIndex)}.
     */
    public abstract int writableBytes();

    /**
     * Returns the maximum possible number of writable bytes, which is equal to {@code
     * (this.maxCapacity - this.writerIndex)}.
     */
    public abstract int maxWritableBytes();

    /**
     * Returns {@code true} if and only if {@code (this.writerIndex - this.readerIndex)} is greater
     * than {@code 0}.
     */
    public abstract boolean isReadable();

    /**
     * Returns {@code true} if and only if this buffer contains equal to or more than the specified
     * number of elements.
     */
    public abstract boolean isReadable(int size);

    /**
     * Returns {@code true} if and only if {@code (this.capacity - this.writerIndex)} is greater
     * than {@code 0}.
     */
    public abstract boolean isWritable();

    /**
     * Returns {@code true} if and only if this buffer has enough room to allow writing the
     * specified number of elements.
     */
    public abstract boolean isWritable(int size);

    /**
     * Sets the {@code readerIndex} and {@code writerIndex} of this buffer to {@code 0}. This method
     * is identical to {@link #setIndex(int, int) setIndex(0, 0)}.
     * Please note that the behavior of this method is different from that of NIO buffer, which sets
     * the {@code limit} to the {@code capacity} of the buffer.
     *
     * @return this
     */
    public abstract ChannelBuffer clear();

    /**
     * Marks the current {@code readerIndex} in this buffer. You can reposition the current {@code
     * readerIndex} to the marked {@code readerIndex} by calling {@link #resetReaderIndex()}. The
     * initial value of the marked {@code readerIndex} is {@code 0}.
     *
     * @return this
     */
    public abstract ChannelBuffer markReaderIndex();

    /**
     * Repositions the current {@code readerIndex} to the marked {@code readerIndex} in this
     * buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the current {@code writerIndex} is less than the marked
     *         {@code readerIndex}
     */
    public abstract ChannelBuffer resetReaderIndex();

    /**
     * Marks the current {@code writerIndex} in this buffer. You can reposition the current {@code
     * writerIndex} to the marked {@code writerIndex} by calling {@link #resetWriterIndex()}. The
     * initial value of the marked {@code writerIndex} is {@code 0}.
     *
     * @return this
     */
    public abstract ChannelBuffer markWriterIndex();

    /**
     * Repositions the current {@code writerIndex} to the marked {@code writerIndex} in this
     * buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the current {@code readerIndex} is greater than the
     *         marked {@code writerIndex}
     */
    public abstract ChannelBuffer resetWriterIndex();

    /**
     * Discards the bytes between the 0th index and {@code readerIndex}. It moves the bytes between
     * {@code readerIndex} and {@code writerIndex} to the 0th index, and sets {@code readerIndex}
     * and {@code writerIndex} to {@code 0} and {@code oldWriterIndex - oldReaderIndex}
     * respectively.
     * Please refer to the class documentation for more detailed explanation.
     *
     * @return this
     */
    public abstract ChannelBuffer discardReadBytes();

    /**
     * Makes sure the number of {@linkplain #writableBytes() the writable bytes} is equal to or
     * greater than the specified value. If there is enough writable bytes in this buffer, this
     * method returns with no side effect. Otherwise, it raises an {@link
     * IllegalArgumentException}.
     *
     * @param minWritableBytes the expected minimum number of writable bytes
     * @return this
     * @throws IndexOutOfBoundsException if {@link #writerIndex()} + {@code minWritableBytes} &gt;
     *         {@link #maxCapacity()}
     */
    public abstract ChannelBuffer ensureWritable(int minWritableBytes);

    /**
     * Gets a boolean at the specified absolute (@code index) in this buffer. This method does not
     * modify the {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     */
    public abstract boolean getBoolean(int index);

    /**
     * Gets a byte at the specified absolute {@code index} in this buffer. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     */
    public abstract byte getByte(int index);

    /**
     * Gets an unsigned byte at the specified absolute {@code index} in this buffer. This method
     * does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     */
    public abstract short getUnsignedByte(int index);

    /**
     * Gets a 16-bit short integer at the specified absolute {@code index} in this buffer. This
     * method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */
    public abstract short getShort(int index);

    /**
     * Gets a 16-bit short integer at the specified absolute {@code index} in this buffer in Little
     * Endian Byte Order. This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */
    public abstract short getShortLE(int index);

    /**
     * Gets an unsigned 16-bit short integer at the specified absolute {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */
    public abstract int getUnsignedShort(int index);

    /**
     * Gets an unsigned 16-bit short integer at the specified absolute {@code index} in this buffer
     * in Little Endian Byte Order. This method does not modify {@code readerIndex} or {@code
     * writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */
    public abstract int getUnsignedShortLE(int index);

    /**
     * Gets a 32-bit integer at the specified absolute {@code index} in this buffer. This method
     * does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public abstract int getInt(int index);

    /**
     * Gets a 32-bit integer at the specified absolute {@code index} in this buffer with Little
     * Endian Byte Order. This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public abstract int getIntLE(int index);

    /**
     * Gets an unsigned 32-bit integer at the specified absolute {@code index} in this buffer. This
     * method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public abstract long getUnsignedInt(int index);

    /**
     * Gets an unsigned 32-bit integer at the specified absolute {@code index} in this buffer in
     * Little Endian Byte Order. This method does not modify {@code readerIndex} or {@code
     * writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public abstract long getUnsignedIntLE(int index);

    /**
     * Gets a 64-bit long integer at the specified absolute {@code index} in this buffer. This
     * method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public abstract long getLong(int index);

    /**
     * Gets a 64-bit long integer at the specified absolute {@code index} in this buffer in Little
     * Endian Byte Order. This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public abstract long getLongLE(int index);

    /**
     * Gets a 2-byte UTF-16 character at the specified absolute {@code index} in this buffer. This
     * method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */
    public abstract char getChar(int index);

    /**
     * Gets a 32-bit floating point number at the specified absolute {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public abstract float getFloat(int index);

    /**
     * Gets a 32-bit floating point number at the specified absolute {@code index} in this buffer in
     * Little Endian Byte Order. This method does not modify {@code readerIndex} or {@code
     * writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public float getFloatLE(int index) {
        return Float.intBitsToFloat(getIntLE(index));
    }

    /**
     * Gets a 64-bit floating point number at the specified absolute {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public abstract double getDouble(int index);

    /**
     * Gets a 64-bit floating point number at the specified absolute {@code index} in this buffer in
     * Little Endian Byte Order. This method does not modify {@code readerIndex} or {@code
     * writerIndex} of this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public double getDoubleLE(int index) {
        return Double.longBitsToDouble(getLongLE(index));
    }

    /**
     * Transfers this buffer's data to the specified destination starting at the specified absolute
     * {@code index} until the destination becomes non-writable. This method is basically same with
     * {@link #getBytes(int, ChannelBuffer, int, int)}, except that this method increases the {@code
     * writerIndex} of the destination by the number of the transferred bytes while {@link
     * #getBytes(int, ChannelBuffer, int, int)} does not. This method does not modify {@code
     * readerIndex} or {@code writerIndex} of the source buffer (i.e. {@code this}).
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or if
     *         {@code index + dst.writableBytes} is greater than {@code this.capacity}
     */
    public abstract ChannelBuffer getBytes(int index, ChannelBuffer dst);

    /**
     * Transfers this buffer's data to the specified destination starting at the specified absolute
     * {@code index}. This method is basically same with {@link #getBytes(int, ChannelBuffer, int,
     * int)}, except that this method increases the {@code writerIndex} of the destination by the
     * number of the transferred bytes while {@link #getBytes(int, ChannelBuffer, int, int)} does
     * not. This method does not modify {@code readerIndex} or {@code writerIndex} of the source
     * buffer (i.e. {@code this}).
     *
     * @param length the number of bytes to transfer
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0}, if
     *         {@code index + length} is greater than {@code this.capacity},
     *         or if {@code length} is greater than {@code dst.writableBytes}
     */
    public abstract ChannelBuffer getBytes(int index, ChannelBuffer dst, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at the specified absolute
     * {@code index}. This method does not modify {@code readerIndex} or {@code writerIndex} of both
     * the source (i.e. {@code this}) and the destination.
     *
     * @param dstIndex the first index of the destination
     * @param length the number of bytes to transfer
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     *         if the specified {@code dstIndex} is less than {@code 0},
     *         if {@code index + length} is greater than {@code this.capacity},
     *         or if {@code dstIndex + length} is greater than {@code dst.capacity}
     */
    public abstract ChannelBuffer getBytes(int index, ChannelBuffer dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at the specified absolute
     * {@code index}. This method does not modify {@code readerIndex} or {@code writerIndex} of this
     * buffer
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or if
     *         {@code index + dst.length} is greater than {@code this.capacity}
     */
    public abstract ChannelBuffer getBytes(int index, byte[] dst);

    /**
     * Transfers this buffer's data to the specified destination starting at the specified absolute
     * {@code index}. This method does not modify {@code readerIndex} or {@code writerIndex} of this
     * buffer.
     *
     * @param dstIndex the first index of the destination
     * @param length the number of bytes to transfer
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     *         if the specified {@code dstIndex} is less than {@code 0},
     *         if {@code index + length} is greater than {@code this.capacity},
     *         or if {@code dstIndex + length} is greater than {@code dst.length}
     */
    public abstract ChannelBuffer getBytes(int index, byte[] dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at the specified absolute
     * {@code index} until the destination's position reaches its limit. This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer while the destination's {@code
     * position} will be increased.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or if
     *         {@code index + dst.remaining()} is greater than {@code this.capacity}
     */
    public abstract ChannelBuffer getBytes(int index, ByteBuffer dst);

    /**
     * Transfers this buffer's data to the specified stream starting at the specified absolute
     * {@code index}. This method does not modify {@code readerIndex} or {@code writerIndex} of this
     * buffer.
     *
     * @param length the number of bytes to transfer
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or if
     *         {@code index + length} is greater than {@code this.capacity}
     * @throws IOException if the specified stream threw an exception during I/O
     */
    public abstract ChannelBuffer getBytes(int index, OutputStream out, int length)
            throws IOException;

    /**
     * Gets a {@link CharSequence} with the given length at the given index.
     *
     * @param length the length to read
     * @param charset that should be used
     * @return the sequence
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code
     *         this.readableBytes}
     */
    public abstract CharSequence getCharSequence(int index, int length, Charset charset);

    /**
     * Sets the specified boolean at the specified absolute {@code index} in this buffer. This
     * method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     */
    public abstract ChannelBuffer setBoolean(int index, boolean value);

    /**
     * Sets the specified byte at the specified absolute {@code index} in this buffer. The 24
     * high-order bits of the specified value are ignored. This method does not modify {@code
     * readerIndex} or {@code writerIndex} of this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 1} is greater than {@code this.capacity}
     */
    public abstract ChannelBuffer setByte(int index, int value);

    /**
     * Sets the specified 16-bit short integer at the specified absolute {@code index} in this
     * buffer. The 16 high-order bits of the specified value are ignored. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */
    public abstract ChannelBuffer setShort(int index, int value);

    /**
     * Sets the specified 16-bit short integer at the specified absolute {@code index} in this
     * buffer with the Little Endian Byte Order. The 16 high-order bits of the specified value are
     * ignored. This method does not modify {@code readerIndex} or {@code writerIndex} of this
     * buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */
    public abstract ChannelBuffer setShortLE(int index, int value);

    /**
     * Sets the specified 32-bit integer at the specified absolute {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public abstract ChannelBuffer setInt(int index, int value);

    /**
     * Sets the specified 32-bit integer at the specified absolute {@code index} in this buffer with
     * Little Endian byte order . This method does not modify {@code readerIndex} or {@code
     * writerIndex} of this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public abstract ChannelBuffer setIntLE(int index, int value);

    /**
     * Sets the specified 64-bit long integer at the specified absolute {@code index} in this
     * buffer. This method does not modify {@code readerIndex} or {@code writerIndex} of this
     * buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public abstract ChannelBuffer setLong(int index, long value);

    /**
     * Sets the specified 64-bit long integer at the specified absolute {@code index} in this buffer
     * in Little Endian Byte Order. This method does not modify {@code readerIndex} or {@code
     * writerIndex} of this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public abstract ChannelBuffer setLongLE(int index, long value);

    /**
     * Sets the specified 2-byte UTF-16 character at the specified absolute {@code index} in this
     * buffer. The 16 high-order bits of the specified value are ignored. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 2} is greater than {@code this.capacity}
     */
    public abstract ChannelBuffer setChar(int index, int value);

    /**
     * Sets the specified 32-bit floating-point number at the specified absolute {@code index} in
     * this buffer. This method does not modify {@code readerIndex} or {@code writerIndex} of this
     * buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public abstract ChannelBuffer setFloat(int index, float value);

    /**
     * Sets the specified 32-bit floating-point number at the specified absolute {@code index} in
     * this buffer in Little Endian Byte Order. This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 4} is greater than {@code this.capacity}
     */
    public ChannelBuffer setFloatLE(int index, float value) {
        return setIntLE(index, Float.floatToRawIntBits(value));
    }

    /**
     * Sets the specified 64-bit floating-point number at the specified absolute {@code index} in
     * this buffer. This method does not modify {@code readerIndex} or {@code writerIndex} of this
     * buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public abstract ChannelBuffer setDouble(int index, double value);

    /**
     * Sets the specified 64-bit floating-point number at the specified absolute {@code index} in
     * this buffer in Little Endian Byte Order. This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *         {@code index + 8} is greater than {@code this.capacity}
     */
    public ChannelBuffer setDoubleLE(int index, double value) {
        return setLongLE(index, Double.doubleToRawLongBits(value));
    }

    /**
     * Transfers the specified source buffer's data to this buffer starting at the specified
     * absolute {@code index} until the source buffer becomes unreadable. This method is basically
     * same with {@link #setBytes(int, ChannelBuffer, int, int)}, except that this method increases
     * the {@code readerIndex} of the source buffer by the number of the transferred bytes while
     * {@link #setBytes(int, ChannelBuffer, int, int)} does not. This method does not modify {@code
     * readerIndex} or {@code writerIndex} of the source buffer (i.e. {@code this}).
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or if
     *         {@code index + src.readableBytes} is greater than {@code this.capacity}
     */
    public abstract ChannelBuffer setBytes(int index, ChannelBuffer src);

    /**
     * Transfers the specified source buffer's data to this buffer starting at the specified
     * absolute {@code index}. This method is basically same with {@link #setBytes(int,
     * ChannelBuffer, int, int)}, except that this method increases the {@code readerIndex} of the
     * source buffer by the number of the transferred bytes while {@link #setBytes(int,
     * ChannelBuffer, int, int)} does not. This method does not modify {@code readerIndex} or {@code
     * writerIndex} of the source buffer (i.e. {@code this}).
     *
     * @param length the number of bytes to transfer
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     *         if {@code index + length} is greater than {@code this.capacity},
     *         or if {@code length} is greater than {@code src.readableBytes}
     */
    public abstract ChannelBuffer setBytes(int index, ChannelBuffer src, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at the specified
     * absolute {@code index}. This method does not modify {@code readerIndex} or {@code
     * writerIndex} of both the source (i.e. {@code this}) and the destination.
     *
     * @param srcIndex the first index of the source
     * @param length the number of bytes to transfer
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     *         if the specified {@code srcIndex} is less than {@code 0},
     *         if {@code index + length} is greater than {@code this.capacity},
     *         or if {@code srcIndex + length} is greater than {@code src.capacity}
     */
    public abstract ChannelBuffer setBytes(int index, ChannelBuffer src, int srcIndex, int length);

    /**
     * Transfers the specified source array's data to this buffer starting at the specified absolute
     * {@code index}. This method does not modify {@code readerIndex} or {@code writerIndex} of this
     * buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or if
     *         {@code index + src.length} is greater than {@code this.capacity}
     */
    public abstract ChannelBuffer setBytes(int index, byte[] src);

    /**
     * Transfers the specified source array's data to this buffer starting at the specified absolute
     * {@code index}. This method does not modify {@code readerIndex} or {@code writerIndex} of this
     * buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     *         if the specified {@code srcIndex} is less than {@code 0},
     *         if {@code index + length} is greater than {@code this.capacity},
     *         or if {@code srcIndex + length} is greater than {@code src.length}
     */
    public abstract ChannelBuffer setBytes(int index, byte[] src, int srcIndex, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at the specified
     * absolute {@code index} until the source buffer's position reaches its limit. This method does
     * not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or if
     *         {@code index + src.remaining()} is greater than {@code this.capacity}
     */
    public abstract ChannelBuffer setBytes(int index, ByteBuffer src);

    /**
     * Transfers the content of the specified source stream to this buffer starting at the specified
     * absolute {@code index}. This method does not modify {@code readerIndex} or {@code
     * writerIndex} of this buffer.
     *
     * @param length the number of bytes to transfer
     * @return the actual number of bytes read in from the specified channel. {@code -1} if the
     *         specified channel is closed.
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or if
     *         {@code index + length} is greater than {@code this.capacity}
     * @throws IOException if the specified stream threw an exception during I/O
     */
    public abstract int setBytes(int index, InputStream in, int length) throws IOException;

    /**
     * Writes the specified {@link CharSequence} at the current {@code writerIndex} and increases
     * the {@code writerIndex} by the written bytes.
     *
     * @param index on which the sequence should be written
     * @param sequence to write
     * @param charset that should be used.
     * @return the written number of bytes.
     * @throws IndexOutOfBoundsException if {@code this.writableBytes} is not large enough to write
     *         the whole sequence
     */
    public abstract int setCharSequence(int index, CharSequence sequence, Charset charset);

    /**
     * Gets a boolean at the current {@code readerIndex} and increases the {@code readerIndex} by
     * {@code 1} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 1}
     */
    public abstract boolean readBoolean();

    /**
     * Gets a byte at the current {@code readerIndex} and increases the {@code readerIndex} by
     * {@code 1} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 1}
     */
    public abstract byte readByte();

    /**
     * Gets an unsigned byte at the current {@code readerIndex} and increases the {@code
     * readerIndex} by {@code 1} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 1}
     */
    public abstract short readUnsignedByte();

    /**
     * Gets a 16-bit short integer at the current {@code readerIndex} and increases the {@code
     * readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 2}
     */
    public abstract short readShort();

    /**
     * Gets a 16-bit short integer at the current {@code readerIndex} in the Little Endian Byte
     * Order and increases the {@code readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 2}
     */
    public abstract short readShortLE();

    /**
     * Gets an unsigned 16-bit short integer at the current {@code readerIndex} and increases the
     * {@code readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 2}
     */
    public abstract int readUnsignedShort();

    /**
     * Gets an unsigned 16-bit short integer at the current {@code readerIndex} in the Little Endian
     * Byte Order and increases the {@code readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 2}
     */
    public abstract int readUnsignedShortLE();

    /**
     * Gets a 32-bit integer at the current {@code readerIndex} and increases the {@code
     * readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 4}
     */
    public abstract int readInt();

    /**
     * Gets a 32-bit integer at the current {@code readerIndex} in the Little Endian Byte Order and
     * increases the {@code readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 4}
     */
    public abstract int readIntLE();

    /**
     * Gets an unsigned 32-bit integer at the current {@code readerIndex} and increases the {@code
     * readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 4}
     */
    public abstract long readUnsignedInt();

    /**
     * Gets an unsigned 32-bit integer at the current {@code readerIndex} in the Little Endian Byte
     * Order and increases the {@code readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 4}
     */
    public abstract long readUnsignedIntLE();

    /**
     * Gets a 64-bit integer at the current {@code readerIndex} and increases the {@code
     * readerIndex} by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 8}
     */
    public abstract long readLong();

    /**
     * Gets a 64-bit integer at the current {@code readerIndex} in the Little Endian Byte Order and
     * increases the {@code readerIndex} by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 8}
     */
    public abstract long readLongLE();

    /**
     * Gets a 2-byte UTF-16 character at the current {@code readerIndex} and increases the {@code
     * readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 2}
     */
    public abstract char readChar();

    /**
     * Gets a 32-bit floating point number at the current {@code readerIndex} and increases the
     * {@code readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 4}
     */
    public abstract float readFloat();

    /**
     * Gets a 32-bit floating point number at the current {@code readerIndex} in Little Endian Byte
     * Order and increases the {@code readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 4}
     */
    public float readFloatLE() {
        return Float.intBitsToFloat(readIntLE());
    }

    /**
     * Gets a 64-bit floating point number at the current {@code readerIndex} and increases the
     * {@code readerIndex} by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 8}
     */
    public abstract double readDouble();

    /**
     * Gets a 64-bit floating point number at the current {@code readerIndex} in Little Endian Byte
     * Order and increases the {@code readerIndex} by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 8}
     */
    public double readDoubleLE() {
        return Double.longBitsToDouble(readLongLE());
    }

    /**
     * Transfers this buffer's data to a newly created buffer starting at the current {@code
     * readerIndex} and increases the {@code readerIndex} by the number of the transferred bytes (=
     * {@code length}). The returned buffer's {@code readerIndex} and {@code writerIndex} are {@code
     * 0} and {@code length} respectively.
     *
     * @param length the number of bytes to transfer
     * @return the newly created buffer which contains the transferred bytes
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code
     *         this.readableBytes}
     */
    public abstract ChannelBuffer readBytes(int length);

    /**
     * Transfers this buffer's data to the specified destination starting at the current {@code
     * readerIndex} until the destination becomes non-writable, and increases the {@code
     * readerIndex} by the number of the transferred bytes. This method is basically same with
     * {@link #readBytes(ChannelBuffer, int, int)}, except that this method increases the {@code
     * writerIndex} of the destination by the number of the transferred bytes while {@link
     * #readBytes(ChannelBuffer, int, int)} does not.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code dst.writableBytes} is greater than {@code
     *         this.readableBytes}
     */
    public abstract ChannelBuffer readBytes(ChannelBuffer dst);

    /**
     * Transfers this buffer's data to the specified destination starting at the current {@code
     * readerIndex} and increases the {@code readerIndex} by the number of the transferred bytes (=
     * {@code length}). This method is basically same with {@link #readBytes(ChannelBuffer, int,
     * int)}, except that this method increases the {@code writerIndex} of the destination by the
     * number of the transferred bytes (= {@code length}) while {@link #readBytes(ChannelBuffer,
     * int, int)} does not.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.readableBytes}
     *         or if {@code length} is greater than {@code dst.writableBytes}
     */
    public abstract ChannelBuffer readBytes(ChannelBuffer dst, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at the current {@code
     * readerIndex} and increases the {@code readerIndex} by the number of the transferred bytes (=
     * {@code length}).
     *
     * @param dstIndex the first index of the destination
     * @param length the number of bytes to transfer
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code dstIndex} is less than {@code 0},
     *         if {@code length} is greater than {@code this.readableBytes},
     *         or if {@code dstIndex + length} is greater than {@code dst.capacity}
     */
    public abstract ChannelBuffer readBytes(ChannelBuffer dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at the current {@code
     * readerIndex} and increases the {@code readerIndex} by the number of the transferred bytes (=
     * {@code dst.length}).
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code dst.length} is greater than {@code
     *         this.readableBytes}
     */
    public abstract ChannelBuffer readBytes(byte[] dst);

    /**
     * Transfers this buffer's data to the specified destination starting at the current {@code
     * readerIndex} and increases the {@code readerIndex} by the number of the transferred bytes (=
     * {@code length}).
     *
     * @param dstIndex the first index of the destination
     * @param length the number of bytes to transfer
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code dstIndex} is less than {@code 0},
     *         if {@code length} is greater than {@code this.readableBytes},
     *         or if {@code dstIndex + length} is greater than {@code dst.length}
     */
    public abstract ChannelBuffer readBytes(byte[] dst, int dstIndex, int length);

    /**
     * Transfers this buffer's data to the specified destination starting at the current {@code
     * readerIndex} until the destination's position reaches its limit, and increases the {@code
     * readerIndex} by the number of the transferred bytes.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code dst.remaining()} is greater than {@code
     *         this.readableBytes}
     */
    public abstract ChannelBuffer readBytes(ByteBuffer dst);

    /**
     * Transfers this buffer's data to the specified stream starting at the current {@code
     * readerIndex}.
     *
     * @param length the number of bytes to transfer
     * @return this
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code
     *         this.readableBytes}
     * @throws IOException if the specified stream threw an exception during I/O
     */
    public abstract ChannelBuffer readBytes(OutputStream out, int length) throws IOException;

    /**
     * Gets a {@link CharSequence} with the given length at the current {@code readerIndex} and
     * increases the {@code readerIndex} by the given length.
     *
     * @param length the length to read
     * @param charset that should be used
     * @return the sequence
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code
     *         this.readableBytes}
     */
    public abstract CharSequence readCharSequence(int length, Charset charset);

    /**
     * Increases the current {@code readerIndex} by the specified {@code length} in this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code
     *         this.readableBytes}
     */
    public abstract ChannelBuffer skipBytes(int length);

    /**
     * Sets the specified boolean at the current {@code writerIndex} and increases the {@code
     * writerIndex} by {@code 1} in this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code this.writableBytes} is less than {@code 1}
     */
    public abstract ChannelBuffer writeBoolean(boolean value);

    /**
     * Sets the specified byte at the current {@code writerIndex} and increases the {@code
     * writerIndex} by {@code 1} in this buffer. The 24 high-order bits of the specified value are
     * ignored.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code this.writableBytes} is less than {@code 1}
     */
    public abstract ChannelBuffer writeByte(int value);

    /**
     * Sets the specified 16-bit short integer at the current {@code writerIndex} and increases the
     * {@code writerIndex} by {@code 2} in this buffer. The 16 high-order bits of the specified
     * value are ignored.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code this.writableBytes} is less than {@code 2}
     */
    public abstract ChannelBuffer writeShort(int value);

    /**
     * Sets the specified 16-bit short integer in the Little Endian Byte Order at the current {@code
     * writerIndex} and increases the {@code writerIndex} by {@code 2} in this buffer. The 16
     * high-order bits of the specified value are ignored.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code this.writableBytes} is less than {@code 2}
     */
    public abstract ChannelBuffer writeShortLE(int value);

    /**
     * Sets the specified 32-bit integer at the current {@code writerIndex} and increases the {@code
     * writerIndex} by {@code 4} in this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code this.writableBytes} is less than {@code 4}
     */
    public abstract ChannelBuffer writeInt(int value);

    /**
     * Sets the specified 32-bit integer at the current {@code writerIndex} in the Little Endian
     * Byte Order and increases the {@code writerIndex} by {@code 4} in this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code this.writableBytes} is less than {@code 4}
     */
    public abstract ChannelBuffer writeIntLE(int value);

    /**
     * Sets the specified 64-bit long integer at the current {@code writerIndex} and increases the
     * {@code writerIndex} by {@code 8} in this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code this.writableBytes} is less than {@code 8}
     */
    public abstract ChannelBuffer writeLong(long value);

    /**
     * Sets the specified 64-bit long integer at the current {@code writerIndex} in the Little
     * Endian Byte Order and increases the {@code writerIndex} by {@code 8} in this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code this.writableBytes} is less than {@code 8}
     */
    public abstract ChannelBuffer writeLongLE(long value);

    /**
     * Sets the specified 2-byte UTF-16 character at the current {@code writerIndex} and increases
     * the {@code writerIndex} by {@code 2} in this buffer. The 16 high-order bits of the specified
     * value are ignored.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code this.writableBytes} is less than {@code 2}
     */
    public abstract ChannelBuffer writeChar(int value);

    /**
     * Sets the specified 32-bit floating point number at the current {@code writerIndex} and
     * increases the {@code writerIndex} by {@code 4} in this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code this.writableBytes} is less than {@code 4}
     */
    public abstract ChannelBuffer writeFloat(float value);

    /**
     * Sets the specified 32-bit floating point number at the current {@code writerIndex} in Little
     * Endian Byte Order and increases the {@code writerIndex} by {@code 4} in this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code this.writableBytes} is less than {@code 4}
     */
    public ChannelBuffer writeFloatLE(float value) {
        return writeIntLE(Float.floatToRawIntBits(value));
    }

    /**
     * Sets the specified 64-bit floating point number at the current {@code writerIndex} and
     * increases the {@code writerIndex} by {@code 8} in this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code this.writableBytes} is less than {@code 8}
     */
    public abstract ChannelBuffer writeDouble(double value);

    /**
     * Sets the specified 64-bit floating point number at the current {@code writerIndex} in Little
     * Endian Byte Order and increases the {@code writerIndex} by {@code 8} in this buffer.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code this.writableBytes} is less than {@code 8}
     */
    public ChannelBuffer writeDoubleLE(double value) {
        return writeLongLE(Double.doubleToRawLongBits(value));
    }

    /**
     * Transfers the specified source buffer's data to this buffer starting at the current {@code
     * writerIndex} until the source buffer becomes unreadable, and increases the {@code
     * writerIndex} by the number of the transferred bytes. This method is basically same with
     * {@link #writeBytes(ChannelBuffer, int, int)}, except that this method increases the {@code
     * readerIndex} of the source buffer by the number of the transferred bytes while {@link
     * #writeBytes(ChannelBuffer, int, int)} does not.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code src.readableBytes} is greater than {@code
     *         this.writableBytes}
     */
    public abstract ChannelBuffer writeBytes(ChannelBuffer src);

    /**
     * Transfers the specified source buffer's data to this buffer starting at the current {@code
     * writerIndex} and increases the {@code writerIndex} by the number of the transferred bytes (=
     * {@code length}). This method is basically same with {@link #writeBytes(ChannelBuffer, int,
     * int)}, except that this method increases the {@code readerIndex} of the source buffer by the
     * number of the transferred bytes (= {@code length}) while {@link #writeBytes(ChannelBuffer,
     * int, int)} does not.
     *
     * @param length the number of bytes to transfer
     * @return this
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.writableBytes} or
     *         if {@code length} is greater then {@code src.readableBytes}
     */
    public abstract ChannelBuffer writeBytes(ChannelBuffer src, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at the current {@code
     * writerIndex} and increases the {@code writerIndex} by the number of the transferred bytes (=
     * {@code length}).
     *
     * @param srcIndex the first index of the source
     * @param length the number of bytes to transfer
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code srcIndex} is less than {@code 0},
     *         if {@code srcIndex + length} is greater than {@code src.capacity},
     *         or if {@code length} is greater than {@code this.writableBytes}
     */
    public abstract ChannelBuffer writeBytes(ChannelBuffer src, int srcIndex, int length);

    /**
     * Transfers the specified source array's data to this buffer starting at the current {@code
     * writerIndex} and increases the {@code writerIndex} by the number of the transferred bytes (=
     * {@code src.length}).
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code src.length} is greater than {@code
     *         this.writableBytes}
     */
    public abstract ChannelBuffer writeBytes(byte[] src);

    /**
     * Transfers the specified source array's data to this buffer starting at the current {@code
     * writerIndex} and increases the {@code writerIndex} by the number of the transferred bytes (=
     * {@code length}).
     *
     * @param srcIndex the first index of the source
     * @param length the number of bytes to transfer
     * @return this
     * @throws IndexOutOfBoundsException if the specified {@code srcIndex} is less than {@code 0},
     *         if {@code srcIndex + length} is greater than {@code src.length},
     *         or if {@code length} is greater than {@code this.writableBytes}
     */
    public abstract ChannelBuffer writeBytes(byte[] src, int srcIndex, int length);

    /**
     * Transfers the specified source buffer's data to this buffer starting at the current {@code
     * writerIndex} until the source buffer's position reaches its limit, and increases the {@code
     * writerIndex} by the number of the transferred bytes.
     *
     * @return this
     * @throws IndexOutOfBoundsException if {@code src.remaining()} is greater than {@code
     *         this.writableBytes}
     */
    public abstract ChannelBuffer writeBytes(ByteBuffer src);

    /**
     * Transfers the content of the specified stream to this buffer starting at the current {@code
     * writerIndex} and increases the {@code writerIndex} by the number of the transferred bytes.
     *
     * @param length the number of bytes to transfer
     * @return the actual number of bytes read in from the specified stream
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code
     *         this.writableBytes}
     * @throws IOException if the specified stream threw an exception during I/O
     */
    public abstract int writeBytes(InputStream in, int length) throws IOException;

    /**
     * Writes the specified {@link CharSequence} at the current {@code writerIndex} and increases
     * the {@code writerIndex} by the written bytes. in this buffer.
     *
     * @param sequence to write
     * @param charset that should be used
     * @return the written number of bytes
     * @throws IndexOutOfBoundsException if {@code this.writableBytes} is not large enough to write
     *         the whole sequence
     */
    public abstract int writeCharSequence(CharSequence sequence, Charset charset);

    /**
     * Returns a copy of this buffer's readable bytes. Modifying the content of the returned buffer
     * or this buffer does not affect each other at all. This method is identical to {@code
     * buf.copy(buf.readerIndex(), buf.readableBytes())}. This method does not modify {@code
     * readerIndex} or {@code writerIndex} of this buffer.
     */
    public abstract ChannelBuffer copy();

    /**
     * Returns a copy of this buffer's sub-region. Modifying the content of the returned buffer or
     * this buffer does not affect each other at all. This method does not modify {@code
     * readerIndex} or {@code writerIndex} of this buffer.
     */
    public abstract ChannelBuffer copy(int index, int length);

    /**
     * Exposes this buffer's readable bytes as an NIO {@link ByteBuffer}. The returned buffer either
     * share or contains the copied content of this buffer, while changing the position and limit of
     * the returned NIO buffer does not affect the indexes and marks of this buffer. This method is
     * identical to {@code buf.nioBuffer(buf.readerIndex(), buf.readableBytes())}. This method does
     * not modify {@code readerIndex} or {@code writerIndex} of this buffer. Please note that the
     * returned NIO buffer will not see the changes of this buffer if this buffer is a dynamic
     * buffer and it adjusted its capacity.
     *
     * @throws UnsupportedOperationException if this buffer cannot create a {@link ByteBuffer} that
     *         shares the content with itself
     * @see #nioBufferCount()
     * @see #nioBuffers()
     * @see #nioBuffers(int, int)
     */
    public abstract ByteBuffer nioBuffer();

    /**
     * Exposes this buffer's sub-region as an NIO {@link ByteBuffer}. The returned buffer either
     * share or contains the copied content of this buffer, while changing the position and limit of
     * the returned NIO buffer does not affect the indexes and marks of this buffer. This method
     * does not modify {@code readerIndex} or {@code writerIndex} of this buffer. Please note that
     * the returned NIO buffer will not see the changes of this buffer if this buffer is a dynamic
     * buffer and it adjusted its capacity.
     *
     * @throws UnsupportedOperationException if this buffer cannot create a {@link ByteBuffer} that
     *         shares the content with itself
     * @see #nioBufferCount()
     * @see #nioBuffers()
     * @see #nioBuffers(int, int)
     */
    public abstract ByteBuffer nioBuffer(int index, int length);

    /**
     * Returns {@code true} if and only if this buffer has a backing byte array. If this method
     * returns true, you can safely call {@link #array()} and {@link #arrayOffset()}.
     */
    public abstract boolean hasArray();

    /**
     * Returns the backing byte array of this buffer.
     *
     * @throws UnsupportedOperationException if there no accessible backing byte array
     */
    public abstract byte[] array();

    /**
     * Returns the offset of the first byte within the backing byte array of this buffer.
     *
     * @throws UnsupportedOperationException if there no accessible backing byte array
     */
    public abstract int arrayOffset();

    /**
     * Compares the content of the specified buffer to the content of this buffer. Comparison is
     * performed in the same manner with the string comparison functions of various languages such
     * as {@code strcmp}, {@code memcmp} and {@link String#compareTo(String)}.
     */
    @Override
    public abstract int compareTo(ChannelBuffer buffer);

    /**
     * Returns the string representation of this buffer. This method does not necessarily return the
     * whole content of the buffer but returns the values of the key properties such as {@link
     * #readerIndex()}, {@link #writerIndex()} and {@link #capacity()}.
     */
    @Override
    public abstract String toString();

}
