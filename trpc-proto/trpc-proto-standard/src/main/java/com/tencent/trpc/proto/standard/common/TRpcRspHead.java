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

import com.google.protobuf.TextFormat;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;

/**
 * TRPC protocol frame header and response header
 */
public class TRpcRspHead {

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Frame header
     */
    private StandardFrame frame;
    /**
     * Response header
     */
    private TRPCProtocol.ResponseProtocol head;

    public TRpcRspHead(StandardFrame frame, TRPCProtocol.ResponseProtocol head) {
        super();
        this.frame = frame;
        this.head = head;
    }

    public TRPCProtocol.ResponseProtocol getHead() {
        return head;
    }

    public void setHead(TRPCProtocol.ResponseProtocol head) {
        this.head = head;
    }

    public StandardFrame getFrame() {
        return frame;
    }

    public void setFrame(StandardFrame frame) {
        this.frame = frame;
    }

    @Override
    public String toString() {
        if (head != null) {
            return "TRpcRspHead  {frame=" + frame + ", head=" + headToString(head) + "}";
        } else {
            return "TRpcRspHead  {frame=" + frame + ", head=<null>" + "}";
        }
    }

    private String headToString(TRPCProtocol.ResponseProtocol head) {
        try {
            if (head == null) {
                return "<null>";
            } else {
                return TextFormat.shortDebugString(head);
            }
        } catch (Exception exception) {
            logger.error("conversion header to string error: ", exception);
            return exception.getMessage();
        }
    }
}
