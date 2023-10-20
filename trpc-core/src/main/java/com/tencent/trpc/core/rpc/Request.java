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

import java.util.Map;

/**
 * TRPC request object definition.
 */
public interface Request extends Cloneable {

    long getRequestId();

    /**
     * Request ID, the framework sends requests using int type, and receives requests for compatibility with other
     * protocols that may be long.
     */
    void setRequestId(long requestId);

    RequestMeta getMeta();

    /**
     * Request-related metadata information, mainly used by the framework.
     */
    void setMeta(RequestMeta meta);

    /**
     * Get data and method-related information. When the server decodes into a Request, it needs to set 1) method
     * routing information rpcServiceName & rpcMethodName for framework routing, 2) Set argument to represent data,
     * method parameters.
     */
    RpcInvocation getInvocation();

    void setInvocation(RpcInvocation invocation);

    /**
     * Protocol additional information, similar to the trans_info field in TRPC, supports two setting methods:
     * attachments as a map, attachObj as a protocol-defined structure for easy user setting.
     */
    Map<String, Object> getAttachments();

    void setAttachments(Map<String, Object> map);

    Object putAttachment(String key, Object value);

    Object getAttachment(String key);

    Object removeAttachment(String key);

    /**
     * Attached request header, each protocol is defined by itself, and the server can get it when receiving
     * the package.
     */
    <T> T getAttachReqHead();

    void setAttachReqHead(Object attachHead);

    /**
     * Carry context, ClientContext for client, and ServerContext for server.
     */
    RpcContext getContext();

    void setContext(RpcContext context);

    Request clone();

}
