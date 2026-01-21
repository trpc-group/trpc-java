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

package com.tencent.trpc.core.configcenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ConfigurationEventTest {

    @Test
    public void test() {
        ConfigurationEvent<String, Integer> event = new ConfigurationEvent<>("g1", "key", 1, "add");
        assertEquals("g1", event.getGroupName());
        assertEquals("add", event.getType());
        assertEquals("key", event.getKey());
        assertEquals(1, (int) event.getValue());
        assertTrue(event.hashCode() > 0);
        ConfigurationEvent<String, Integer> event2 = new ConfigurationEvent<>("g1", "key", 1, "add");
        assertEquals(event, event2);
    }
}
