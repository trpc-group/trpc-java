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

package com.tencent.trpc.core.common;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NamedThreadFactoryTest {

    private NamedThreadFactory namedThreadFactory;

    @Before
    public void setUp() {
        namedThreadFactory = new NamedThreadFactory();
    }

    @Test
    public void testNewThread() {
        namedThreadFactory.newThread(() -> {
        });
    }

    @Test
    public void testGetThreadGroup() {
        Assert.assertNotNull(namedThreadFactory.getThreadGroup());
    }
}