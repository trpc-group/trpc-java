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

package com.tencent.trpc.opentelemetry.sdk;

public final class Constants {

    public static final String CTX_TELEMETRY_CONTEXT = "ctx_telemetry_context";
    public static final String CALLEE_SERVICE_KEY = "trpc.callee_service";
    public static final String CALLEE_METHOD_KEY = "trpc.callee_method";
    public static final String CALLER_SERVICE_KEY = "trpc.caller_service";
    public static final String CALLER_METHOD_KEY = "trpc.caller_method";
    public static final String DYEING_KEY = "tps.dyeing";
    public static final String ENV_NAME_KEY = "trpc.envname";
    public static final String NAMESPACE_KEY = "trpc.namespace";
    public static final String BODY_KEY = "message.detail.";
    public static final String ATTACH_KEY = "message.attach";
    public static final String META_KEY = "message.meta";
    public static final String DETAIL_KEY = "message.detail";
    public static final String SENT_KEY = "SENT";
    public static final String RECEIVED_KEY = "RECEIVED";
    public static final String CONSTANT_TENANT_ID_KEY = "tps.tenant.id";
    public static final String NAMESPACE = "service.namespace";
    public static final String ENV = "env";
    public static final String SERVER_ID = "server.id";
    public static final String SERVICE_NAME = "service.name";
    public static final String CLIENT_REQUEST_METRICS_STATE_KEY = "trpc-client-request-metrics-state";
    public static final String SERVER_REQUEST_METRICS_STATE_KEY = "trpc-server-request-metrics-state";
    public static final String TRPC = "TRPC";

    private Constants() {
    }

}
