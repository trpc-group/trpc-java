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

package com.tencent.trpc.spring.context.configuration.schema.client;

import com.tencent.trpc.spring.context.configuration.schema.AbstractProtocolSchema;

/**
 * Basic configurations for tRPC clients
 *
 * @see AbstractProtocolSchema
 */

public abstract class AbstractClientProtocolSchema extends AbstractProtocolSchema {

    /**
     * Connections per address
     */
    private Integer connsPerAddr;

    /**
     * Connection timeout in millis
     */
    private Integer connTimeout;

    public Integer getConnsPerAddr() {
        return connsPerAddr;
    }

    public void setConnsPerAddr(Integer connsPerAddr) {
        this.connsPerAddr = connsPerAddr;
    }

    public Integer getConnTimeout() {
        return connTimeout;
    }

    public void setConnTimeout(Integer connTimeout) {
        this.connTimeout = connTimeout;
    }

}
