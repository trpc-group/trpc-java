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

import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExceptionHelperTest {

    @Test
    public void testIsTRpcException() {
        Assertions.assertTrue(ExceptionHelper.isTRpcException(new TRpcException()));
        Assertions.assertTrue(ExceptionHelper.isTRpcException(TRpcException.newException(1, 1, "1")));
        Assertions.assertTrue(ExceptionHelper.isTRpcException(TRpcException.newException(1, 1, "1", new Throwable())));
        Assertions.assertTrue(ExceptionHelper.isTRpcException(TRpcException.newBizException(1, "1", new Throwable())));
        Assertions.assertTrue(ExceptionHelper.isTRpcException(TRpcException.newBizException(1, "%s", "s")));
        Assertions.assertTrue(ExceptionHelper.isTRpcException(TRpcException.newFrameException(1, "%s", "s")));
        Assertions.assertFalse(ExceptionHelper.isTRpcException(new BinderException()));
        Assertions.assertFalse(ExceptionHelper.isTRpcException(new TRpcExtensionException()));
        Assertions.assertFalse(ExceptionHelper.isTRpcException(new ConfigCenterException()));
        Assertions.assertFalse(ExceptionHelper.isTRpcException(new LifecycleException()));
        Assertions.assertFalse(ExceptionHelper.isTRpcException(new IllegalAccessError()));
    }

    @Test
    public void testUnwrapCompletionException() {
        Assertions.assertNotNull(ExceptionHelper.parseResponseException(null, new Throwable()));
        Assertions.assertNull(ExceptionHelper.unwrapCompletionException(null));
        TRpcException exception = new TRpcException();
        Assertions.assertEquals(exception,
                ExceptionHelper.unwrapCompletionException(new CompletionException(exception)));
        Assertions.assertEquals(exception, ExceptionHelper.unwrapCompletionException(exception));
    }

    @Test
    public void testIsBizException() {
        Assertions.assertTrue(ExceptionHelper.isBizException(TRpcException.newBizException(111, "hello")));
        Assertions.assertFalse(ExceptionHelper.isBizException(new BinderException("binder")));
        Assertions.assertFalse(ExceptionHelper.isBizException(new BinderException("binder", null)));
        Assertions.assertFalse(ExceptionHelper.isBizException(new ConfigCenterException("cce")));
        Assertions.assertFalse(ExceptionHelper.isBizException(new ConfigCenterException("cce", null)));
        Assertions.assertFalse(ExceptionHelper.isBizException(new ConfigCenterException(new Throwable())));
        Assertions.assertFalse(ExceptionHelper.isBizException(new LifecycleException(new Throwable())));
        Assertions.assertFalse(ExceptionHelper.isBizException(new TransportException(new Throwable())));
        Assertions.assertFalse(ExceptionHelper.isBizException(new TRpcExtensionException(new Throwable())));
        Assertions.assertFalse(ExceptionHelper.isBizException(TransportException.trans(new Throwable())));
        Assertions.assertFalse(ExceptionHelper.isBizException(new IllegalAccessError()));
    }
}
