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

package com.tencent.trpc.container.config.yaml;

public class ParseErrorInfo {

    private static final String CONFIG_MISSING = "[%s] yaml config missing: [%s]";
    private static final String CONFIG_MISSING_LIST = "[%s] yaml config index [%s] missing: [%s]";

    public static String info(String key, String name) {
        return String.format(CONFIG_MISSING, key, name);
    }

    public static String info(String key, String index, String name) {
        return String.format(CONFIG_MISSING_LIST, key, index, name);
    }

}
