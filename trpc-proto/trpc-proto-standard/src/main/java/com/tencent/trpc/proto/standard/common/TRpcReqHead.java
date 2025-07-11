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
import com.tencent.trpc.proto.standard.common.TRPCProtocol.RequestProtocol;

/**
 * TRPC protocol frame header and request header
 */
public class TRpcReqHead {

    private Logger logger = LoggerFactory.getLogger(getClass());

    // frame header
    protected StandardFrame frame;
    // request header
    protected TRPCProtocol.RequestProtocol head;

    public TRpcReqHead(StandardFrame frame, RequestProtocol head) {
        super();
        this.frame = frame;
        this.head = head;
    }

    public StandardFrame getFrame() {
        return frame;
    }

    public void setFrame(StandardFrame frame) {
        this.frame = frame;
    }

    public TRPCProtocol.RequestProtocol getHead() {
        return head;
    }

    public void setHead(TRPCProtocol.RequestProtocol head) {
        this.head = head;
    }

    @Override
    public String toString() {
        if (head != null) {
            return "TRpcReqHead  {frame=" + frame + ", head=" + headToString(head) + "}";
        } else {
            return "TRpcReqHead  {frame=" + frame + ", head=<null>" + "}";
        }
    }

    private String headToString(TRPCProtocol.RequestProtocol head) {
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
