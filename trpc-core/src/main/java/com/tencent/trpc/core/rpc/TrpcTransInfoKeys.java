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

/**
 * Framework link transparent key constants.
 */
public interface TrpcTransInfoKeys {

    /**
     * Dyeing key.
     */
    String DYEING_KEY = "trpc-dyeing-key";

    /**
     * Caller container name.
     */
    String CALLER_CONTAINER_NAME = "trpc-caller-container-name";
    /**
     * Caller set name.
     */
    String CALLER_SET_NAME = "trpc-caller-set-name";
    /**
     * Digital signature.
     */
    String DIGITAL_SIGNATURE = "trpc-digital-signature";

}
