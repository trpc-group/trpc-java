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

package com.tencent.trpc.core.rpc;

import com.tencent.trpc.core.utils.BytesUtils;
import com.tencent.trpc.core.utils.ProtoJsonConverter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * TRPC response abstract class.
 */
public abstract class AbstractResponse implements Response {

    /**
     * Response pass-through information, information passed through from the server to the client.
     */
    protected Map<String, Object> attachmentMap = new HashMap<>();
    /**
     * Sequence number.
     */
    private long requestId;
    /**
     * Response result.
     */
    private Object value;
    private Throwable exception;
    private Request request;
    /**
     * Protocol attached response object, for obtaining other header information besides pass-through information.
     */
    private Object attachRspHead;
    /**
     * Response metadata.
     */
    private ResponseMeta meta = new ResponseMeta();
    /**
     * Attachments sent from the server to the client, no need for serialization.
     */
    private byte[] responseUncodecDataSegment;

    @Override
    public String toString() {
        return "AbstractResponse {value=" + ProtoJsonConverter.toString(value) + ", exception="
                + exception + ", attachmentMap=" + attachmentMap + ", attachRspHead="
                + attachRspHead + ", meta=" + meta + ", attachment size="
                + BytesUtils.bytesLength(responseUncodecDataSegment) + "}";
    }

    @Override
    public Response clone() {
        AbstractResponse clone;
        try {
            clone = (AbstractResponse) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("", e);
        }
        clone.setRequestId(requestId);
        clone.setValue(value);
        clone.setException(exception);
        clone.setRequest(request);
        clone.setAttachments(new HashMap<>(attachmentMap));
        clone.setAttachRspHead(attachRspHead);
        clone.setMeta(meta.clone());
        clone.setResponseUncodecDataSegment(this.responseUncodecDataSegment);
        return clone;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    @Override
    public ResponseMeta getMeta() {
        return meta;
    }

    public void setMeta(ResponseMeta meta) {
        this.meta = meta;
    }

    @Override
    public Throwable getException() {
        return exception;
    }

    @Override
    public void setException(Throwable exception) {
        this.exception = exception;
    }

    @Override
    public Object putAttachment(String key, Object value) {
        return attachmentMap.put(key, value);
    }

    @Override
    public Object getAttachment(String key) {
        return attachmentMap.get(key);
    }

    @Override
    public Object removeAttachment(String key) {
        return attachmentMap.remove(key);
    }

    @Override
    public Map<String, Object> getAttachments() {
        return attachmentMap;
    }

    @Override
    public void setAttachments(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        this.attachmentMap = map;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAttachRspHead() {
        return (T) attachRspHead;
    }

    @Override
    public void setAttachRspHead(Object attachRspHead) {
        this.attachRspHead = attachRspHead;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Request> T getRequest() {
        return (T) request;
    }

    @Override
    public void setRequest(Request request) {
        this.request = request;
    }

    /**
     * Get the attachments sent by the server using the TRPC protocol.
     *
     * @return byte array of the attachments
     */
    @Override
    public byte[] getResponseUncodecDataSegment() {
        return this.responseUncodecDataSegment;
    }

    /**
     * Set the attachments to be sent by the server using the TRPC protocol.
     *
     * @param responseUncodecDataSegment byte array of the attachments
     */
    @Override
    public void setResponseUncodecDataSegment(byte[] responseUncodecDataSegment) {
        this.responseUncodecDataSegment = responseUncodecDataSegment;
    }

}
