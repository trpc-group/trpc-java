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
import java.util.HashMap;
import java.util.Map;

/**
 * Response meta information, to avoid adding more and more methods to the {@link Response} interface.
 */
public class ResponseMeta implements Cloneable {

    /**
     * Response body size.
     */
    private int size;
    /**
     * Protocol type.
     */
    private int messageType;
    /**
     * Extension map.
     */
    private Map<String, Object> map = Maps.newHashMap();

    public ResponseMeta clone() {
        ResponseMeta clone;
        try {
            clone = (ResponseMeta) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        clone.setMap(new HashMap<String, Object>(map));
        return clone;
    }

    public void addMessageType(int messageType) {
        this.messageType |= messageType;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public void setMap(Map<String, Object> map) {
        this.map = map;
    }

    @Override
    public String toString() {
        return "ResponseMeta [size=" + size + ", messageType=" + messageType + ", map=" + map + "]";
    }

}
