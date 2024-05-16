package com.tencent.trpc.logger.admin;

import com.tencent.trpc.core.logger.LoggerLevel;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class Log4j2LoggerProcessUnitTest {

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
        log4j2LoggerProcessUnit.addLogger("unit-test", new LoggerConfig());
        String logger = log4j2LoggerProcessUnit.setLoggerLevel("unit-test", LoggerLevel.ALL);
        Assert.assertEquals(logger, "ERROR");
    }

}