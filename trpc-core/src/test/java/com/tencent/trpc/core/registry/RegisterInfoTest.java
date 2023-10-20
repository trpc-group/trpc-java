/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 THL A29 Limited, a Tencent company. 
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.core.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class RegisterInfoTest {

    @Test
    public void test() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("int", 1);
        map.put("short", 1);
        map.put("byte", 1);
        map.put("string", "string");
        map.put("boolean", false);
        map.put("long", 10L);
        map.put("double", 1.0d);
        map.put("float", 2.0d);
        RegisterInfo registerInfo = new RegisterInfo("proto", "host", 80, "servicename", "group", "v1", map);

        assertEquals("proto", registerInfo.getProtocol());
        assertEquals("servicename", registerInfo.getServiceName());
        assertEquals("proto", registerInfo.getProtocol());
        assertEquals("host", registerInfo.getHost());
        assertEquals(80, registerInfo.getPort());
        assertEquals("host:80", registerInfo.getAddress());
        assertEquals("group", registerInfo.getGroup());
        assertEquals("v1", registerInfo.getVersion());

        assertEquals("string", registerInfo.getParameter("string"));
        assertEquals("string", registerInfo.getObject("string"));
        assertEquals("string", registerInfo.getParameter("stringx", "string"));
        assertNull(registerInfo.getParameter("stringx"));

        assertEquals(2, registerInfo.getParameter("doublex", 2.0d), 0.0);
        assertEquals(1, registerInfo.getParameter("double", 2.0d), 0.0);

        assertEquals(3, registerInfo.getParameter("floatx", 3.0f), 0.0);
        assertEquals(2, registerInfo.getParameter("float", 3.0f), 0.0);

        assertEquals(12, registerInfo.getParameter("longx", 12L));
        assertEquals(10, registerInfo.getParameter("long", 12L));

        assertEquals(2, registerInfo.getParameter("intx", 2));
        assertEquals(1, registerInfo.getParameter("int", 2));

        assertEquals(2, registerInfo.getParameter("shortx", (short) 2));
        assertEquals(1, registerInfo.getParameter("short", (short) 2));

        assertEquals(2, registerInfo.getParameter("bytex", (byte) 2));
        assertEquals(1, registerInfo.getParameter("byte", (byte) 2));

        assertTrue(registerInfo.getParameter("booleanx", true));
        assertFalse(registerInfo.getParameter("boolean", true));

        RegisterInfo registerInfo2 = new RegisterInfo("trpc", "0.0.0.0", 12001,
                "group", "v1", "test.service1");
        assertEquals("trpc", registerInfo2.getProtocol());
        assertEquals("0.0.0.0", registerInfo2.getHost());
        assertEquals(12001, registerInfo2.getPort());

        assertEquals("proto://host:80/servicename", registerInfo.toIdentityString());
        RegisterInfo registerInfoClone =
                new RegisterInfo("proto", "host", 80, "servicename", "group", "v1", map);
        Assert.assertTrue(registerInfo.hashCode() > 0);
        String[] strings = new String[]{"20"};
        assertEquals(strings[0], registerInfo.getParameter("20", strings)[0]);
        assertEquals(registerInfo, registerInfoClone);
        registerInfo.compareTo(registerInfoClone);
        registerInfo.toString();
        registerInfo.getIdentity();
        Assert.assertNotNull(registerInfo.clone());
        registerInfo.buildString(true, true, true, "a", "b");
        assertEquals(80, registerInfo.getPort(10));
        assertEquals(80, registerInfo.getPort(10));
        assertEquals(80, registerInfo.getPort(10));

    }

    @Test
    public void testEncodeAndDecode() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("int", 1);
        map.put("short", 1);
        map.put("byte", 1);
        map.put("string", "string");
        map.put("boolean", false);
        map.put("long", 10L);
        map.put("double", 1.0d);
        map.put("float", 2.0d);
        RegisterInfo registerInfo = new RegisterInfo("trpc", "127.0.0.1", 80,
                "test.server1", "group", "v1", map);

        String url = RegisterInfo.encode(registerInfo);
        Assert.assertEquals(
                "trpc%3A%2F%2F127.0.0.1%3A80%2Ftest.server1%3Fboolean%3Dfalse%26byte"
                        + "%3D1%26double%3D1.0%26float%3D2.0%26int%3D1%26long%3D10%26short%3D1%26"
                        + "string%3Dstring",
                url);

        assertEquals(registerInfo, RegisterInfo.decode(url));
    }
}
