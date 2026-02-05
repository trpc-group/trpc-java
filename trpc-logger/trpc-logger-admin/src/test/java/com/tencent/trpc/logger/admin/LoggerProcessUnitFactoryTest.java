package com.tencent.trpc.logger.admin;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.LoggerContext;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLoggerFactory;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class LoggerProcessUnitFactoryTest {

    @BeforeEach
    public void setUp() throws Exception {
        resetLoggerProcessUnit();
    }

    @AfterEach
    public void tearDown() throws Exception {
        resetLoggerProcessUnit();
    }

    /**
     * 通过反射重置 LoggerProcessUnitFactory 的静态变量
     */
    private void resetLoggerProcessUnit() throws Exception {
        Field field = LoggerProcessUnitFactory.class.getDeclaredField("loggerProcessUnit");
        field.setAccessible(true);
        field.set(null, null);
    }

    @Test
    public void testGetGetLoggerProcessUnit() {
        try (MockedStatic<LoggerFactory> mockedLoggerFactory = Mockito.mockStatic(LoggerFactory.class);
             MockedStatic<LoggerFactoryEnum> mockedEnum = Mockito.mockStatic(LoggerFactoryEnum.class)) {
            // 模拟 LoggerFactoryEnum.getLoggerFactoryEnum 返回 LOG4J2_FACTORY
            mockedLoggerFactory.when(LoggerFactory::getILoggerFactory).thenReturn(new NOPLoggerFactory());
            mockedEnum.when(() -> LoggerFactoryEnum.getLoggerFactoryEnum(Mockito.anyString()))
                    .thenReturn(LoggerFactoryEnum.LOG4J2_FACTORY);
            LoggerProcessUnit log = LoggerProcessUnitFactory.getLoggerProcessUnit();
            assertNotNull(log);
            assertTrue(log instanceof Log4j2LoggerProcessUnit);
        }
    }

    @Test
    public void testLogback() {
        try (MockedStatic<LoggerFactory> mockedLoggerFactory = Mockito.mockStatic(LoggerFactory.class)) {
            mockedLoggerFactory.when(LoggerFactory::getILoggerFactory).thenReturn(new LoggerContext());
            LoggerProcessUnit log = LoggerProcessUnitFactory.getLoggerProcessUnit();
            assertNotNull(log);
            assertTrue(log instanceof LogbackLoggerProcessUnit);
            log = LoggerProcessUnitFactory.getLoggerProcessUnit();
            assertNotNull(log);
        }
    }

    @Test
    public void testUnSupport() {
        try (MockedStatic<LoggerFactory> mockedLoggerFactory = Mockito.mockStatic(LoggerFactory.class)) {
            mockedLoggerFactory.when(LoggerFactory::getILoggerFactory).thenReturn(new NOPLoggerFactory());
            LoggerProcessUnit log = LoggerProcessUnitFactory.getLoggerProcessUnit();
            assertNotNull(log);
            assertTrue(log instanceof UnSupportLoggerProcessUnit);
        }
    }

}
