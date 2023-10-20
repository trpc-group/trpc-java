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

import com.tencent.trpc.core.common.Version;

/**
 * Provide a basic framework information view for Admin
 */
public class TRpcFrameOverview {

    public static final String UNKNOWN = "<unknown>";

    private static final String TRPC_FRAME_NAME = "trpc-java";

    public static String getName() {
        return TRPC_FRAME_NAME;
    }

    public static String getVersion() {
        return Version.version();
    }

}
