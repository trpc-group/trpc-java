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

import org.junit.Assert;
import org.junit.Test;

public class TimerUtilTest {

    @Test
    public void testTimer() throws InterruptedException {
        TimerUtil timerUtil = TimerUtil.newInstance();
        timerUtil.start();
        Thread.sleep(50);
        timerUtil.end();
        long cost = timerUtil.getCost();
        Assert.assertTrue(cost >= 25);
        timerUtil.reset();
        Assert.assertTrue(timerUtil.getCost() == 0);
        timerUtil.nstart();
        Thread.sleep(10);
        timerUtil.nend();
        Assert.assertTrue(timerUtil.ngetCost() > 1);
        timerUtil.getMinCost();
        timerUtil.getTotalCost();
        Assert.assertTrue(timerUtil.getMaxCost() > 1);
        Assert.assertTrue(timerUtil.getTotalCost() > 1);
    }
}
