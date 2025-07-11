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

package com.tencent.trpc.proto.standard.common;

import java.util.Arrays;

/**
 * TRPC protocol frame header, including frame header size, trpc protocol magic number, data type, end state,
 * total size of trpc protocol, body size, etc.
 */
public class StandardFrame {

    /**
     * Frame header size, 16 bytes
     */
    public static final int FRAME_SIZE = 16;

    /**
     * TRPC protocol magic number
     */
    public static final short TRPC_MAGIC = 0x930;

    /**
     * Magic number, 2 bytes
     */
    private short magic = TRPC_MAGIC;
    /**
     * Data type, 1 byte
     * 0x00 one request one response
     * 0x01 stream
     */
    private byte type;
    /**
     * End state, 1 byte
     * 0x00 not any status
     * 0x01 stream end
     */
    private byte state;
    /**
     * TRPC protocol total size, 4 bytes
     * including: frame header, body header, body, attachment
     */
    private int size;
    /**
     * Body header size, 2 bytes
     * 2 sign bits need to be considered
     */
    private int headSize;
    /**
     * Stream ID, 4 bytes
     */
    private int streamId;
    /**
     * Reserved field, 2 bytes
     */
    private byte[] reserved = {0, 0};


    @Override
    public String toString() {
        return "StandardFrame  {magic=" + magic + ", type=" + type + ", state=" + state + ", size="
                + size + ", headSize=" + headSize + ", streamId=" + streamId + ", reserved="
                + Arrays.toString(reserved) + "}";
    }

    public short getMagic() {
        return magic;
    }

    public StandardFrame setMagic(short magic) {
        this.magic = magic;
        return this;
    }

    public byte getType() {
        return type;
    }

    public StandardFrame setType(byte type) {
        this.type = type;
        return this;
    }

    public byte getState() {
        return state;
    }

    public StandardFrame setState(byte state) {
        this.state = state;
        return this;
    }

    public int getSize() {
        return size;
    }

    public StandardFrame setSize(int size) {
        this.size = size;
        return this;
    }

    public int getHeadSize() {
        return headSize;
    }

    public StandardFrame setHeadSize(int headSize) {
        this.headSize = headSize;
        return this;
    }

    public int getStreamId() {
        return streamId;
    }

    public StandardFrame setStreamId(int streamId) {
        this.streamId = streamId;
        return this;
    }

    public byte[] getReserved() {
        return reserved;
    }

    public StandardFrame setReserved(byte[] reserved) {
        this.reserved = reserved;
        return this;
    }
}
