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

package com.tencent.trpc.core.selector;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServiceInstanceTest {

    @Test
    public void test() {
        ServiceInstance serviceInstance = new ServiceInstance("127.0.0.1", 12000);
        ServiceInstance serviceInstance1 = new ServiceInstance("127.0.0.1", 12000);
        Assertions.assertEquals(serviceInstance, serviceInstance1);
        Assertions.assertNotNull(serviceInstance.hashCode());
        Assertions.assertEquals(serviceInstance.toFullString(), serviceInstance1.toFullString());

        Assertions.assertNull(serviceInstance.getObject("a"));
        Assertions.assertNull(serviceInstance.getParameter("a"));
        Assertions.assertTrue(serviceInstance.getParameters().isEmpty());
        serviceInstance = new ServiceInstance("127.0.0.1", 12000, false);
        serviceInstance1 = new ServiceInstance("127.0.0.1", 12000, false);
        Assertions.assertEquals(serviceInstance, serviceInstance1);

        serviceInstance = new ServiceInstance("127.0.0.1", 12000, true);
        serviceInstance1 = new ServiceInstance("127.0.0.1", 12000, false);
        Assertions.assertNotEquals(serviceInstance, serviceInstance1);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("int", 1);
        map.put("short", 1);
        map.put("byte", 1);
        map.put("string", "string");
        map.put("boolean", false);
        map.put("long", 10L);
        map.put("double", 1.0d);
        map.put("float", 2.0d);

        Map<String, Object> map1 = new HashMap<String, Object>();
        map1.put("int", 1);
        map1.put("short", 1);
        map1.put("byte", 1);
        map1.put("string", "string");
        map1.put("boolean", false);
        map1.put("long", 10L);
        map1.put("double", 1.0d);
        map1.put("float", 2.0d);

        serviceInstance = new ServiceInstance("127.0.0.1", 12000, map);
        serviceInstance1 = new ServiceInstance("127.0.0.1", 12000, map1);
        Assertions.assertEquals(serviceInstance, serviceInstance1);

        serviceInstance = new ServiceInstance("127.0.0.1", 12000, true, map);
        serviceInstance1 = new ServiceInstance("127.0.0.1", 12000, true, map1);
        Assertions.assertEquals(serviceInstance, serviceInstance1);

        serviceInstance = new ServiceInstance("127.0.0.1", 12000, true, map);
        serviceInstance1 = new ServiceInstance("127.0.0.1", 12000, false, map1);
        Assertions.assertNotEquals(serviceInstance, serviceInstance1);
    }

}
