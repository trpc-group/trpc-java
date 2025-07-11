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

package com.tencent.trpc.core.common;

public class TRpcSystemProperties {

    public static final String CONFIG_PATH = "trpc_config_path";
    public static final String CONFIG_TYPE = "trpc_config_type";
    /**
     * Compatible with unused trpc_ prefix
     */
    public static final String CONTAINER_TYPE = Constants.CONTAINER_TYPE;
    public static final String IGNORE_SAME_PLUGIN_NAME = "trpc_ignore_same_plugin_name";

    public static String getProperties(String key, String def) {
        return System.getProperty(key, def);
    }

    public static String getProperties(String key) {
        return System.getProperty(key);
    }

    public static String setProperties(String key, String value) {
        return System.setProperty(key, value);
    }

    public static boolean isIgnoreSamePluginName() {
        String flag = getProperties(IGNORE_SAME_PLUGIN_NAME, Boolean.FALSE.toString());
        return Boolean.parseBoolean(flag);
    }

    public static void setIgnoreSamePluginName(boolean flag) {
        setProperties(IGNORE_SAME_PLUGIN_NAME, Boolean.toString(flag));
    }

}
