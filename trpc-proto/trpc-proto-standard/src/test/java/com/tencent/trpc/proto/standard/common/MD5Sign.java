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

package com.tencent.trpc.proto.standard.common;

import com.tencent.trpc.core.sign.spi.Sign;

public class MD5Sign implements Sign {

    @Override
    public String name() {
        return "md5Sign";
    }

    @Override
    public String digest(byte[] body) {
        return new String(body);
    }
}
