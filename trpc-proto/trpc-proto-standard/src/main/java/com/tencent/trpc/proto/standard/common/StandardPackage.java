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

package com.tencent.trpc.proto.standard.common;

import com.google.protobuf.InvalidProtocolBufferException;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.codec.ChannelBuffer;
import com.tencent.trpc.core.transport.codec.Codec.DecodeResult;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.RequestProtocol;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.ResponseProtocol;

/**
 * Intermediate encoding and decoding structure for all content of the trpc protocol.
 */
public class StandardPackage {

    /**
     * 0x930 identifies the trpc protocol
     */
    private static final short FRAME_MAGIC = 0x930;
    /**
     * Protocol frame
     */
    private StandardFrame frame = new StandardFrame();
    /**
     * Protocol header
     */
    private byte[] headBytes;
    /**
     * Protocol body
     */
    private byte[] bodyBytes;
    /**
     * Protocol attachment
     */
    private byte[] attachmentBytes;
    /**
     * Request header
     */
    private RequestProtocol requestHead;
    /**
     * Response header
     */
    private ResponseProtocol responseHead;


    /**
     * TRPC protocol decoding
     *
     * @param channel the connected channel
     * @param in the input channelBuffer
     * @param isServer whether it is a server
     * @return the decoded object, StandardPackage instance
     */
    public static Object decode(Channel channel, ChannelBuffer in, boolean isServer) {
        // check if the frame length is sufficient
        if (in.readableBytes() < StandardFrame.FRAME_SIZE) {
            return DecodeResult.NOT_ENOUGH_DATA;
        }
        final short start = in.readShort();
        // check if it is trpc protocol
        if (start != FRAME_MAGIC) {
            throw TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_DECODE_ERR, "the request protocol is not trpc");
        }
        // data type
        final byte type = in.readByte();
        // end state
        final byte state = in.readByte();
        // trpc protocol total size
        int size = in.readInt();
        int payLoadLimit = channel.getProtocolConfig().getPayload();
        // limit packet size
        if (size > payLoadLimit & isServer) {
            throw TRpcException.newFrameException(ErrorCode.TRPC_SERVER_DECODE_ERR,
                    " pkg length > " + payLoadLimit);
        }
        // check if the data length is reasonable
        // header size + stream id size + reserved size = 8
        if (in.readableBytes() - 8 < size - StandardFrame.FRAME_SIZE) {
            return DecodeResult.NOT_ENOUGH_DATA;
        }
        // delay the creation of objects, and then create them after judging that the data is sufficient
        StandardPackage standardPackage = new StandardPackage();
        // header size
        int headSize = in.readUnsignedShort();
        int streamId = in.readInt();
        byte[] reserved = new byte[2];
        in.readBytes(reserved);
        standardPackage.getFrame().setType(type).setState(state)
                .setSize(size).setHeadSize(headSize).setStreamId(streamId).setReserved(reserved);
        // header
        byte[] headBytes = new byte[headSize];
        if (headSize > 0) {
            in.readBytes(headBytes);
        }
        standardPackage.setHeadBytes(headBytes);
        // body size
        int bodySize = parseHead(standardPackage, headBytes, isServer);
        // body
        byte[] bodyBytes = new byte[bodySize];
        if (bodySize > 0) {
            in.readBytes(bodyBytes);
        }
        standardPackage.setBodyBytes(bodyBytes);
        int attachmentSize = getAttachmentSize(standardPackage, isServer);
        if (attachmentSize > 0) {
            // attachment
            byte[] attachment = new byte[attachmentSize];
            in.readBytes(attachment);
            standardPackage.setAttachmentBytes(attachment);
        }
        return standardPackage;
    }

    /**
     * Parse header
     *
     * @param standardPackage trpc protocol instance
     * @param headBytes header
     * @param isServer whether it is a server
     * @return body size
     */
    private static int parseHead(StandardPackage standardPackage, byte[] headBytes, boolean isServer) {
        if (isServer) {
            standardPackage.setRequestHead(parseRequestHeader(headBytes));
            return getSize(standardPackage.getFrame().getSize(), headBytes,
                    standardPackage.getRequestHead().getAttachmentSize());
        }
        standardPackage.setResponseHead(parseResponseHeader(headBytes));
        return getSize(standardPackage.getFrame().getSize(), headBytes,
                standardPackage.getResponseHead().getAttachmentSize());
    }

    private static RequestProtocol parseRequestHeader(byte[] headerBytes) {
        try {
            return RequestProtocol.parseFrom(headerBytes);
        } catch (InvalidProtocolBufferException e) {
            throw TRpcException.newFrameException(ErrorCode.TRPC_SERVER_DECODE_ERR,
                    "TRpc server decode error, parse head exception", e);
        }
    }

    private static ResponseProtocol parseResponseHeader(byte[] headBytes) {
        try {
            return ResponseProtocol.parseFrom(headBytes);
        } catch (InvalidProtocolBufferException e) {
            throw TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_DECODE_ERR,
                    "TRpc client decode error, parse head exception", e);
        }
    }

    private static int getAttachmentSize(StandardPackage standardPackage, boolean isServer) {
        if (isServer) {
            return standardPackage.getRequestHead().getAttachmentSize();
        }
        return standardPackage.getResponseHead().getAttachmentSize();
    }

    /**
     * Get body size or attachment size
     *
     * @param frameSize frame header size
     * @param headBytes header
     * @param size body size or attachment size
     * @return body size or attachment size
     */
    private static int getSize(int frameSize, byte[] headBytes, int size) {
        return frameSize - StandardFrame.FRAME_SIZE
                - headBytes.length - size;
    }

    /**
     * Write the StandardPackage to the channelBuffer
     *
     * @param channelBuffer data channel buffer, used to transfer data
     */
    public void write(ChannelBuffer channelBuffer) {
        channelBuffer.ensureWritable(frame.getSize());
        channelBuffer.writeShort(frame.getMagic());
        channelBuffer.writeByte(frame.getType());
        channelBuffer.writeByte(frame.getState());
        channelBuffer.writeInt(frame.getSize());
        channelBuffer.writeShort(frame.getHeadSize());
        channelBuffer.writeInt(frame.getStreamId());
        channelBuffer.writeBytes(frame.getReserved());
        if (headBytes != null) {
            channelBuffer.writeBytes(this.headBytes);
        }
        if (bodyBytes != null) {
            channelBuffer.writeBytes(this.bodyBytes);
        }
        if (attachmentBytes != null) {
            channelBuffer.writeBytes(this.attachmentBytes);
        }
    }

    public StandardFrame getFrame() {
        return frame;
    }

    public void setFrame(StandardFrame frame) {
        this.frame = frame;
    }

    public byte[] getHeadBytes() {
        return headBytes;
    }

    public void setHeadBytes(byte[] headBytes) {
        this.headBytes = headBytes;
    }

    public byte[] getBodyBytes() {
        return bodyBytes;
    }

    public void setBodyBytes(byte[] bodyBytes) {
        this.bodyBytes = bodyBytes;
    }

    public byte[] getAttachmentBytes() {
        return attachmentBytes;
    }

    /**
     * Set trpc protocol attachments. The total size of trpc protocol is represented by 4 bytes
     * (frame header + header + body + attachment), with a maximum size of 2GB.
     * Therefore, the attachment size must be smaller than 2GB.
     *
     * @param attachmentBytes attachment
     */
    public void setAttachmentBytes(byte[] attachmentBytes) {
        this.attachmentBytes = attachmentBytes;
    }

    public RequestProtocol getRequestHead() {
        return requestHead;
    }

    public void setRequestHead(RequestProtocol requestHead) {
        this.requestHead = requestHead;
    }

    public ResponseProtocol getResponseHead() {
        return responseHead;
    }

    public void setResponseHead(ResponseProtocol responseHead) {
        this.responseHead = responseHead;
    }
}
