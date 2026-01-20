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

public class TimerUtilTest {

    @Test
    public void testTimer() throws InterruptedException {
        TimerUtil timerUtil = TimerUtil.newInstance();
        timerUtil.start();
        Thread.sleep(50);
        timerUtil.end();
        long cost = timerUtil.getCost();
        Assertions.assertTrue(cost >= 25);
        timerUtil.reset();
        Assertions.assertTrue(timerUtil.getCost() == 0);
        timerUtil.nstart();
        Thread.sleep(10);
        timerUtil.nend();
        Assertions.assertTrue(timerUtil.ngetCost() > 1);
        timerUtil.getMinCost();
        timerUtil.getTotalCost();
        Assertions.assertTrue(timerUtil.getMaxCost() > 1);
        Assertions.assertTrue(timerUtil.getTotalCost() > 1);
    }
}
