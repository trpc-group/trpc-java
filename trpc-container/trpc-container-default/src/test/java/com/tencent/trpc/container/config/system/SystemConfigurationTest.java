package com.tencent.trpc.container.config.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SystemConfigurationTest {

    @Test
    public void testGetInternalProperty() {
        System.setProperty("testSysProperty", "1");
        SystemConfiguration systemConfiguration = new SystemConfiguration();
        Object result = systemConfiguration.getInternalProperty("testSysProperty");
        assertEquals("1", result);
    }
}
