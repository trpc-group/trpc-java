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

package com.tencent.trpc.core.rpc;

/**
 * Rpc context value key list.
 */
public class RpcContextValueKeys {

    public static final String CTX_TRACER = "ctx_tracer";
    public static final String CTX_TRACE_SPAN = "ctx_trace_span";

    /**
     * Telemetry span context.
     */
    public static final String CTX_TELEMETRY_TRACE_SPAN = "ctx_telemetry_trace_span";
    /**
     * Remote IP.
     */
    public static final String CTX_CALLER_REMOTE_IP = "ctx_caller_remote_ip";
    /**
     * Full link timeout key.
     */
    public static final String CTX_LINK_INVOKE_TIMEOUT = "ctx_link_invoke_timeout";
    /**
     * Server-side body signature verification result.
     */
    public static final String SERVER_SIGNATURE_VERIFY_RESULT_KEY = "server_ctx_signature_verify_result";
    /**
     * Metrics extension field 3 key.
     */
    public static final String CTX_M007_EXT3 = "ctx_m007_ext3";
    /**
     * Rpc invocation interface information configuration key.
     */
    public static final String RPC_INVOCATION_KEY = "ctx_rpc_invocation";
    /**
     * Rpc call information configuration key.
     */
    public static final String RPC_CALL_INFO_KEY = "ctx_rpc_call_info";

}
