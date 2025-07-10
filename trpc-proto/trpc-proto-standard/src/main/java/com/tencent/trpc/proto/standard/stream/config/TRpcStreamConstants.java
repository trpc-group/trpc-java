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

package com.tencent.trpc.proto.standard.stream.config;

/**
 * Basic configuration for TRPC related protocols.
 *
 * <p>Future will implement TRPC flow control capabilities, set buffer size to unlimited for now</p>
 */
public interface TRpcStreamConstants {

    /**
     * Default buffer size for a streaming receiving window.
     */
    int DEFAULT_STREAM_WINDOW_SIZE = 64 * 1024;

    /**
     * Starting number for available stream IDs, 0-99 IDs are reserved for the system.
     */
    int MIN_USER_STREAM_ID = 100;

    /**
     * TRPC caller format.
     */
    String TRPC_CALLER_PATTERN = "trpc.%s.%s.%s";
    /**
     * TRPC callee format.
     */
    String TRPC_CALLEE_PATTERN = "trpc.%s.%s.%s";
    /**
     * TRPC specific call method format.
     */
    String TRPC_FUNC_PATTERN = "/%s/%s";

    /**
     * Default return information for successful calls.
     */
    String RPC_DEFAULT_RET_CODE_OK = "OK";

}
