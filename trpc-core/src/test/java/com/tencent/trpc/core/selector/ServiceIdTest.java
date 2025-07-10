/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 Tencent.
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.core.selector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

public class ServiceIdTest {

    @Test
    public void test() {
        ServiceId serviceId = new ServiceId();
        serviceId.setGroup("group");
        serviceId.setParameters(ImmutableMap.of("int", 1, "string", "a", "boolean", false));
        serviceId.setServiceName("serviceName");
        serviceId.setVersion("version");
        serviceId.setCallerServiceName("callerServiceName");
        serviceId.setCallerNamespace("callerNamespace");
        serviceId.setCallerEnvName("callerEnvName");
        assertEquals(serviceId.getGroup(), "group");
        assertEquals(serviceId.getServiceName(), "serviceName");
        assertEquals(serviceId.getVersion(), "version");
        assertEquals(serviceId.getParameter("string"), "a");
        assertFalse(serviceId.getParameter("boolean", true));
        assertTrue(serviceId.getParameter("boolean2", true));
        assertEquals(serviceId.getParameter("int", 2), 1);
        assertEquals(serviceId.getParameter("int2", 2), 2);
        assertEquals(serviceId.getParameter("int2", 2L), 2L);
        assertEquals(serviceId.getParameter("string", "b"), "a");
        assertEquals(serviceId.getParameter("string2", "b"), "b");
        assertTrue(serviceId.toString().contains("ServiceId [serviceName="));
        assertTrue(serviceId.toSimpleString().contains("ServiceId {serviceName="));
        assertEquals(serviceId.getCallerServiceName(), "callerServiceName");
        assertEquals(serviceId.getCallerNamespace(), "callerNamespace");
        assertEquals(serviceId.getCallerEnvName(), "callerEnvName");
    }
}
