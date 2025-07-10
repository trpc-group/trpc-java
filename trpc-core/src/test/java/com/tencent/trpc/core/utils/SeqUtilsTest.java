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

package com.tencent.trpc.core.utils;

import org.junit.Assert;
import org.junit.Test;

public class SeqUtilsTest {

    @Test
    public void testSeq() {
        int intSeq = SeqUtils.genIntegerSeq();
        int intSeq1 = SeqUtils.genIntegerSeq();
        Assert.assertEquals(intSeq1, intSeq + 1);
    }
}
