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

package com.tencent.trpc.spring.test;

import com.tencent.trpc.core.common.ConfigManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * tRPC test utils
 */
public class TRpcConfigManagerTestUtils {

    /**
     * Reset ConfigManager
     */
    public static void resetConfigManager() {
        try {
            Constructor<ConfigManager> constructor = ConfigManager.class.getDeclaredConstructor();
            boolean originConstructorAccessible = constructor.isAccessible();
            constructor.setAccessible(true);
            ConfigManager configManager = constructor.newInstance();
            constructor.setAccessible(originConstructorAccessible);

            Field instanceField = ConfigManager.class.getDeclaredField("instance");
            boolean originFieldAccessible = instanceField.isAccessible();
            instanceField.setAccessible(true);
            instanceField.set(null, configManager);
            instanceField.setAccessible(originFieldAccessible);
        } catch (Exception e) {
            throw new IllegalStateException("reset ConfigManager instance error", e);
        }
    }

}
