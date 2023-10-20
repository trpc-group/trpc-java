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

package com.tencent.trpc.core.exception;

public class ErrorCodeUtils {

    /**
     * Determine whether circuit breaking is needed.
     *
     * @param trpcErrorCode the TRPC error code
     * @return true if circuit breaking is needed, false otherwise
     */
    public static boolean needCircuitBreaker(int trpcErrorCode) {
        return trpcErrorCode == ErrorCode.TRPC_CLIENT_INVOKE_TIMEOUT_ERR
                || trpcErrorCode == ErrorCode.TRPC_CLIENT_CONNECT_ERR
                || trpcErrorCode == ErrorCode.TRPC_CLIENT_NETWORK_ERR
                || trpcErrorCode == ErrorCode.TRPC_SERVER_OVERLOAD_ERR
                || trpcErrorCode == ErrorCode.TRPC_SERVER_TIMEOUT_ERR;
    }

}
