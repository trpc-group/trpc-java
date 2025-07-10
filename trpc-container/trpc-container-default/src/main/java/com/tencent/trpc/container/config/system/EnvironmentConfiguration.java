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

package com.tencent.trpc.container.config.system;

/**
 * Environment variable configuration.
 */
public class EnvironmentConfiguration implements Configuration {

    /**
     * Actual method to get the property.
     *
     * @param key specified property key
     * @return value
     */
    @Override
    public Object getInternalProperty(String key) {
        return System.getenv(key);
    }

}
