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

package com.tencent.trpc.springmvc;

import com.tencent.trpc.proto.http.common.HttpConstants;

/**
 * HTTP Constants
 */
public class TRpcHttpConstants extends HttpConstants {


    public static final String DEFAULT_TRPC_BASE_PATH = RPC_CALL_DEFAULT_PATH;

    public static final String TRPC_PARAM_SERVICE = RPC_CALL_PARAM_SERVICE;
    public static final String TRPC_PARAM_METHOD = RPC_CALL_PARAM_METHOD;

    public static final String TRPC_SERVICE_NAME_PATTERN = "trpc.%s.%s.%s";

}
