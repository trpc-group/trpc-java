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

package com.tencent.trpc.selector.open.polaris.common;

import com.tencent.trpc.polaris.common.PolarisConstant;
import org.junit.Assert;
import org.junit.Test;

public class PolarisConstantsTest {

    @Test
    public void testTrpcPolarisKey() {
        Assert.assertNull(PolarisConstant.TrpcPolarisParams.getByKey("not exist"));
        Assert.assertTrue(
                PolarisConstant.TrpcPolarisParams
                        .getByKey(PolarisConstant.TrpcPolarisParams.INCLUDE_CIRCUITBREAK.getKey()).isTrpcPluginKey());
    }
}
