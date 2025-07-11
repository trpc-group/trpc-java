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

package com.tencent.trpc.core.exception;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TRpcExceptionTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testCheckArgument() {
        expectedEx.expect(TRpcException.class);
        expectedEx.expectMessage("error");
        TRpcException.checkArgument(Boolean.FALSE, 1, 1, "%s", "error");
    }

    @Test
    public void testCheckBizArgument() {
        expectedEx.expect(TRpcException.class);
        expectedEx.expectMessage("error");
        TRpcException.checkBizArgument(Boolean.FALSE, 1, "%s", "error");
    }

    @Test
    public void testCheckFrameArgument() {
        expectedEx.expect(TRpcException.class);
        expectedEx.expectMessage("error");
        TRpcException.checkFrameArgument(Boolean.FALSE, 1, 1, "%s", "error");
    }

    @Test
    public void testTrans() {
        Assert.assertTrue(TRpcException.trans(new TRpcException()) instanceof TRpcException);
        Assert.assertTrue(TRpcException.trans(new TransportException("a")) instanceof TRpcException);
    }

    @Test
    public void testIsFrameException() {
        TRpcException frameEx = TRpcException.newFrameException(1, "frameex");
        Assert.assertTrue(frameEx.isFrameException());
        Assert.assertFalse(frameEx.isBizException());
        Assert.assertEquals(1, frameEx.getCode());
        Assert.assertEquals(0, frameEx.getBizCode());
        frameEx.setCode(2);
        frameEx.setBizCode(2);
        Assert.assertEquals(2, frameEx.getCode());
        Assert.assertEquals(2, frameEx.getBizCode());
    }
}