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

import com.tencent.trpc.core.configcenter.spi.ConfigurationLoader;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfigurationManagerTest {

    @Test
    public void testGetConfigLoader() {
        TRpcExtensionException exception = Assertions.assertThrows(TRpcExtensionException.class, () -> {
            ConfigurationManager.getConfigurationLoader("rainbow");
        });
        Assertions.assertTrue(exception.getMessage().contains("Cannot get extension of type"));
    }
}
