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

import com.tencent.trpc.core.utils.StringUtils;
import org.junit.Assert;
import org.junit.Test;

public class LimiterFallbackExceptionTest {

    @Test
    public void test() {
        LimiterFallbackException exception = new LimiterFallbackException("exception");
        Assert.assertTrue(exception.getMessage().equals("exception"));
    }

    @Test
    public void test2() {
        LimiterFallbackException exception = new LimiterFallbackException(new NullPointerException("msg"));
        Assert.assertTrue(exception.getMessage().equals("java.lang.NullPointerException: msg"));
        Assert.assertTrue(exception.getCause() instanceof NullPointerException);
    }

    @Test
    public void test3() {
        LimiterFallbackException exception = new LimiterFallbackException("msg2", new NullPointerException("msg1"));
        Assert.assertTrue(exception.getCause() instanceof NullPointerException);
        Assert.assertTrue(exception.getMessage().equals("msg2"));
    }

    @Test
    public void test4() {
        LimiterFallbackException exception = new LimiterFallbackException();
        Assert.assertTrue(StringUtils.isEmpty(exception.getMessage()));
    }

    @Test
    public void test5() {
        LimiterFallbackException exception = new LimiterFallbackException("msg3", new NullPointerException(), false,
                false);
        Assert.assertTrue(exception.getMessage().equals("msg3"));
    }
}