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

package com.tencent.trpc.core.common.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import org.junit.Test;

public class NamingOptionsTest {

    @Test
    public void test() {
        NamingOptions options = NamingOptions.parseNamingUrl("selectorid://naming?k=v", new HashMap<>());
        assertEquals("selectorid", options.getSelectorId());
        assertEquals("naming", options.getServiceNaming());
        assertEquals("v", options.getExtMap().get("k"));
        assertEquals("ip://127.0.0.1:1234", NamingOptions.toDirectNamingUrl("127.0.0.1:1234"));
        assertEquals("polaris://hello", NamingOptions.toNamingUrl("polaris", "hello"));
        options.setSelectorId("b");
        options.setServiceNaming("bb");
        options.setExtMap(null);
        assertEquals("b", options.getSelectorId());
        assertEquals("bb", options.getServiceNaming());
        assertNull(options.getExtMap());
    }
}
