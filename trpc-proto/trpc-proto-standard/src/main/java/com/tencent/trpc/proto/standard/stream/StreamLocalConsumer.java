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

package com.tencent.trpc.proto.standard.stream;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.stream.transport.RpcConnection;
import com.tencent.trpc.core.utils.PreconditionUtils;
import com.tencent.trpc.proto.standard.stream.codec.TRpcStreamFrameCodec;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class StreamLocalConsumer implements Consumer<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(StreamLocalConsumer.class);

    private final RpcConnection connection;

    private final int streamId;

    // The initialized windowSize of the peer. Zero means no window size and no flow control.
    private final int windowSize;
    private final boolean noFlowControl;

    // Actually consumed window size
    private int consumedWindowSize;

    public StreamLocalConsumer(RpcConnection connection, int streamId, int windowSize) {
        System.out.println(11111);
        this.connection = Objects.requireNonNull(connection, "connection is null");
        PreconditionUtils.checkArgument(windowSize >= 0 && streamId >= 0,
                "create remote flow subscriber failed, windowSize=%d, streamId=%d",
                windowSize, streamId);
        this.streamId = streamId;
        this.windowSize = windowSize;
        this.noFlowControl = windowSize == 0;
    }

    @Override
    public void accept(ByteBuf byteBuf) {
        if (noFlowControl || byteBuf == null) {
            return;
        }
        consumedWindowSize += byteBuf.readableBytes();
        // As aligned with other language of tRpc, here will send increment feedback to peer if this end
        // has already consumed more than quarter of windowSize.
        if (consumedWindowSize >= windowSize / 4) {
            int increment = consumedWindowSize;
            consumedWindowSize = 0;
            feedback(increment);
        }
    }

    private void feedback(int increment) {
        logger.debug("stream {} has consumed {} windowSize, sending feedback", streamId, increment);
        connection.send(TRpcStreamFrameCodec.encodeStreamFeedbackFrame(connection.alloc(), streamId, increment));
    }

}
