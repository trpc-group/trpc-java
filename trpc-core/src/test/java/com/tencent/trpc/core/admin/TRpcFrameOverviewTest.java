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

package com.tencent.trpc.core.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TRpcFrameOverviewTest {

    @Test
    public void test() {
        String trpcName = TRpcFrameOverview.getName();
        assertEquals(trpcName, "trpc-java");
        Assertions.assertNotNull(TRpcFrameOverview.getVersion());
    }

}
