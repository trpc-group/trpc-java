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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class CollectionUtilsTest {

    @Test
    public void testMergeList() {
        List<Integer> l1 = Lists.newArrayList(1, 2, 3);
        List<Integer> l2 = Lists.newArrayList(4, 5);
        List<Integer> l3 = Lists.newArrayList(6);
        List<Integer> expected = Lists.newArrayList(1, 2, 3, 4, 5, 6);
        List<Integer> merged = CollectionUtils.mergeList(l1, l2, l3);
        Assert.assertEquals(expected, merged);
        List<Integer> mergedLinkedList = CollectionUtils.mergeList(Lists::newLinkedList, l1, l2, l3);
        Assert.assertEquals(Lists.newLinkedList(expected), mergedLinkedList);
        Assert.assertEquals(0, CollectionUtils.mergeList(null).size());
    }

    @Test
    public void testMergeSet() {
        Set<Integer> s1 = Sets.newHashSet(1, 2, 3);
        Set<Integer> s2 = Sets.newHashSet(4, 5);
        Set<Integer> s3 = Sets.newHashSet(6);
        Set<Integer> expected = Sets.newHashSet(1, 2, 3, 4, 5, 6);
        Set<Integer> merged = CollectionUtils.mergeSet(s1, s2, s3);
        Assert.assertEquals(expected.size(), merged.size());
        merged.removeAll(expected);
        Assert.assertTrue(merged.isEmpty());
        Set<Integer> mergedLinkedSet = CollectionUtils.mergeSet(Sets::newLinkedHashSet, s1, s2, s3);
        Assert.assertEquals(Sets.newLinkedHashSet(Sets.newTreeSet(expected)), mergedLinkedSet);
    }

    @Test
    public void testSize() {
        Assert.assertEquals(0, CollectionUtils.size(null));
        Assert.assertEquals(1, CollectionUtils.size(Sets.newHashSet(1, 1, 1)));
    }
}
