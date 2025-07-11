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

package com.tencent.trpc.logger.admin;

import com.tencent.trpc.core.logger.LoggerLevel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class UnSupportLoggerProcessUnitTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private UnSupportLoggerProcessUnit unSupportLoggerProcessUnit;

    @Before
    public void setUp() {
        unSupportLoggerProcessUnit = new UnSupportLoggerProcessUnit();
    }

    @Test
    public void testInit() {
        unSupportLoggerProcessUnit.init();
    }

    @Test
    public void testGetLoggers() {
        expectedEx.expect(UnsupportedOperationException.class);
        expectedEx.expectMessage("Current log frame doesn't support this operation!");
        unSupportLoggerProcessUnit.getLoggers();
    }

    @Test
    public void testGetLoggerLevelInfo() {
        expectedEx.expect(UnsupportedOperationException.class);
        expectedEx.expectMessage("Current log frame doesn't support this operation!");
        unSupportLoggerProcessUnit.getLoggerLevelInfo();
    }

    @Test
    public void testSetLoggerLevel() {
        expectedEx.expect(UnsupportedOperationException.class);
        expectedEx.expectMessage("Current log frame doesn't support this operation!");
        unSupportLoggerProcessUnit.setLoggerLevel("a", LoggerLevel.ALL);
    }

}