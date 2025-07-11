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
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class Log4j2LoggerProcessUnitTest {

    public static final String UNIT_TEST = "unit-test";
    
    private Log4j2LoggerProcessUnit log4j2LoggerProcessUnit;
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        log4j2LoggerProcessUnit = new Log4j2LoggerProcessUnit();
    }

    @Test
    public void testInit() {
        log4j2LoggerProcessUnit.init();
    }

    @Test
    public void testSetLogger() {
        log4j2LoggerProcessUnit.addLogger(UNIT_TEST, new LoggerConfig());
        String logger = log4j2LoggerProcessUnit.setLoggerLevel(UNIT_TEST, LoggerLevel.ALL);
        Assert.assertEquals(logger, "ERROR");
    }

}