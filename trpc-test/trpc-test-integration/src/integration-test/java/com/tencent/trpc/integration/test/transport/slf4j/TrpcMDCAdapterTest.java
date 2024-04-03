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

package com.tencent.trpc.integration.test.transport.slf4j;

import com.tencent.trpc.integration.test.TrpcServerApplication;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.TrpcMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TrpcServerApplication.class)
@ActiveProfiles("transport")
public class TrpcMDCAdapterTest {

    @Test
    public void testTrpcMDCAdapter() {
        TrpcMDCAdapter test = new TrpcMDCAdapter();
        MDCAdapter adapter = TrpcMDCAdapter.init();
        adapter.put("traceId", "test");
        adapter.get("traceId");
        adapter.remove("traceId");
        adapter.clear();
        Map<String, String> contextMap = new HashMap<>();
        adapter.setContextMap(contextMap);
        adapter.getCopyOfContextMap();
        Assert.assertNotNull(adapter);
    }
}
