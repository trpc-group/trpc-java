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

import com.tencent.trpc.core.transport.Channel;

/**
 * Abstract a ChannelBuffer layer, wrap the native I/O channel, make it easy for subsequent upgrades without affecting
 * the business, and avoid memory copying to some extent.
 */
public interface Codec {

    /**
     * In case of insufficient data, return {@link DecodeResult#compareTo(DecodeResult)}
     * Note: In the TCP scenario, when an exception is thrown, the corresponding channel will be closed.
     *
     * @param channel the Channel to encode
     * @param channelBuffer the ChannelBuffer to write the encoded data
     * @param message the message to be encoded
     */
    void encode(Channel channel, ChannelBuffer channelBuffer, Object message);

    /**
     * In case of insufficient data, return {@link DecodeResult#compareTo(DecodeResult)}
     * Note: In the TCP scenario, when an exception is thrown, the corresponding channel will be closed.
     *
     * @param channel the Channel to decode
     * @param channelBuffer the ChannelBuffer containing the data to be decoded
     * @return the decoded object
     */
    Object decode(Channel channel, ChannelBuffer channelBuffer);

    enum DecodeResult {

        /**
         * Return when there is not enough data, do not use exceptions.
         */
        NOT_ENOUGH_DATA;

        public static boolean isNotEnoughData(Object result) {
            return result == DecodeResult.NOT_ENOUGH_DATA;
        }
    }

}
