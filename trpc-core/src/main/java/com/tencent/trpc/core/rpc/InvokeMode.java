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

import java.util.Objects;

public enum InvokeMode {

    /**
     * Synchronous method.
     */
    SYNC,
    /**
     * Asynchronous method.
     */
    ASYNC,
    /**
     * Server-side streaming method.
     */
    SERVER_STREAM,
    /**
     * Client-side streaming method.
     */
    CLIENT_STREAM,
    /**
     * Bidirectional streaming method.
     */
    DUPLEX_STREAM;

    /**
     * Determine if it is an asynchronous method.
     *
     * @param mode invocation mode
     * @return {@code true} if it is an asynchronous method
     */
    public static boolean isAsync(InvokeMode mode) {
        return Objects.equals(mode, ASYNC);
    }

    /**
     * Determine if it is a streaming method.
     *
     * @param mode invocation mode
     * @return {@code true} if it is a streaming method, including server-side streaming, client-side streaming, and
     *         bidirectional streaming.
     */
    public static boolean isStream(InvokeMode mode) {
        if (mode == null) {
            return false;
        }

        return mode == SERVER_STREAM || mode == CLIENT_STREAM || mode == DUPLEX_STREAM;
    }

}
