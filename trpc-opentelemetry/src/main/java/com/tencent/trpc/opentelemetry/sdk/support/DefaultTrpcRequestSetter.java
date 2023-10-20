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

package com.tencent.trpc.opentelemetry.sdk.support;

import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.opentelemetry.spi.ITrpcRequestSetter;

/**
 * Default implementation of {@link ITrpcRequestSetter}
 */
public class DefaultTrpcRequestSetter implements ITrpcRequestSetter {

    /**
     * Setting key/value pairs into the request
     *
     * @param carrier tRPC request object
     * @param key key
     * @param value value
     */
    @Override
    public void set(Request carrier, String key, String value) {
        if (carrier != null && carrier.getAttachments() != null) {
            carrier.getAttachments().put(key, value);
        }
    }

}
