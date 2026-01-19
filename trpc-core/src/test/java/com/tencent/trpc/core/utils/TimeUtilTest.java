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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TimeUtilTest {

    @Test
    public void testConvertTimeUnitStringToMills() {
        int day = TimeUtil.convertTimeUnitStringToMills("5d");
        Assertions.assertEquals(day, 5 * 24 * 60 * 60 * 1000);
        int hour = TimeUtil.convertTimeUnitStringToMills("54h");
        Assertions.assertEquals(hour, 54 * 60 * 60 * 1000);
        int second = TimeUtil.convertTimeUnitStringToMills("345m");
        Assertions.assertEquals(second, 345 * 60 * 1000);
        int ms = TimeUtil.convertTimeUnitStringToMills("345343ms");
        Assertions.assertEquals(345343, ms);
        int s = TimeUtil.convertTimeUnitStringToMills("34443s");
        Assertions.assertEquals(s, 34443 * 1000);
        int seconds = TimeUtil.convertTimeUnitStringToSeconds("3442s");
        Assertions.assertEquals(seconds, 3442);
    }
}
