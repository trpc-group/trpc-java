package com.tencent.trpc.spring.context.configuration.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class AbstractProtocolSchemaTest {

    private AbstractProtocolSchema schema;

    @Before
    public void setUp() {
        schema = new AbstractProtocolSchema() {

        };
    }

    @Test
    public void testProtocolSetterAndGetter() {
        String testProtocol = "testProtocol";
        schema.setProtocol(testProtocol);
        assertEquals(testProtocol, schema.getProtocol());
    }

    @Test
    public void testExtMapSetterAndGetter() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key", "value");
        schema.setExtMap(testMap);
        assertEquals(testMap, schema.getExtMap());
    }

    @Test
    public void testExtMapDefault() {
        assertEquals(0, schema.getExtMap().size());
    }

    @Test
    public void testBooleanSetterAndGetter() {
        schema.setLazyinit(Boolean.FALSE);
        assertFalse(schema.getLazyinit());
    }

    @Test
    public void testIntegerSetterAndGetter() {
        schema.setPayload(1024);
        assertEquals(Integer.valueOf(1024), schema.getPayload());
    }

    @Test
    public void testSetProtocolType() {
        schema.setProtocolType("type1");
        String protocolType = schema.getProtocolType();
        assertEquals("type1", protocolType);
    }

    @Test
    public void setCompressMinBytes() {
        schema.setCompressMinBytes(10);
        Integer compressMinBytes = schema.getCompressMinBytes();
        assertEquals(10L, Long.parseLong(compressMinBytes + ""));
    }
}