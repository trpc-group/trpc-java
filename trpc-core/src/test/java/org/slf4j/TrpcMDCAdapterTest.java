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

package org.slf4j;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.spi.MDCAdapter;

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
        Assertions.assertNotNull(adapter);
    }
}
