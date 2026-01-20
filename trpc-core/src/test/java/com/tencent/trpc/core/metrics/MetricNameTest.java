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

package com.tencent.trpc.core.metrics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * MetricNameTest
 */
public class MetricNameTest {

    @Test
    public void testTestEquals() {
        MetricName labelName = MetricName.build("test", null);
        Assertions.assertNotNull(labelName.getLabels());

        MetricName name = MetricName.build("test");
        MetricName name1 = MetricName.build("test");
        Assertions.assertEquals(name, name1);

        name = MetricName.build("test", "1", "2");
        name1 = MetricName.build("test", "1", "2");
        Assertions.assertEquals(name, name1);

        name = MetricName.build("test", "1");
        name1 = MetricName.build("test", "1", "2");
        Assertions.assertNotEquals(name, name1);

        Assertions.assertEquals(name.getName(), name1.getName());
        Assertions.assertNotEquals(name.getLabels().length, name1.getLabels().length);
        Assertions.assertNotSame(name.hashCode(), name.hashCode());

        Assertions.assertNotSame("", name.toString());
        Assertions.assertNotSame(MetricName.build("test", "1").hashCode(),
                MetricName.build("test", "1").hashCode());
    }
}
