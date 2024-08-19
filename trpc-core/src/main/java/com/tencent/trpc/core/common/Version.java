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

package com.tencent.trpc.core.common;

/**
 * Framework version information, modify when releasing.
 */
public class Version {

    /**
     * TRPC version number rules
     * 1. MAJOR version when you make incompatible API changes;
     * 2. MINOR version when you add functionality in a backwards-compatible manner;
     * 3. PATCH version when you make backwards-compatible bug fixes;
     * 4. Additional labels for pre-release and build metadata are available as extensions to the
     * MAJOR.MINOR.PATCH format;
     * Public beta version: MAJOR.MINOR.PATCH-SNAPSHOT, eg: 0.14.0-SNAPSHOT
     * Official release version: MAJOR.MINOR.PATCH, eg: 0.14.0
     */
    public static final String VERSION_SUFFIX = "-SNAPSHOT";
    /**
     * VERSION: Do not modify the line number of this line. If you want to modify it, be sure to change deploy.sh at
     * the same time.
     */
    public static final String VERSION = "v1.2.0";
    public static final String SNAPSHOT_VERSION = VERSION + VERSION_SUFFIX;
    /**
     * IS_FORMAL_VERSION: Do not modify the line number of this line. If you want to modify it, be sure to change
     * deploy.sh atthe same time.
     */
    public static final boolean IS_FORMAL_VERSION = false;

    /**
     * Version returns the version number of the trpc framework.
     *
     * @return the version number
     */
    public static String version() {
        // Formal version
        if (IS_FORMAL_VERSION) {
            return VERSION;
        }
        // SNAPSHOT version
        return SNAPSHOT_VERSION;
    }

}
