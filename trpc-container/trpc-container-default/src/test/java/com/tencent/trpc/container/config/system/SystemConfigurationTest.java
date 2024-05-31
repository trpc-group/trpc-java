package com.tencent.trpc.container.config.system;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

public class SystemConfigurationTest{

    @Test
    public void testGetInternalProperty() {
        System.setProperty("testSysProperty","1");
        SystemConfiguration systemConfiguration = new SystemConfiguration();
        Object result = systemConfiguration.getInternalProperty("testSysProperty");
        Assert.assertNotNull(result);
    }
}