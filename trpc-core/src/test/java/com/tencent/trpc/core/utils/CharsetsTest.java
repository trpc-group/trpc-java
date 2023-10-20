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

package com.tencent.trpc.core.utils;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class CharsetsTest {

    @Test
    public void test() {
        assertEquals(Charsets.US_ASCII, StandardCharsets.US_ASCII);

        assertEquals(Charsets.ISO_8859_1, StandardCharsets.ISO_8859_1);

        assertEquals(Charsets.UTF_8, StandardCharsets.UTF_8);

        assertEquals(Charsets.UTF_16BE, StandardCharsets.UTF_16BE);

        assertEquals(Charsets.UTF_16LE, StandardCharsets.UTF_16LE);

        assertEquals(Charsets.UTF_16, StandardCharsets.UTF_16);
    }
}
