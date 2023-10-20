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

import java.util.Iterator;
import org.junit.Assert;
import org.junit.Test;

public class ConcurrentHashSetTest {

    @Test
    public void testUse() {
        ConcurrentHashSet concurrentHashSet = new ConcurrentHashSet();
        ConcurrentHashSet concurrentHashSetMax = new ConcurrentHashSet(100);
        Assert.assertTrue(concurrentHashSetMax.isEmpty());
        concurrentHashSet.add(1);
        concurrentHashSet.add(2);
        Assert.assertTrue(concurrentHashSet.size() == 2);
        Assert.assertTrue(concurrentHashSet.contains(1));
        concurrentHashSet.remove(1);
        Assert.assertFalse(concurrentHashSet.contains(1));
        Iterator<Number> iterator = concurrentHashSet.iterator();
        Number t2 = iterator.next();
        Assert.assertTrue(t2.intValue() == 2);
        concurrentHashSet.clear();
        Assert.assertTrue(concurrentHashSet.isEmpty());
    }
}
