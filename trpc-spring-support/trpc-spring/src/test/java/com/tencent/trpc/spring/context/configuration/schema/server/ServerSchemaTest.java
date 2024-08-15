package com.tencent.trpc.spring.context.configuration.schema.server;

import org.junit.Assert;
import org.junit.Test;

public class ServerSchemaTest {

    @Test
    public void testSetMethod() {
        ServerSchema serverSchema = new ServerSchema();
        serverSchema.setCloseTimeout(2000L);
        serverSchema.setWaitTimeout(2000L);
        Assert.assertEquals(Long.valueOf(2000), serverSchema.getWaitTimeout());
        Assert.assertEquals(Long.valueOf(2000), serverSchema.getCloseTimeout());
    }
}
