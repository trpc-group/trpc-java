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

import java.util.concurrent.CompletionException;
import org.junit.Assert;
import org.junit.Test;

public class ExceptionHelperTest {

    @Test
    public void testIsTRpcException() {
        Assert.assertTrue(ExceptionHelper.isTRpcException(new TRpcException()));
        Assert.assertTrue(ExceptionHelper.isTRpcException(TRpcException.newException(1, 1, "1")));
        Assert.assertTrue(ExceptionHelper.isTRpcException(TRpcException.newException(1, 1, "1", new Throwable())));
        Assert.assertTrue(ExceptionHelper.isTRpcException(TRpcException.newBizException(1, "1", new Throwable())));
        Assert.assertTrue(ExceptionHelper.isTRpcException(TRpcException.newBizException(1, "%s", "s")));
        Assert.assertTrue(ExceptionHelper.isTRpcException(TRpcException.newFrameException(1, "%s", "s")));
        Assert.assertFalse(ExceptionHelper.isTRpcException(new BinderException()));
        Assert.assertFalse(ExceptionHelper.isTRpcException(new TRpcExtensionException()));
        Assert.assertFalse(ExceptionHelper.isTRpcException(new ConfigCenterException()));
        Assert.assertFalse(ExceptionHelper.isTRpcException(new LifecycleException()));
        Assert.assertFalse(ExceptionHelper.isTRpcException(new IllegalAccessError()));
    }

    @Test
    public void testUnwrapCompletionException() {
        Assert.assertNotNull(ExceptionHelper.parseResponseException(null, new Throwable()));
        Assert.assertNull(ExceptionHelper.unwrapCompletionException(null));
        TRpcException exception = new TRpcException();
        Assert.assertEquals(exception, ExceptionHelper.unwrapCompletionException(new CompletionException(exception)));
        Assert.assertEquals(exception, ExceptionHelper.unwrapCompletionException(exception));
    }

    @Test
    public void testIsBizException() {
        Assert.assertTrue(ExceptionHelper.isBizException(TRpcException.newBizException(111, "hello")));
        Assert.assertFalse(ExceptionHelper.isBizException(new BinderException("binder")));
        Assert.assertFalse(ExceptionHelper.isBizException(new BinderException("binder", null)));
        Assert.assertFalse(ExceptionHelper.isBizException(new ConfigCenterException("cce")));
        Assert.assertFalse(ExceptionHelper.isBizException(new ConfigCenterException("cce", null)));
        Assert.assertFalse(ExceptionHelper.isBizException(new ConfigCenterException(new Throwable())));
        Assert.assertFalse(ExceptionHelper.isBizException(new LifecycleException(new Throwable())));
        Assert.assertFalse(ExceptionHelper.isBizException(new TransportException(new Throwable())));
        Assert.assertFalse(ExceptionHelper.isBizException(new TRpcExtensionException(new Throwable())));
        Assert.assertFalse(ExceptionHelper.isBizException(TransportException.trans(new Throwable())));
        Assert.assertFalse(ExceptionHelper.isBizException(new IllegalAccessError()));
    }
}