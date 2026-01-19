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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LimiterExceptionTest {

    @Test
    public void test() {
        LimiterException limiterException = new LimiterException("limiter exception");
        Assertions.assertTrue(limiterException.getMessage().equals("limiter exception"));
    }

    @Test
    public void test2() {
        LimiterException limiterException = new LimiterException(new NullPointerException("msg"));
        Assertions.assertTrue(limiterException.getMessage().equals("java.lang.NullPointerException: msg"));
        Assertions.assertTrue(limiterException.getCause() instanceof NullPointerException);
    }

    @Test
    public void test3() {
        LimiterException exception = new LimiterException("msg2", new NullPointerException("msg1"));
        Assertions.assertTrue(exception.getCause() instanceof NullPointerException);
        Assertions.assertTrue(exception.getMessage().equals("msg2"));
    }

    @Test
    public void test4() {
        LimiterException limiterException = new LimiterException();
        Assertions.assertTrue(StringUtils.isEmpty(limiterException.getMessage()));
    }

    @Test
    public void test5() {
        LimiterException limiterException = new LimiterException("msg3", new NullPointerException(), false, false);
        Assertions.assertTrue(limiterException.getMessage().equals("msg3"));
    }
}
