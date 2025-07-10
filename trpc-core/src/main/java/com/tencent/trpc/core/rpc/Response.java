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

import java.util.Map;

/**
 * TRPC response object definition.
 */
public interface Response extends Cloneable {

    /**
     * Request id, the same as RequestId in Request.
     */
    long getRequestId();

    /**
     * Data responded by the server.
     */
    Object getValue();

    void setValue(Object object);

    ResponseMeta getMeta();

    /**
     * Set ResponseMeta.
     */
    void setMeta(ResponseMeta meta);

    Throwable getException();

    void setException(Throwable exception);

    /**
     * Get the request corresponding to the response.
     */
    <T extends Request> T getRequest();

    /**
     * Set request, usually set when receiving a package.
     */
    void setRequest(Request request);

    /**
     * Protocol attached information, fields that need to be passed through, similar to the trans_info field in tRpc,
     * support two setting methods: attachements as map, attachObj as protocol-defined structure.
     */
    Map<String, Object> getAttachments();

    /**
     * Set pass-through map.
     *
     * @param map
     */
    void setAttachments(Map<String, Object> map);

    /**
     * Set a value.
     *
     * @param key
     * @param value
     * @return
     */
    Object putAttachment(String key, Object value);

    /**
     * Get the value of a key.
     *
     * @param key
     * @return
     */
    Object getAttachment(String key);

    /**
     * Remove a key.
     *
     * @param key
     * @return
     */
    Object removeAttachment(String key);

    /**
     * Attached response header, each protocol is defined by itself, and the client can get it when receiving
     * the package.
     */
    <T> T getAttachRspHead();

    void setAttachRspHead(Object attachRspHead);

    Response clone();

    /**
     * Get attachments sent by the server.
     */
    byte[] getResponseUncodecDataSegment();

    /**
     * Store attachments.
     */
    void setResponseUncodecDataSegment(byte[] dataSegment);

}
