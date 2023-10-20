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

package com.tencent.trpc.opentelemetry.spi;

import com.tencent.trpc.core.rpc.Request;

/**
 * Set the propagation data into the request:
 * implement this interface for customization when the private protocol
 * on the business side is different from the default implementation
 */
public interface ITrpcRequestSetter {

    /**
     * Setting key/value pairs into the request
     * <br>
     * Note: SPI implementations are responsible for handling exceptions,
     * and throwing exceptions in this method may affect normal business processes.
     *
     * @param carrier tRPC request
     * @param key key
     * @param value value
     */
    void set(Request carrier, String key, String value);
    
}
