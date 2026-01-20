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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TRpcExceptionTest {

    @Test
    public void testCheckArgument() {
        TRpcException exception = Assertions.assertThrows(TRpcException.class, () -> {
            TRpcException.checkArgument(Boolean.FALSE, 1, 1, "%s", "error");
        });
        Assertions.assertTrue(exception.getMessage().contains("error"));
    }

    @Test
    public void testCheckBizArg() {
        TRpcException exception = Assertions.assertThrows(TRpcException.class, () -> {
            TRpcException.checkBizArgument(Boolean.FALSE, 1, "%s", "error");
        });
        Assertions.assertTrue(exception.getMessage().contains("error"));
    }

    @Test
    public void testCheckFrameArg() {
        TRpcException exception = Assertions.assertThrows(TRpcException.class, () -> {
            TRpcException.checkFrameArgument(Boolean.FALSE, 1, 1, "%s", "error");
        });
        Assertions.assertTrue(exception.getMessage().contains("error"));
    }

    @Test
    public void testTrans() {
        Assertions.assertTrue(TRpcException.trans(new TRpcException()) instanceof TRpcException);
        Assertions.assertTrue(TRpcException.trans(new TransportException("a")) instanceof TRpcException);
    }

    @Test
    public void testIsFrameException() {
        TRpcException frameEx = TRpcException.newFrameException(1, "frameex");
        Assertions.assertTrue(frameEx.isFrameException());
        Assertions.assertFalse(frameEx.isBizException());
        Assertions.assertEquals(1, frameEx.getCode());
        Assertions.assertEquals(0, frameEx.getBizCode());
        frameEx.setCode(2);
        frameEx.setBizCode(2);
        Assertions.assertEquals(2, frameEx.getCode());
        Assertions.assertEquals(2, frameEx.getBizCode());
    }
}
