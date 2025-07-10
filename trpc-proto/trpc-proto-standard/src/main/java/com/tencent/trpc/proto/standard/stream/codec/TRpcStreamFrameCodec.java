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

package com.tencent.trpc.proto.standard.stream.codec;

import com.google.protobuf.ByteString;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.compressor.CompressorSupport;
import com.tencent.trpc.core.compressor.spi.Compressor;
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcContextValueKeys;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.serialization.SerializationSupport;
import com.tencent.trpc.core.serialization.spi.Serialization;
import com.tencent.trpc.core.utils.PreconditionUtils;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.proto.standard.common.StandardFrame;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcDataFrameType;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcRetCode;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcStreamCloseMeta;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcStreamCloseType;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcStreamFeedBackMeta;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcStreamInitMeta;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcStreamInitRequestMeta;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcStreamInitResponseMeta;
import com.tencent.trpc.proto.standard.common.TRpcFrameType;
import com.tencent.trpc.proto.standard.stream.config.TRpcStreamConstants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * TRPC streaming frame protocol-specific encoding and decoding utility class
 *
 * <p>Only need to create a TRpcStreamFrameCodec instance for encoding and decoding data frames, other frames can be
 * encoded and decoded through static methods.</p>
 */
public class TRpcStreamFrameCodec {

    /**
     * Protocol configuration
     */
    private final ProtocolConfig protocolConfig;
    /**
     * ByteBuf allocator
     */
    private final ByteBufAllocator allocator;
    /**
     * Compressor
     */
    private final Compressor compressor;
    /**
     * Serializer
     */
    private final Serialization serialization;

    /**
     * TRPC streaming frame protocol codec
     *
     * @param protocolConfig protocol configuration for encoding
     * @param allocator byteBuf allocator
     * @param compressType decompress type
     * @param serializeType decoding serialization type
     */
    private TRpcStreamFrameCodec(ProtocolConfig protocolConfig, ByteBufAllocator allocator,
            int compressType, int serializeType) {
        this.protocolConfig = Objects.requireNonNull(protocolConfig, "protocolConfig is null");
        this.allocator = Objects.requireNonNull(allocator, "allocator is null");
        this.compressor = Objects.requireNonNull(CompressorSupport.ofType(compressType),
                "cannot find compressor of type " + compressType);
        this.serialization = Objects.requireNonNull(SerializationSupport.ofType(serializeType),
                "cannot find serialization of type " + serializeType);
    }

    /**
     * Create a data frame codec
     *
     * <p>Data frame encoding and decoding depends on the init frame and the encoding and decoding types configured
     * by ProtocolConfig, which are related to a specific stream. Other frames are in a fixed PB format.</p>
     *
     * @param protocolConfig protocol configuration for encoding
     * @param allocator byteBuf allocator
     * @param compressType decompress type
     * @param serializeType decoding serialization type
     * @return a single stream data frame codec
     */
    public static TRpcStreamFrameCodec newDataFrameCodec(ProtocolConfig protocolConfig, ByteBufAllocator allocator,
            int compressType, int serializeType) {
        return new TRpcStreamFrameCodec(protocolConfig, allocator, compressType, serializeType);
    }

    /**
     * Encode the stream init request message
     *
     * @param allocator byteBuf allocator
     * @param streamId stream id
     * @param windowSize local buffer size
     * @param rpcCallInfo RPC call information
     * @param backendConfig client protocol configuration
     * @return request encoded frame
     */
    public static ByteBuf encodeStreamInitRequestFrame(ByteBufAllocator allocator, int streamId, int windowSize,
            RpcCallInfo rpcCallInfo, BackendConfig backendConfig) {
        TrpcStreamInitRequestMeta.Builder reqBuilder = TrpcStreamInitRequestMeta.newBuilder();
        reqBuilder.setCaller(ByteString.copyFromUtf8(rpcCallInfo.getCaller()));
        reqBuilder.setCallee(ByteString.copyFromUtf8(rpcCallInfo.getCallee()));
        reqBuilder.setFunc(ByteString.copyFromUtf8(rpcCallInfo.getFunc()));

        // get serialization type
        Serialization serialization = SerializationSupport.ofName(backendConfig.getSerialization());
        PreconditionUtils.checkArgument(serialization != null,
                "cannot find serialization of type %s", backendConfig.getSerialization());

        // get compression type
        Compressor compressor = CompressorSupport.ofName(backendConfig.getCompressor());
        PreconditionUtils.checkArgument(compressor != null,
                "cannot find compressor of type %s", backendConfig.getCompressor());

        // add attachments
        rpcCallInfo.getAttachments().forEach((key, val) -> {
            if (val instanceof String) {
                reqBuilder.putTransInfo(key, ByteString.copyFromUtf8((String) val));
            } else if (val instanceof byte[]) {
                reqBuilder.putTransInfo(key, ByteString.copyFrom((byte[]) val));
            }
        });

        TrpcStreamInitMeta initMeta = TrpcStreamInitMeta.newBuilder()
                .setRequestMeta(reqBuilder.build())
                .setInitWindowSize(windowSize)
                .setContentType(serialization.type())
                .setContentEncoding(compressor.type())
                .build();

        return encodeStreamFrame(allocator, streamId, TRpcFrameType.INIT, initMeta.toByteArray());
    }

    /**
     * Encode the stream init response message
     *
     * @param allocator byteBuf allocator
     * @param streamId stream id
     * @param bufSize local buffer size
     * @param protocolConfig protocol config
     * @param ret response code
     * @param msg error message
     * @return response encoded frame
     */
    public static ByteBuf encodeStreamInitResponseFrame(ByteBufAllocator allocator, int streamId, int bufSize,
            ProtocolConfig protocolConfig, int ret, String msg) {
        TrpcStreamInitResponseMeta.Builder rspBuilder = TrpcStreamInitResponseMeta.newBuilder();
        rspBuilder.setRet(ret);
        if (StringUtils.isNotEmpty(msg)) {
            rspBuilder.setErrorMsg(ByteString.copyFromUtf8(msg));
        }

        TrpcStreamInitMeta.Builder initMetaBuilder = TrpcStreamInitMeta.newBuilder()
                .setResponseMeta(rspBuilder.build())
                .setInitWindowSize(bufSize);

        // check serialization and compressor if no exception
        if (ret == TrpcRetCode.TRPC_INVOKE_SUCCESS_VALUE) {
            // get serialization type
            Serialization serialization = SerializationSupport
                    .ofName(protocolConfig.getSerialization());
            PreconditionUtils.checkArgument(serialization != null,
                    "cannot find serialization of type %s", protocolConfig.getSerialization());
            initMetaBuilder.setContentType(serialization.type());

            // get compression type
            Compressor compressor = CompressorSupport.ofName(protocolConfig.getCompressor());
            PreconditionUtils.checkArgument(compressor != null,
                    "cannot find compressor of type %s", protocolConfig.getCompressor());
            initMetaBuilder.setContentEncoding(compressor.type());
        }

        return encodeStreamFrame(allocator, streamId, TRpcFrameType.INIT, initMetaBuilder.build().toByteArray());
    }

    /**
     * Encode the stream feedback message
     *
     * @param allocator byteBuf allocator
     * @param streamId stream id
     * @param increment increment window size
     * @return encoded feedback frame
     */
    public static ByteBuf encodeStreamFeedbackFrame(ByteBufAllocator allocator, int streamId, int increment) {
        TrpcStreamFeedBackMeta.Builder builder = TrpcStreamFeedBackMeta.newBuilder();
        builder.setWindowSizeIncrement(increment);

        return encodeStreamFrame(allocator, streamId, TRpcFrameType.FEEDBACK, builder.build().toByteArray());
    }

    /**
     * Encode the stream close message
     *
     * @param allocator byteBuf allocator
     * @param streamId stream id
     * @param retCode response code
     * @param errMsg error message
     * @return encoded close frame
     */
    public static ByteBuf encodeStreamCloseFrame(ByteBufAllocator allocator, int streamId, int retCode, String errMsg) {
        TrpcStreamCloseMeta.Builder builder = TrpcStreamCloseMeta.newBuilder();
        builder.setCloseType(TrpcStreamCloseType.TRPC_STREAM_CLOSE_VALUE);
        builder.setRet(retCode);
        builder.setMsg(ByteString.copyFromUtf8(errMsg));

        return encodeStreamFrame(allocator, streamId, TRpcFrameType.CLOSE, builder.build().toByteArray());
    }

    /**
     * Encode the stream reset message
     *
     * @param allocator byteBuf allocator
     * @param streamId stream id
     * @param retCode response code
     * @param errMsg error message
     * @return encoded close frame
     */
    public static ByteBuf encodeStreamResetFrame(ByteBufAllocator allocator, int streamId, int retCode, String errMsg) {
        TrpcStreamCloseMeta.Builder builder = TrpcStreamCloseMeta.newBuilder();
        builder.setCloseType(TrpcStreamCloseType.TRPC_STREAM_RESET_VALUE);
        builder.setRet(retCode);
        builder.setMsg(ByteString.copyFromUtf8(errMsg));

        return encodeStreamFrame(allocator, streamId, TRpcFrameType.CLOSE, builder.build().toByteArray());
    }

    /**
     * Encode the stream frame
     *
     * @param streamId stream id
     * @param frameType frame type
     * @param data frame data
     * @return encoded frame
     */
    private static ByteBuf encodeStreamFrame(ByteBufAllocator allocator, int streamId, TRpcFrameType frameType,
            byte[] data) {
        PreconditionUtils.checkArgument(data != null, "frame data is null"); // 流式协议每帧必须有帧数据
        int totalLength = TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH + data.length;
        ByteBuf frame = allocator.buffer(totalLength);
        // write fix head
        frame.writeShort(StandardFrame.TRPC_MAGIC);
        frame.writeByte(TrpcDataFrameType.TRPC_STREAM_FRAME_VALUE);
        frame.writeByte(frameType.getEncodedType());
        frame.writeInt(totalLength);
        frame.writeShort(0);  // nohead
        frame.writeInt(streamId); // stream id
        frame.writeShort(0);  // reserved
        // write data
        frame.writeBytes(data);
        return frame;
    }

    /**
     * Decode the initialization frame
     *
     * @param data frame data
     * @return init data
     */
    public static TrpcStreamInitMeta decodeStreamInitFrame(ByteBuf data) {
        try {
            return TrpcStreamInitMeta.parseFrom(data.nioBuffer());
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid stream init data", e);
        }
    }

    /**
     * Decode the stream feedback frame
     *
     * @param data frame data
     * @return feedback data
     */
    public static TrpcStreamFeedBackMeta decodeStreamFeedbackFrame(ByteBuf data) {
        try {
            return TrpcStreamFeedBackMeta.parseFrom(data.nioBuffer());
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid stream feedback data", e);
        }
    }

    /**
     * Decode the stream close frame
     *
     * @param data frame data
     * @return close frame data
     */
    public static TrpcStreamCloseMeta decodeStreamCloseFrame(ByteBuf data) {
        try {
            return TrpcStreamCloseMeta.parseFrom(data.nioBuffer());
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid stream close data", e);
        }
    }

    /**
     * Parse the RPC call information from the context with attachments.
     *
     * @param ctx context
     * @return call information
     */
    public static RpcCallInfo buildRpcCallInfo(RpcContext ctx) {
        CallInfo callInfo = RpcContextUtils.getValueMapValue(ctx, RpcContextValueKeys.RPC_CALL_INFO_KEY);
        Objects.requireNonNull(callInfo, "callInfo is null");
        RpcInvocation invocation = RpcContextUtils.getValueMapValue(ctx, RpcContextValueKeys.RPC_INVOCATION_KEY);
        Objects.requireNonNull(invocation, "invocation is null");

        String caller = String.format(TRpcStreamConstants.TRPC_CALLER_PATTERN,
                callInfo.getCallerApp(), callInfo.getCallerServer(), callInfo.getCallerService());
        String callee = String.format(TRpcStreamConstants.TRPC_CALLEE_PATTERN,
                callInfo.getCalleeApp(), callInfo.getCalleeServer(), callInfo.getCalleeService());
        String func = String.format(TRpcStreamConstants.TRPC_FUNC_PATTERN,
                invocation.getRpcServiceName(), invocation.getRpcMethodName());

        RpcCallInfo rpcCallInfo = new RpcCallInfo(caller, callee, func);
        rpcCallInfo.getAttachments().putAll(ctx.getReqAttachMap());
        return rpcCallInfo;
    }

    /**
     * Encode the stream data frame
     *
     * @param streamId stream id
     * @param data data
     * @return encoded frame
     */
    public ByteBuf encodeStreamDataFrame(int streamId, Object data) {
        // get serialization type
        String serializationName = this.protocolConfig.getSerialization();
        Serialization serialization = SerializationSupport.ofName(serializationName);
        PreconditionUtils.checkArgument(serialization != null,
                "cannot find serialization of type %s", serializationName);

        // get compression type
        String compressorName = this.protocolConfig.getCompressor();
        Compressor compressor = CompressorSupport.ofName(compressorName);
        PreconditionUtils.checkArgument(compressor != null,
                "cannot find compressor of type %s", compressorName);

        byte[] value;
        try {
            value = serialization.serialize(data);
        } catch (Exception e) {
            throw new IllegalArgumentException("encode stream data failed", e);
        }

        try {
            value = compressor.compress(value);
        } catch (IOException e) {
            throw new IllegalArgumentException(compressor.getClass() + " compress stream data failed", e);
        }

        return encodeStreamFrame(this.allocator, streamId, TRpcFrameType.DATA, value);
    }

    /**
     * Decode the binary body data in the data frame
     *
     * @param data binary data
     * @param clazz body type class
     * @param <T> actual data type
     * @return decoded data body
     */
    public <T> T decodeDataFrameData(ByteBuf data, Class<T> clazz) {
        byte[] value = new byte[data.readableBytes()];
        data.readBytes(value);

        try {
            value = this.compressor.decompress(value);
        } catch (IOException e) {
            throw new IllegalArgumentException(compressor.getClass() + " decompress error", e);
        }

        try {
            return this.serialization.deserialize(value, clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    " deserialize to " + clazz.getGenericSuperclass().getTypeName()
                            + " error", e);
        }
    }

    public static class RpcCallInfo {

        final String caller;
        final String callee;
        final String func;
        final Map<String, Object> attachments = new HashMap<>();

        public RpcCallInfo(String caller, String callee, String func) {
            this.caller = Objects.requireNonNull(caller, "caller is null");
            this.callee = Objects.requireNonNull(callee, "callee is null");
            this.func = Objects.requireNonNull(func, "func is null");
        }

        public String getCaller() {
            return caller;
        }

        public String getCallee() {
            return callee;
        }

        public String getFunc() {
            return func;
        }

        public Map<String, Object> getAttachments() {
            return attachments;
        }

    }

}
