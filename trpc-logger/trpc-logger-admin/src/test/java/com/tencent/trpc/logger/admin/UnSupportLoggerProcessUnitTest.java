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

package com.tencent.trpc.logger.admin;

import com.tencent.trpc.core.logger.LoggerLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UnSupportLoggerProcessUnitTest {

    private UnSupportLoggerProcessUnit unSupportLoggerProcessUnit;

    @BeforeEach
    public void setUp() {
        unSupportLoggerProcessUnit = new UnSupportLoggerProcessUnit();
    }

    @Test
    public void testInit() {
        unSupportLoggerProcessUnit.init();
    }

    @Test
    public void testGetLoggers() {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> unSupportLoggerProcessUnit.getLoggers());
        assertEquals("Current log frame doesn't support this operation!", exception.getMessage());
    }

    @Test
    public void testGetLoggerLevelInfo() {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> unSupportLoggerProcessUnit.getLoggerLevelInfo());
        assertEquals("Current log frame doesn't support this operation!", exception.getMessage());
    }

    @Test
    public void testSetLoggerLevel() {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> unSupportLoggerProcessUnit.setLoggerLevel("a", LoggerLevel.ALL));
        assertEquals("Current log frame doesn't support this operation!", exception.getMessage());
    }

}
