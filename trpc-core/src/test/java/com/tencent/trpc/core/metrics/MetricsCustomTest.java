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

import static org.junit.Assert.assertEquals;

import com.tencent.trpc.core.metrics.MetricsCustom.StatPolicy;
import com.tencent.trpc.core.metrics.MetricsCustom.StatValue;
import org.junit.Test;

public class MetricsCustomTest {

    @Test
    public void testStatValue() {
        StatValue statValue = StatValue.of(100.0);
        assertEquals(100.0d, statValue.value, 0.0);
        StatValue statValue2 = StatValue.of(100.0, 10, StatPolicy.AVG);
        assertEquals(100.0d, statValue2.value, 0.0);
        assertEquals(10, statValue2.count);
    }
}