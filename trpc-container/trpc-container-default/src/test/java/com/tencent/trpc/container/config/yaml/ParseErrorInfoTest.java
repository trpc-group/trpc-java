package com.tencent.trpc.container.config.yaml;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

public class ParseErrorInfoTest extends TestCase {

    @Test
    public void testInfo() {
        String info = ParseErrorInfo.info("key", "value");
        Assert.assertNotNull(info);
    }

    @Test
    public void testTestInfo() {
        String info = ParseErrorInfo.info("key", "index", "value");
        Assert.assertNotNull(info);
    }
}