package com.tencent.trpc.logger.admin;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ch.qos.logback.classic.LoggerContext;
import org.apache.logging.slf4j.Log4jLoggerFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLoggerFactory;

@RunWith(PowerMockRunner.class)
public class LoggerProcessUnitFactoryTest {

    @Test
    @PrepareForTest({LoggerFactory.class})
    public void testGetGetLoggerProcessUnit() {
        PowerMockito.mockStatic(LoggerFactory.class);
        PowerMockito.when(LoggerFactory.getILoggerFactory()).thenReturn(new Log4jLoggerFactory());
        LoggerProcessUnit log = LoggerProcessUnitFactory.getLoggerProcessUnit();
        assertNotNull(log);
        assertTrue(log instanceof Log4j2LoggerProcessUnit);
    }


    @Test
    @PrepareForTest({LoggerFactory.class})
    public void testLogback() {
        PowerMockito.mockStatic(LoggerFactory.class);
        PowerMockito.when(LoggerFactory.getILoggerFactory()).thenReturn(new LoggerContext());
        LoggerProcessUnit log = LoggerProcessUnitFactory.getLoggerProcessUnit();
        assertNotNull(log);
        assertTrue(log instanceof LogbackLoggerProcessUnit);
        log = LoggerProcessUnitFactory.getLoggerProcessUnit();
        assertNotNull(log);
    }

    @Test
    @PrepareForTest({LoggerFactory.class})
    public void testUnSupport() {
        PowerMockito.mockStatic(LoggerFactory.class);
        PowerMockito.when(LoggerFactory.getILoggerFactory()).thenReturn(new NOPLoggerFactory());
        LoggerProcessUnit log = LoggerProcessUnitFactory.getLoggerProcessUnit();
        assertNotNull(log);
        assertTrue(log instanceof UnSupportLoggerProcessUnit);
    }

}