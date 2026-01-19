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

package com.tencent.trpc.core.sign;

import com.tencent.trpc.core.sign.spi.Sign;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SignSupportTest {

    @Test
    public void testOfName() {
        Sign sign = SignSupport.ofName("test");
        Assertions.assertNotNull(sign);
        Assertions.assertNull(SignSupport.ofName("test1"));
    }

    @Test
    public void testIsVerify() {
        Assertions.assertFalse(SignSupport.isVerify("test", null));
        Assertions.assertFalse(SignSupport.isVerify(null, new byte[0]));
        Assertions.assertTrue(SignSupport.isVerify("test", new byte[0]));
    }

    @Test
    public void testIsNotVerify() {
        Assertions.assertTrue(SignSupport.isNotVerify("test", null));
        Assertions.assertTrue(SignSupport.isNotVerify("", new byte[0]));
        Assertions.assertFalse(SignSupport.isNotVerify("test", new byte[0]));
    }
}
