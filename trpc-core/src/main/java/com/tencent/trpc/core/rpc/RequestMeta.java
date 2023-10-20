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

import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Request meta information, to avoid adding more and more methods to the {@link Request} interface.
 */
public class RequestMeta implements Cloneable {

    /**
     * Request creation time
     */
    private long createTime = System.currentTimeMillis();
    /**
     * Framework set caller address
     */
    private InetSocketAddress localAddress;
    /**
     * Framework set callee address
     */
    private InetSocketAddress remoteAddress;
    /**
     * Request timeout
     */
    private int timeout;
    /**
     * Whether it's oneWay
     */
    private boolean isOneWay;
    /**
     * Request body size
     */
    private int size;
    /**
     * Server-side configuration information
     */
    private ProviderConfig<?> providerConfig;
    /**
     * Client-side configuration information
     */
    private ConsumerConfig<?> consumerConfig;
    /**
     * Caller and callee information
     */
    private CallInfo callInfo = new CallInfo();
    /**
     * Dyeing key
     */
    private String dyeingKey;
    /**
     * Consistent hash value
     */
    private String hashVal;
    /**
     * Protocol type
     */
    private int messageType;
    /**
     * Extension map
     */
    private Map<String, Object> map = Maps.newHashMap();

    public RequestMeta clone() {
        RequestMeta clone;
        try {
            clone = (RequestMeta) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        clone.setCreateTime(System.currentTimeMillis());
        clone.setCallInfo(callInfo.clone());
        clone.setMap(new HashMap<String, Object>(map));
        return clone;
    }

    public void addMessageType(int messageType) {
        this.messageType |= messageType;
    }

    public boolean hasMessageType(int messageType) {
        this.messageType &= messageType;
        return this.messageType == messageType;
    }

    public boolean isDyeing() {
        return dyeingKey != null;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getDyeingKey() {
        return dyeingKey;
    }

    public void setDyeingKey(String dyeingKey) {
        this.dyeingKey = dyeingKey;
    }

    public boolean isOneWay() {
        return isOneWay;
    }

    public void setOneWay(boolean isOneWay) {
        this.isOneWay = isOneWay;
    }

    public CallInfo getCallInfo() {
        return callInfo;
    }

    public void setCallInfo(CallInfo callInfo) {
        this.callInfo = callInfo;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public ProviderConfig<?> getProviderConfig() {
        return providerConfig;
    }

    public void setProviderConfig(ProviderConfig<?> providerConfig) {
        this.providerConfig = providerConfig;
    }

    public ConsumerConfig<?> getConsumerConfig() {
        return consumerConfig;
    }

    public void setConsumerConfig(ConsumerConfig<?> consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    public String getHashVal() {
        return hashVal;
    }

    public void setHashVal(String hashVal) {
        this.hashVal = hashVal;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public void setMap(Map<String, Object> map) {
        this.map = map;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    @Override
    public String toString() {
        return "RequestMeta [createTime=" + createTime + ", localAddress=" + localAddress
                + ", remoteAddress=" + remoteAddress + ", timeout=" + timeout + ", isOneWay="
                + isOneWay
                + ", size=" + size + ", callInfo=" + callInfo + ", dyeingKey=" + dyeingKey
                + ", hashVal="
                + hashVal + ", map=" + map + ", messageType=" + messageType + "]";
    }

}
