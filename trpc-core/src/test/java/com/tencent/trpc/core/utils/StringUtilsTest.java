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

import org.junit.Assert;
import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void testSplitToWords() {
        Assert.assertArrayEquals(new String[]{"a", "b", "c"}, StringUtils.splitToWords("a, b, c"));
        Assert.assertArrayEquals(new String[]{"a. b. c"}, StringUtils.splitToWords("a. b. c"));
        Assert.assertArrayEquals(new String[]{"a", "b", "c"}, StringUtils.splitToWords("a,b,c"));
        Assert.assertArrayEquals(new String[]{"a", "b", "c"}, StringUtils.splitToWords("   a,   b,  c     "));
        Assert.assertArrayEquals(new String[]{"hello"}, StringUtils.splitToWords("hello"));
        Assert.assertArrayEquals(new String[]{"hello"}, StringUtils.splitToWords("    hello     "));
        Assert.assertArrayEquals(new String[]{"hello world"}, StringUtils.splitToWords("hello world"));
        Assert.assertArrayEquals(new String[]{"hello   world"}, StringUtils.splitToWords("   hello   world   "));
        Assert.assertArrayEquals(new String[]{}, StringUtils.splitToWords(null));
        Assert.assertArrayEquals(new String[]{}, StringUtils.splitToWords(""));
    }
}
