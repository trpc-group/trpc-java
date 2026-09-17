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

import static com.tencent.trpc.core.rpc.RpcContextValueKeys.SERVER_SIGNATURE_VERIFY_RESULT_KEY;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.protobuf.ByteString;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.compressor.spi.Compressor;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.core.rpc.TrpcTransInfoKeys;
import com.tencent.trpc.core.rpc.def.DecodableValue;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.EncodableValue;
import com.tencent.trpc.core.serialization.spi.Serialization;
import com.tencent.trpc.core.sign.SignSupport;
import com.tencent.trpc.core.sign.spi.Sign;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.codec.ChannelBuffer;
import com.tencent.trpc.core.transport.codec.ServerCodec;
import com.tencent.trpc.core.utils.BytesUtils;
import com.tencent.trpc.core.utils.ProtoJsonConverter;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.RequestProtocol;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.ResponseProtocol;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.ResponseProtocol.Builder;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcCallType;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcMessageType;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;

/**
 * TRPC protocol server codec
 */
public class StandardServerCodec extends ServerCodec {

    /**
     * The max entry number of the decoding caches, the entries will be evicted when it is exceeded.
     */
    private static final int CACHE_MAX_SIZE = 10000;

    /**
     * The initial capacity of the decoding caches.
     */
    private static final int CACHE_INITIAL_CAPACITY = 64;

    /**
     * The expire time(in minutes) after the last access of a decoding cache entry.
     */
    private static final int CACHE_EXPIRE_MINUTES = 60;

    /**
     * The max length of the cache key, the key longer than it will never be cached, so that a few oversized
     * attacker-controlled keys can not occupy too much memory.
     */
    private static final int CACHE_KEY_MAX_LENGTH = 1024;

    /**
     * The separator of the func of the request head, whose format is {@code /serviceName/methodName}.
     */
    private static final String FUNC_SEPARATOR = "/";

    /**
     * The begin index of the serviceName in the func, the func always starts with {@link #FUNC_SEPARATOR}.
     */
    private static final int FUNC_SERVICE_NAME_BEGIN_INDEX = 1;

    /**
     * The index of the serviceName in the parsed func info.
     */
    private static final int FUNC_SERVICE_NAME_INDEX = 0;

    /**
     * The index of the methodName in the parsed func info.
     */
    private static final int FUNC_METHOD_NAME_INDEX = 1;

    /**
     * Remotely call the cache of services and methods to avoid string cutting operations during decoding.
     *
     * <p>Note: the cache key comes from the attacker-controllable protocol head, so it MUST be bounded, see
     * {@link #newCache()} and {@link #getOrCompute(Cache, String, Function)}.</p>
     */
    private static final Cache<String, String[]> FUNC_INFO_CACHE = newCache();

    /**
     * The cache of the caller and callee information avoids the string cutting operation during decoding, which can
     * increase the throughput of the framework by about 4%. Considering that the mainstream of the current
     * architecture is microservices, and each service has limited external interfaces, a bounded cache is enough
     * here.
     *
     * <p>Note: the cache key comes from the attacker-controllable protocol head, so it MUST be bounded, see
     * {@link #newCache()} and {@link #getOrCompute(Cache, String, Function)}.</p>
     */
    private static final Cache<String, CallInfo> CALL_INFO_CACHE = newCache();

    /**
     * Create a bounded cache whose entries are evicted by size and by idle time.
     *
     * @param <V> the type of the cached value
     * @return the bounded cache
     */
    private static <V> Cache<String, V> newCache() {
        return Caffeine.newBuilder()
                .initialCapacity(CACHE_INITIAL_CAPACITY)
                .maximumSize(CACHE_MAX_SIZE)
                .expireAfterAccess(Duration.ofMinutes(CACHE_EXPIRE_MINUTES))
                .build();
    }

    /**
     * Get the value from the bounded cache, compute it if absent.
     *
     * <p>The cache keys are built from the request head fields(func/caller/callee) which are fully controlled by the
     * remote peer, and they are written before the service/method existence check. An unbounded cache would allow an
     * attacker to write an entry per request and finally exhaust the heap(OOM). The cache is bounded in two
     * dimensions here: the entry number is limited by {@link #CACHE_MAX_SIZE}, and an oversized key is never cached
     * so that the memory of a single entry is limited as well.</p>
     *
     * @param cache the cache to read and write
     * @param key the cache key, which is untrusted
     * @param mappingFunction the function to compute the value
     * @param <V> the type of the cached value
     * @return the cached or newly computed value
     */
    private static <V> V getOrCompute(Cache<String, V> cache, String key, Function<String, V> mappingFunction) {
        if (key.length() > CACHE_KEY_MAX_LENGTH) {
            return mappingFunction.apply(key);
        }
        return cache.get(key, mappingFunction);
    }

    @Override
    public void encode(Channel channel, ChannelBuffer channelBuffer, Object message) {
        Response response = checkAndConvertMessage(message);
        Request request = response.getRequest();
        TRpcReqHead head = request.getAttachReqHead();
        Serialization serialization = checkAndGetSerialization(head.getHead().getContentType());
        // trpc protocol server attachment
        byte[] attachment = request.getContext().getResponseUncodecDataSegment();
        int attachmentSize = BytesUtils.bytesLength(attachment);
        TRPCProtocol.ResponseProtocol.Builder rawResponse = TRPCProtocol.ResponseProtocol
                .newBuilder()
                .setCallType(head.getHead().getCallType())
                .setRequestId(head.getHead().getRequestId())
                .setContentType(serialization.type())
                .setVersion(head.getHead().getVersion())
                .setMessageType(response.getMeta().getMessageType())
                .setAttachmentSize(attachmentSize);

        ProtocolConfig protocol = channel.getProtocolConfig();
        Compressor compressor = checkAndGetCompressor(protocol.getCompressor());
        EncodableValue value = getEncodableValue(protocol.getCompressMinBytes(), serialization, compressor,
                request.getInvocation().isGeneric(), response.getValue());
        fillResponseWithAttachments(response, rawResponse);
        fillResponseIfError(response, rawResponse);
        byte[] unaryBody = value.encode();
        // setContentEncoding must be executed after value.encode(),
        // because the compression type is confirmed in value.encode()
        rawResponse.setContentEncoding(getContentEncoding(value));
        doBodySignature(unaryBody, rawResponse, protocol.getSign());
        byte[] rspBytes = rawResponse.build().toByteArray();
        int pkgLength = writePackageAndGetPkgLength(channelBuffer, request.getAttachReqHead(),
                unaryBody, attachment, rspBytes);
        response.getMeta().setSize(pkgLength);
        if (logger.isDebugEnabled()) {
            logger.debug(">>>Trpc server encode {response=[{}], body=[{}], attachment size=[{}], exception=[{}]}",
                    ProtoJsonConverter.toString(rawResponse), ProtoJsonConverter.toString(response.getValue()),
                    attachmentSize, response.getException());
        }
    }

    private Response checkAndConvertMessage(Object message) {
        if (!(message instanceof Response)) {
            throw TRpcException.newFrameException(ErrorCode.TRPC_SERVER_DECODE_ERR,
                    "TRpc server codec encode error, not support message<class=" + message.getClass() + ">");
        }
        return (Response) message;
    }

    private void fillResponseWithAttachments(Response response, Builder rawResponse) {
        response.getAttachments().forEach((k, v) -> putTransInfo(rawResponse, k, v));
        response.getRequest().getAttachments().forEach((k, v) -> putTransInfo(rawResponse, k, v));
    }

    /**
     * Added transparent field
     *
     * @param rawResponse response class
     * @param k the key of transparent field
     * @param v the value of transparent field
     */
    private void putTransInfo(Builder rawResponse, String k, Object v) {
        if (v instanceof String) {
            rawResponse.putTransInfo(k, ByteString.copyFromUtf8((String) v));
        } else if (v instanceof byte[]) {
            rawResponse.putTransInfo(k, ByteString.copyFrom((byte[]) v));
        }
    }

    private void fillResponseIfError(Response response, Builder rawResponse) {
        Throwable e = response.getException();
        if (e != null) {
            if (e.getMessage() != null) {
                rawResponse.setErrorMsg(ByteString.copyFromUtf8(e.getMessage()));
            }
            if (e instanceof TRpcException) {
                TRpcException te = (TRpcException) e;
                rawResponse.setRet(te.getCode());
                rawResponse.setFuncRet(te.getBizCode());
            } else {
                rawResponse.setRet(ErrorCode.TRPC_INVOKE_UNKNOWN_ERR);
                rawResponse.setFuncRet(ErrorCode.TRPC_INVOKE_UNKNOWN_ERR);
            }
        }
    }

    private void doBodySignature(byte[] body, ResponseProtocol.Builder responseHeader, String signName) {
        if (SignSupport.isVerify(signName, body)) {
            Sign sign = Objects.requireNonNull(SignSupport.ofName(signName), "the sign " + signName + " is not exists");
            String digitalSignature = sign.digest(body);
            responseHeader.putTransInfo(TrpcTransInfoKeys.DIGITAL_SIGNATURE, ByteString.copyFromUtf8(digitalSignature));
        }
    }

    private int writePackageAndGetPkgLength(ChannelBuffer channelBuffer, TRpcReqHead reqHead,
            byte[] body, byte[] attachment, byte[] resHeader) {
        StandardPackage pkg = new StandardPackage();
        pkg.getFrame().setStreamId(reqHead.getFrame().getStreamId());
        pkg.setHeadBytes(resHeader);
        pkg.setBodyBytes(body);
        pkg.setAttachmentBytes(attachment);
        pkg.getFrame().setHeadSize(resHeader.length);
        int bodySize = BytesUtils.bytesLength(body);
        int attachmentSize = BytesUtils.bytesLength(attachment);
        int pkgLength = StandardFrame.FRAME_SIZE + resHeader.length + bodySize + attachmentSize;
        pkg.getFrame().setSize(pkgLength);
        pkg.write(channelBuffer);
        return pkgLength;
    }

    @Override
    public Object decode(Channel channel, ChannelBuffer in) {
        Object rawResult = StandardPackage.decode(channel, in, Boolean.TRUE);
        if (rawResult == DecodeResult.NOT_ENOUGH_DATA) {
            return DecodeResult.NOT_ENOUGH_DATA;
        }
        StandardPackage packet = (StandardPackage) rawResult;
        RequestProtocol requestHeader = packet.getRequestHead();
        DefRequest request = new DefRequest();
        request.setRequestId(requestHeader.getRequestId());
        request.setAttachReqHead(new TRpcReqHead(new StandardFrame(), requestHeader));
        setAttachments(requestHeader, request);
        byte[] unaryBody = packet.getBodyBytes();
        setVerifyBodySignatureResult(request, unaryBody, channel.getProtocolConfig().getSign());
        byte[] attachment = packet.getAttachmentBytes();
        request.getContext().setRequestUncodecDataSegment(attachment);
        setDyeingKeyIfNonNull(requestHeader, request);
        RpcInvocation inv = buildRpcInvocation(packet, requestHeader);
        request.setInvocation(inv);
        setRequestMeta(packet, requestHeader, inv, request);
        if (logger.isDebugEnabled()) {
            logger.debug("tRPC server decode {request=[{}], body=[{}], attachment size=[{}]}",
                    ProtoJsonConverter.toString(requestHeader),
                    ProtoJsonConverter.toString(request.getInvocation().getFirstArgument()),
                    BytesUtils.bytesLength(attachment));
        }
        return request;
    }

    private void setVerifyBodySignatureResult(Request request, byte[] body, String signName) {
        RpcServerContext serverContext = new RpcServerContext();
        request.setContext(serverContext);
        if (SignSupport.isNotVerify(signName, body)) {
            RpcContextUtils.putValueMapValue(serverContext, SERVER_SIGNATURE_VERIFY_RESULT_KEY, Boolean.TRUE);
            return;
        }
        Sign sign = SignSupport.ofName(signName);
        String bodySignature = RpcContextUtils.getAttachValue(request, TrpcTransInfoKeys.DIGITAL_SIGNATURE);
        RpcContextUtils.putValueMapValue(serverContext, SERVER_SIGNATURE_VERIFY_RESULT_KEY,
                null != sign && sign.verify(body, bodySignature));
    }

    private void setRequestMeta(StandardPackage packet, RequestProtocol requestHeader,
            RpcInvocation inv, DefRequest request) {
        request.getMeta().addMessageType(requestHeader.getMessageType());
        request.getMeta().setOneWay(requestHeader.getCallType() == TrpcCallType.TRPC_ONEWAY_CALL_VALUE);
        request.getMeta().setTimeout(requestHeader.getTimeout());
        request.getMeta().setSize(packet.getFrame().getSize());
        CallInfo callInfo = buildCallInfo(inv, requestHeader);
        if (callInfo != null) {
            request.getMeta().setCallInfo(callInfo);
        }
    }

    private void setAttachments(RequestProtocol requestHeader, DefRequest request) {
        requestHeader.getTransInfoMap().forEach((k, v) -> request.getAttachments().put(k, v.toByteArray()));
    }

    private RpcInvocation buildRpcInvocation(StandardPackage packet, RequestProtocol requestHeader) {
        RpcInvocation inv = new RpcInvocation();
        String func = requestHeader.getFunc().toStringUtf8();
        String[] funcInfo = getOrCompute(FUNC_INFO_CACHE, func, StandardServerCodec::parseFunc);
        inv.setFunc(func);
        inv.setRpcServiceName(funcInfo[FUNC_SERVICE_NAME_INDEX]);
        inv.setRpcMethodName(funcInfo[FUNC_METHOD_NAME_INDEX]);
        Object[] obj = new Object[]{new DecodableValue(requestHeader.getContentEncoding(),
                requestHeader.getContentType(), packet.getBodyBytes())};
        inv.setArguments(obj);
        return inv;
    }

    /**
     * Parse the func of the request head, whose format is {@code /serviceName/methodName}.
     *
     * @param func the func of the request head
     * @return an array of [serviceName, methodName], both of them are empty if the func is illegal
     */
    private static String[] parseFunc(String func) {
        int idx = func.lastIndexOf(FUNC_SEPARATOR);
        return (idx > FUNC_SERVICE_NAME_BEGIN_INDEX && func.length() > idx + 1)
                ? new String[]{func.substring(FUNC_SERVICE_NAME_BEGIN_INDEX, idx), func.substring(idx + 1)}
                : new String[]{StringUtils.EMPTY, StringUtils.EMPTY};
    }

    private void setDyeingKeyIfNonNull(RequestProtocol requestHeader, DefRequest request) {
        ByteString dyeingKeyByte = requestHeader.getTransInfoMap().get(TrpcTransInfoKeys.DYEING_KEY);
        if (dyeingKeyByte != null) {
            request.getMeta().setDyeingKey(dyeingKeyByte.toStringUtf8());
            request.getMeta().addMessageType(TrpcMessageType.TRPC_DYEING_MESSAGE_VALUE);
        }
    }

    /**
     * Build a CALL_INFO_CACHE cache, use the caller and callee information and rpcMethodName to form the key,
     * and callInfo as the value
     *
     * @param rpcInvocation rpc call context
     * @param rawReqBuilder request head
     * @return caller and callee information
     */
    private CallInfo buildCallInfo(RpcInvocation rpcInvocation, TRPCProtocol.RequestProtocol rawReqBuilder) {
        String caller = rawReqBuilder.getCaller().toStringUtf8();
        String callee = rawReqBuilder.getCallee().toStringUtf8();
        String rpcMethodName = StringUtils.isNotBlank(rpcInvocation.getRpcMethodName())
                ? rpcInvocation.getRpcMethodName() : "";
        String cacheKey = caller + callee + rpcMethodName;
        if (StringUtils.isBlank(cacheKey)) {
            return null;
        }
        return getOrCompute(CALL_INFO_CACHE, cacheKey, s -> {
            CallInfo callInfo = new CallInfo();
            fillCallerInfo(caller, callInfo);
            fillCalleeInfo(callee, rpcMethodName, callInfo);
            return callInfo;
        });
    }

    private void fillCallerInfo(String caller, CallInfo callInfo) {
        if (StringUtils.isNotBlank(caller)) {
            fillCallinfo(caller, callInfo, /* isCaller */true);
        }
    }

    private void fillCalleeInfo(String callee, String rpcMethodName, CallInfo callInfo) {
        if (StringUtils.isNotBlank(callee)) {
            fillCallinfo(callee, callInfo, /* isCaller */false);
        }
        if (StringUtils.isBlank(callInfo.getCalleeMethod())) {
            callInfo.setCalleeMethod(rpcMethodName);
        }
    }

    private void fillCallinfo(String call, CallInfo callInfo, boolean isCaller) {
        String[] strings = call.split("\\.");
        String app = (strings.length < 2 ? "" : strings[1]);
        String server = (strings.length < 3 ? "" : strings[2]);
        String service = (strings.length < 4 ? "" : strings[3]);
        if (isCaller) {
            // set caller
            callInfo.setCallerApp(app).setCallerServer(server).setCallerService(service)
                    .setCaller(call);
            return;
        }
        // set callee
        String method = (strings.length < 5 ? "" : strings[4]);
        callInfo.setCalleeApp(app).setCalleeServer(server).setCalleeService(service)
                .setCalleeMethod(method).setCallee(call);
    }
}