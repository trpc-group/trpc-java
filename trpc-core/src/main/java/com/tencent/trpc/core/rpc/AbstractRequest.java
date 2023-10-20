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

import java.util.HashMap;
import java.util.Map;

/**
 * TRPC request abstract class, encapsulates common parameters.
 */
public abstract class AbstractRequest implements Request {

    /**
     * Request transparent information.
     */
    protected Map<String, Object> attachmentMap = new HashMap<>();
    private long requestId;
    /**
     * Framework settings, rpc call context
     */
    private RpcInvocation invocation;
    /**
     * Protocol-related request object, for getting header information other than transparent information.
     */
    private Object attachReqHead;
    /**
     * Request metadata information.
     */
    private RequestMeta meta = new RequestMeta();
    /**
     * RpcContext context.
     */
    private RpcContext context;

    public AbstractRequest() {
    }

    @Override
    public String toString() {
        return "AbstractRequest {"
                + "  requestId=" + requestId
                + ", invocation=" + invocation
                + ", attachmentMap=" + attachmentMap
                + ", attachReqHead=" + attachReqHead
                + ", meta=" + meta + '}';
    }

    @Override
    public Request clone() {
        AbstractRequest clone;
        try {
            clone = (AbstractRequest) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("", e);
        }
        clone.setRequestId(requestId);
        clone.setInvocation(invocation);
        clone.setAttachments(new HashMap<>(attachmentMap));
        clone.setAttachReqHead(attachReqHead);
        clone.setMeta(meta.clone());
        return clone;
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
        this.attachmentMap = map;
    }

    @Override
    public RpcInvocation getInvocation() {
        return invocation;
    }

    @Override
    public void setInvocation(RpcInvocation invocation) {
        this.invocation = invocation;
    }

    @Override
    public RequestMeta getMeta() {
        return meta;
    }

    @Override
    public void setMeta(RequestMeta meta) {
        this.meta = meta;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAttachReqHead() {
        return (T) attachReqHead;
    }

    @Override
    public void setAttachReqHead(Object attachReqHead) {
        this.attachReqHead = attachReqHead;
    }

    @Override
    public long getRequestId() {
        return requestId;
    }

    @Override
    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public RpcContext getContext() {
        return context;
    }

    public void setContext(RpcContext context) {
        this.context = context;
    }

}
