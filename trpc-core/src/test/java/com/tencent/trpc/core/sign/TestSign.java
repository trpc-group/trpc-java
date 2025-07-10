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

public class TestSign implements Sign {

    @Override
    public String name() {
        return "test";
    }

    @Override
    public String digest(byte[] body) {
        return null;
    }
}