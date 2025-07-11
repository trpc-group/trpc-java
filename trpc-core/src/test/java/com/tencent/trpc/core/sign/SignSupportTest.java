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

package com.tencent.trpc.core.sign;

import com.tencent.trpc.core.sign.spi.Sign;
import org.junit.Assert;
import org.junit.Test;

public class SignSupportTest {

    @Test
    public void testOfName() {
        Sign sign = SignSupport.ofName("test");
        Assert.assertNotNull(sign);
        Assert.assertNull(SignSupport.ofName("test1"));
    }

    @Test
    public void testIsVerify() {
        Assert.assertFalse(SignSupport.isVerify("test", null));
        Assert.assertFalse(SignSupport.isVerify(null, new byte[0]));
        Assert.assertTrue(SignSupport.isVerify("test", new byte[0]));
    }

    @Test
    public void testIsNotVerify() {
        Assert.assertTrue(SignSupport.isNotVerify("test", null));
        Assert.assertTrue(SignSupport.isNotVerify("", new byte[0]));
        Assert.assertFalse(SignSupport.isNotVerify("test", new byte[0]));
    }
}