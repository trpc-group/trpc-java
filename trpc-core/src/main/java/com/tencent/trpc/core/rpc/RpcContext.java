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

package com.tencent.trpc.core.rpc;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.utils.BytesUtils;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

/**
 * RpcContext definition, contains shared data in the client or server invoke process.
 */
public abstract class RpcContext {

    /**
     * [Framework usage]: Parameter sharing during context transfer, ServerContext.newClientContext will pass t
     * his information.
     */
    protected ConcurrentMap<String, Object> valueMap = Maps.newConcurrentMap();
    /**
     * [Business usage] Protocol request transparent fields: client setting | server receiving, mainly used by TRPC
     * and HTTP, {@code <String, byte[]>}.
     */
    protected ConcurrentMap<String, Object> reqAttachMap = Maps.newConcurrentMap();
    /**
     * [Business usage] Protocol response transparent fields: server setting | client receiving, mainly used by TRPC
     * and HTTP, {@code <String, byte[]>}.
     */
    protected ConcurrentMap<String, Object> rspAttachMap = Maps.newConcurrentMap();
    /**
     * [Business usage] Whether it is oneway.
     */
    protected boolean isOneWay;
    /**
     * [Business usage] CallInfo for reporting monitoring: caller and callee information.
     */
    protected CallInfo callInfo = new CallInfo();
    /**
     * [Business usage] Dyeing key.
     */
    private String dyeingKey;

    /**
     * [For business use] Client attachment, valid for tRpc protocol.
     */
    private byte[] requestUncodecDataSegment;

    /**
     * [For business use] Server attachment, valid for tRpc protocol.
     */
    private byte[] responseUncodecDataSegment;

    @Override
    public String toString() {
        return "RpcContext {valueMap=" + valueMap
                + ", reqAttachMap=" + reqAttachMap
                + ", rspAttachMap=" + rspAttachMap
                + ", isOneWay=" + isOneWay
                + ", callInfo=" + callInfo
                + ", dyeingKey=" + dyeingKey
                + ", requestAttachment size=" + BytesUtils.bytesLength(requestUncodecDataSegment)
                + ", responseAttachment size=" + BytesUtils.bytesLength(responseUncodecDataSegment) + "}";
    }

    /**
     * 复制context信息，默认不复制附件，如需要复制附件请业务方自行设置
     * {@code
     * context.setRequestUncodecDataSegment(requestUncodecDataSegment);
     * context.setResponseUncodecDataSegment(responseUncodecDataSegment);
     * }
     *
     * @param context
     */
    protected void cloneTo(RpcContext context) {
        cloneValueMapTo(context);
        cloneCallInfoTo(context);
        cloneReqAttachTo(context);
        cloneRspAttachTo(context);
        context.setOneWay(isOneWay);
        context.setDyeingKey(dyeingKey);
    }

    protected void cloneValueMapTo(RpcContext context) {
        if (valueMap != null && !valueMap.isEmpty()) {
            context.getValueMap().putAll(valueMap);
        }
    }

    protected void cloneReqAttachTo(RpcContext context) {
        if (!this.getReqAttachMap().isEmpty()) {
            context.getReqAttachMap().putAll(this.getReqAttachMap());
        }
    }

    protected void cloneRspAttachTo(RpcContext context) {
        if (!this.getRspAttachMap().isEmpty()) {
            context.getRspAttachMap().putAll(this.getRspAttachMap());
        }
    }

    protected void cloneCallInfoTo(RpcContext context) {
        context.setCallInfo(callInfo.clone());
    }

    public RpcServerContext toServerContext() {
        return (RpcServerContext) this;
    }

    public RpcClientContext toClientContext() {
        return (RpcClientContext) this;
    }

    public boolean isServerContext() {
        return this instanceof RpcServerContext;
    }

    public ConcurrentMap<String, Object> getReqAttachMap() {
        return reqAttachMap;
    }

    public boolean isDyeing() {
        return dyeingKey != null;
    }

    public String getDyeingKey() {
        return dyeingKey;
    }

    public void setDyeingKey(String dyeingKey) {
        this.dyeingKey = dyeingKey;
    }

    public CallInfo getCallInfo() {
        return callInfo;
    }

    public void setCallInfo(CallInfo callInfo) {
        Objects.requireNonNull(callInfo, "callInfo");
        this.callInfo = callInfo;
    }

    public ConcurrentMap<String, Object> getRspAttachMap() {
        return rspAttachMap;
    }

    public boolean isOneWay() {
        return isOneWay;
    }

    public void setOneWay(boolean isOneWay) {
        this.isOneWay = isOneWay;
    }

    public ConcurrentMap<String, Object> getValueMap() {
        return valueMap;
    }

    /**
     * Get the client attachment, valid for tRpc protocol.
     *
     * @return byte array of the client attachment
     */
    public byte[] getRequestUncodecDataSegment() {
        return requestUncodecDataSegment;
    }

    /**
     * Set the client attachment, valid for tRpc protocol.
     * The total size of tRpc protocol request data is represented by 4 bytes (frame header + Unary header + Unary body
     * + attachment), up to 2GB, so the attachment size must also be less than 2GB.
     *
     * @param requestUncodecDataSegment byte array of the client attachment
     */
    public void setRequestUncodecDataSegment(byte[] requestUncodecDataSegment) {
        this.requestUncodecDataSegment = requestUncodecDataSegment;
    }

    /**
     * Get the server attachment, valid for tRpc protocol.
     *
     * @return byte array of the server attachment
     */
    public byte[] getResponseUncodecDataSegment() {
        return responseUncodecDataSegment;
    }

    /**
     * Set the server attachment, valid for tRpc protocol.
     * The total size of tRpc protocol request data is represented by 4 bytes (frame header + Unary header + Unary body
     * + attachment), up to 2GB, so the attachment size must also be less than 2GB.
     *
     * @param responseUncodecDataSegment byte array of the server attachment
     */
    public void setResponseUncodecDataSegment(byte[] responseUncodecDataSegment) {
        this.responseUncodecDataSegment = responseUncodecDataSegment;
    }

}
