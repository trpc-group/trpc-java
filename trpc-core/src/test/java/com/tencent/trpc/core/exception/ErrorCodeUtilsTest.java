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

import org.junit.Assert;
import org.junit.Test;

public class ErrorCodeUtilsTest {

    @Test
    public void testNeedCircuitBreaker() {
        Assert.assertTrue(ErrorCodeUtils.needCircuitBreaker(ErrorCode.TRPC_CLIENT_INVOKE_TIMEOUT_ERR));
        Assert.assertTrue(ErrorCodeUtils.needCircuitBreaker(ErrorCode.TRPC_CLIENT_CONNECT_ERR));
        Assert.assertTrue(ErrorCodeUtils.needCircuitBreaker(ErrorCode.TRPC_CLIENT_NETWORK_ERR));
        Assert.assertTrue(ErrorCodeUtils.needCircuitBreaker(ErrorCode.TRPC_SERVER_OVERLOAD_ERR));
        Assert.assertTrue(ErrorCodeUtils.needCircuitBreaker(ErrorCode.TRPC_SERVER_TIMEOUT_ERR));
    }
}