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

import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PreconditionUtilsTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testCheckArgument() {
        expectedEx.expect(IllegalArgumentException.class);
        List<String> list = new ArrayList();
        PreconditionUtils
                .checkArgument(list == null, "list is null %s", "list");
    }
}
