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

package com.tencent.trpc.core.metrics;

import org.junit.Assert;
import org.junit.Test;

/**
 * MetricNameTest
 */
public class MetricNameTest {

    @Test
    public void testTestEquals() {
        MetricName labelName = MetricName.build("test", null);
        Assert.assertNotNull(labelName.getLabels());

        MetricName name = MetricName.build("test");
        MetricName name1 = MetricName.build("test");
        Assert.assertEquals(name, name1);

        name = MetricName.build("test", "1", "2");
        name1 = MetricName.build("test", "1", "2");
        Assert.assertEquals(name, name1);

        name = MetricName.build("test", "1");
        name1 = MetricName.build("test", "1", "2");
        Assert.assertNotEquals(name, name1);

        Assert.assertEquals(name.getName(), name1.getName());
        Assert.assertNotEquals(name.getLabels().length, name1.getLabels().length);
        Assert.assertNotSame(name.hashCode(),name.hashCode());

        Assert.assertNotSame("", name.toString());
        Assert.assertNotSame(MetricName.build("test", "1").hashCode(),
                MetricName.build("test", "1").hashCode());
    }
}