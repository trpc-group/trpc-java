package com.tencent.trpc.spring.context.configuration.schema.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServerSchemaTest {

    @Test
    public void testSetMethod() {
        ServerSchema serverSchema = new ServerSchema();
        serverSchema.setCloseTimeout(2000L);
        serverSchema.setWaitTimeout(2000L);
        Assertions.assertEquals(Long.valueOf(2000), serverSchema.getWaitTimeout());
        Assertions.assertEquals(Long.valueOf(2000), serverSchema.getCloseTimeout());
    }
}
