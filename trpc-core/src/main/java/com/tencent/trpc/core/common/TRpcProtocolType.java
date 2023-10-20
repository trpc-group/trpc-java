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

package com.tencent.trpc.core.common;

import java.util.Arrays;

/**
 * TRPC protocol type, TRPC protocol supports streaming and standard modes.
 */
public enum TRpcProtocolType {

    /**
     * Standard protocol.
     */
    STANDARD("standard"),
    /**
     * Streaming protocol.
     */
    STREAM("stream");

    private final String name;

    TRpcProtocolType(String name) {
        this.name = name;
    }

    /**
     * Find the TRpcProtocolType by name.
     *
     * @param name the name
     * @return the TRpcProtocolType
     */
    public static TRpcProtocolType valueOfName(String name) {
        return Arrays.stream(TRpcProtocolType.values())
                .filter(clientType -> clientType.getName().equals(name))
                .findFirst().orElse(null);
    }

    public String getName() {
        return name;
    }

}
