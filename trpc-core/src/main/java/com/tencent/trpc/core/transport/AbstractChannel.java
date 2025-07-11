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

package com.tencent.trpc.core.transport;

import com.tencent.trpc.core.exception.TransportException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.utils.FutureUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractChannel implements Channel {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractChannel.class);
    private final AtomicBoolean closedFlag = new AtomicBoolean(false);

    protected abstract CompletionStage<Void> doSend(Object message) throws TransportException;

    protected abstract CompletionStage<Void> doClose();

    @Override
    public CompletionStage<Void> send(Object message) throws TransportException {
        String msgType = (message == null ? "" : message.getClass().getName());
        if (isClosed()) {
            String msgFormat = "Failed to send message(%s), cause: channel(%s) is closed";
            return FutureUtils.failed(TransportException.create(msgFormat, msgType, this));
        }
        if (!isConnected()) {
            String msgFormat = "Failed to send message(%s), cause: channel(%s) is disconnnect";
            return FutureUtils.failed(TransportException.create(msgFormat, msgType, this));
        }
        return doSend(message);
    }

    @Override
    public String toString() {
        return "{" + super.toString() + "-" + getProtocolConfig().getNetwork() + "|"
                + getLocalAddress()
                + " -> " + getRemoteAddress() + "}";
    }

    @Override
    public CompletionStage<Void> close() {
        if (closedFlag.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
            return doClose();
        } else {
            LOG.warn("Close channel(" + this + ")");
            return CompletableFuture.completedFuture(null);
        }
    }

    public boolean isClosed() {
        return closedFlag.get();
    }

}
