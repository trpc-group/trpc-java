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

import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcStreamFrameType;

/**
 * TRPC protocol frame type definition, used for trpc frame data encoding and decoding
 */
public enum TRpcFrameType {

    /**
     * Initialization frame
     */
    INIT(TrpcStreamFrameType.TRPC_STREAM_FRAME_INIT_VALUE),
    /**
     * Data frame
     */
    DATA(TrpcStreamFrameType.TRPC_STREAM_FRAME_DATA_VALUE),
    /**
     * Stream control frame
     */
    FEEDBACK(TrpcStreamFrameType.TRPC_STREAM_FRAME_FEEDBACK_VALUE),
    /**
     * Stream close frame
     */
    CLOSE(TrpcStreamFrameType.TRPC_STREAM_FRAME_CLOSE_VALUE);

    /**
     * Frame encoding type
     */
    private final int encodedType;

    TRpcFrameType(int encodedType) {
        this.encodedType = encodedType;
    }

    /**
     * Convert from frame encoding type to frame type。
     *
     * @param encodedType frame encoding type
     * @return frame type
     */
    public static TRpcFrameType fromEncodedType(int encodedType) {
        switch (encodedType) {
            case TrpcStreamFrameType.TRPC_STREAM_FRAME_INIT_VALUE:
                return INIT;
            case TrpcStreamFrameType.TRPC_STREAM_FRAME_DATA_VALUE:
                return DATA;
            case TrpcStreamFrameType.TRPC_STREAM_FRAME_FEEDBACK_VALUE:
                return FEEDBACK;
            case TrpcStreamFrameType.TRPC_STREAM_FRAME_CLOSE_VALUE:
                return CLOSE;
            default:
                throw new IllegalArgumentException("unknown encodedType " + encodedType);
        }
    }

    /**
     * Get frame encoding type
     *
     * @return frame encoding type
     */
    public int getEncodedType() {
        return encodedType;
    }
}
