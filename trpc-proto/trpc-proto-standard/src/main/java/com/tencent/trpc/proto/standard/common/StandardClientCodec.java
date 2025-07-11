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

import static com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcCallType.TRPC_ONEWAY_CALL_VALUE;
import static com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcCallType.TRPC_UNARY_CALL_VALUE;

import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.compressor.spi.Compressor;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.RequestMeta;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.TrpcTransInfoKeys;
import com.tencent.trpc.core.rpc.def.DecodableValue;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.core.rpc.def.EncodableValue;
import com.tencent.trpc.core.serialization.spi.Serialization;
import com.tencent.trpc.core.sign.SignSupport;
import com.tencent.trpc.core.sign.spi.Sign;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.codec.ChannelBuffer;
import com.tencent.trpc.core.transport.codec.ClientCodec;
import com.tencent.trpc.core.utils.BytesUtils;
import com.tencent.trpc.core.utils.ProtoJsonConverter;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.RequestProtocol.Builder;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.ResponseProtocol;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcMessageType;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcProtoVersion;
import java.util.Map;
import java.util.Objects;

/**
 * TRPC protocol client codec
 */
public class StandardClientCodec extends ClientCodec {

    @Override
    public void encode(Channel channel, ChannelBuffer channelBuffer, Object message) {
        Request request = checkAndConvertMessage(message);
        ProtocolConfig protocol = channel.getProtocolConfig();
        Serialization serialization = checkAndGetSerialization(protocol.getSerialization());
        RpcInvocation invocation = request.getInvocation();
        // trpc protocol client attachment
        byte[] attachment = request.getContext().getRequestUncodecDataSegment();
        int attachmentSize = BytesUtils.bytesLength(attachment);
        TRPCProtocol.RequestProtocol.Builder requestHeader = TRPCProtocol.RequestProtocol.newBuilder()
                .setContentType(serialization.type())
                .setVersion(TrpcProtoVersion.TRPC_PROTO_V1_VALUE)
                .setRequestId((int) request.getRequestId())
                .setAttachmentSize(attachmentSize)
                .setFunc(ByteString.copyFromUtf8(invocation.getFunc()));
        fillRequestHeaderWithMeta(requestHeader, request.getMeta());
        fillRequestHeaderWithAttachments(requestHeader, request.getAttachments());
        Compressor compressor = checkAndGetCompressor(protocol.getCompressor());
        EncodableValue value = getEncodableValue(protocol.getCompressMinBytes(), serialization, compressor,
                invocation.isGeneric(), invocation.getFirstArgument());
        byte[] unaryBody = value.encode();
        requestHeader.setContentEncoding(getContentEncoding(value));
        doBodySignature(unaryBody, requestHeader, protocol.getSign());
        byte[] requestHeaders = requestHeader.build().toByteArray();
        int pkgLength = writePackageAndGetPkgLength(channelBuffer, unaryBody, attachment, requestHeaders);
        request.getMeta().setSize(pkgLength);
        if (logger.isDebugEnabled()) {
            logger.debug(">>>tRPC client encode {request=[{}], unaryBody=[{}], attachment size=[{}]}",
                    TextFormat.shortDebugString(requestHeader), ProtoJsonConverter.toString(invocation.getArguments()),
                    attachmentSize);
        }
    }

    private Request checkAndConvertMessage(Object message) {
        if (!(message instanceof Request)) {
            throw TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_ENCODE_ERR, "not support request "
                    + message.getClass());
        }
        return (Request) message;
    }

    private void fillRequestHeaderWithMeta(Builder requestBuilder, RequestMeta meta) {
        requestBuilder.setCallType(meta.isOneWay() ? TRPC_ONEWAY_CALL_VALUE : TRPC_UNARY_CALL_VALUE);
        if (meta.getTimeout() > 0) {
            requestBuilder.setTimeout(meta.getTimeout());
        }
        // set caller/callee
        CallInfo callInfo = meta.getCallInfo();
        requestBuilder.setCaller(ByteString.copyFromUtf8("trpc." + callInfo.getCallerApp() + "."
                + callInfo.getCallerServer() + "." + callInfo.getCallerService()));
        requestBuilder.setCallee(ByteString.copyFromUtf8("trpc." + callInfo.getCalleeApp() + "."
                + callInfo.getCalleeServer() + "." + callInfo.getCalleeService()));
        // set messageType
        requestBuilder.setMessageType(requestBuilder.getMessageType() | meta.getMessageType());
        // set additional key
        if (meta.getDyeingKey() != null) {
            requestBuilder.putTransInfo(TrpcTransInfoKeys.DYEING_KEY, ByteString.copyFromUtf8(meta.getDyeingKey()));
            requestBuilder.setMessageType(requestBuilder.getMessageType() | TrpcMessageType.TRPC_DYEING_MESSAGE_VALUE);
        }
    }

    private void fillRequestHeaderWithAttachments(Builder requestBuilder, Map<String, Object> attachments) {
        attachments.forEach((k, v) -> {
            if (v instanceof String) {
                requestBuilder.putTransInfo(k, ByteString.copyFromUtf8((String) v));
            } else if (v instanceof byte[]) {
                requestBuilder.putTransInfo(k, ByteString.copyFrom((byte[]) v));
            }
        });
    }

    private int writePackageAndGetPkgLength(ChannelBuffer channelBuffer, byte[] body,
            byte[] attachment, byte[] requestHeaders) {
        StandardPackage pkg = new StandardPackage();
        pkg.setBodyBytes(body);
        pkg.setHeadBytes(requestHeaders);
        pkg.setAttachmentBytes(attachment);
        int headSize = requestHeaders.length;
        pkg.getFrame().setHeadSize(headSize);
        int unaryBodySize = BytesUtils.bytesLength(body);
        int attachmentSize = BytesUtils.bytesLength(attachment);
        int pkgLength = StandardFrame.FRAME_SIZE + headSize + unaryBodySize + attachmentSize;
        pkg.getFrame().setSize(pkgLength);
        pkg.write(channelBuffer);
        return pkgLength;
    }

    private void doBodySignature(byte[] body, Builder requestBuilder, String signName) {
        if (SignSupport.isVerify(signName, body)) {
            Sign sign = Objects.requireNonNull(SignSupport.ofName(signName), "the sign " + signName + " is not exists");
            String digitalSignature = sign.digest(body);
            requestBuilder.putTransInfo(TrpcTransInfoKeys.DIGITAL_SIGNATURE, ByteString.copyFromUtf8(digitalSignature));
        }
    }

    @Override
    public Object decode(Channel channel, ChannelBuffer in) {
        Object rawResult = StandardPackage.decode(channel, in, Boolean.FALSE);
        if (DecodeResult.isNotEnoughData(rawResult)) {
            return DecodeResult.NOT_ENOUGH_DATA;
        }
        StandardPackage result = (StandardPackage) rawResult;
        ResponseProtocol head = result.getResponseHead();
        // build response
        DefResponse rsp = new DefResponse();
        rsp.setAttachRspHead(new TRpcRspHead(result.getFrame(), head));
        head.getTransInfoMap().forEach((k, v) -> rsp.getAttachments().put(k, v.toByteArray()));
        rsp.setRequestId(head.getRequestId());
        rsp.getMeta().setSize(result.getFrame().getSize());
        rsp.getMeta().addMessageType(head.getMessageType());
        byte[] attachment = result.getAttachmentBytes();
        if (logger.isDebugEnabled()) {
            logger.debug(">>>tRPC client decode {response=[{}], body=[{}], attachment size=[{}], exception=[{}]}",
                    TextFormat.shortDebugString(head), ProtoJsonConverter.toString(rsp.getValue()),
                    BytesUtils.bytesLength(attachment), rsp.getException());
        }
        if (isFailed(head)) {
            rsp.setException(TRpcException.newException(head.getRet(), head.getFuncRet(),
                    head.getErrorMsg().toStringUtf8()));
            return rsp;
        }
        // trpc protocol response body
        byte[] unaryBody = result.getBodyBytes();
        if (!verifyBodySignature(unaryBody, channel.getProtocolConfig().getSign(), rsp)) {
            rsp.setException(TRpcException.newFrameException(ErrorCode.SIGNATURE_VERIFY_FAILURE,
                    "Signature verification failed"));
            return rsp;
        }
        rsp.setValue(new DecodableValue(head.getContentEncoding(), head.getContentType(), unaryBody));
        // set server attachment
        rsp.setResponseUncodecDataSegment(attachment);
        return rsp;
    }

    private boolean isFailed(ResponseProtocol head) {
        return head.getRet() != ErrorCode.TRPC_INVOKE_SUCCESS || head.getFuncRet() != ErrorCode.TRPC_INVOKE_SUCCESS;
    }

    private boolean verifyBodySignature(byte[] body, String signName, DefResponse rsp) {
        if (SignSupport.isNotVerify(signName, body)) {
            return true;
        }
        Sign sign = SignSupport.ofName(signName);
        String bodySignature = RpcContextUtils.getAttachValue(rsp, TrpcTransInfoKeys.DIGITAL_SIGNATURE);
        return null != sign && sign.verify(body, bodySignature);
    }
}
