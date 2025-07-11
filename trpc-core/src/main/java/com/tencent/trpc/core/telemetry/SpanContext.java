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

package com.tencent.trpc.core.telemetry;

import com.tencent.trpc.core.rpc.RpcContext;

/**
 * Telemetry span context information, abstract decoupling not coupled with specific link frameworks.
 *
 * <p>This abstraction can be used in RpcContextUtils to obtain context link information, and various business plugins
 * can obtain link content through this.
 * {@link com.tencent.trpc.core.utils.RpcContextUtils#getParentSpan(RpcContext)}
 * Therefore, telemetry plugins are generally configured globally only once, and are added to the context through
 * {@link com.tencent.trpc.core.utils.RpcContextUtils#putValueMapValue(RpcContext, String, Object)} method;
 * when reporting links, data can be obtained directly from the context.</p>
 */
public interface SpanContext {

    /**
     * Invalid traceId number.
     */
    String INVALID_TRACE_ID = "00000000000000000000000000000000";
    /**
     * Invalid spanId number.
     */
    String INVALID_SPAN_ID = "0000000000000000";

    /**
     * Definition of invalid span information, can be provided for failed link generation, no link information or unit
     * testing, etc.
     */
    SpanContext INVALID = new SpanContext() {
        @Override
        public String getTraceId() {
            return INVALID_TRACE_ID;
        }

        @Override
        public String getSpanId() {
            return INVALID_SPAN_ID;
        }

        @Override
        public boolean isSampled() {
            return false;
        }
    };

    /**
     * Get the link traceId information.
     *
     * @return the link traceId
     */
    String getTraceId();

    /**
     * Get the link spanId information.
     *
     * @return the link spanId
     */
    String getSpanId();

    /**
     * Whether the sampling flag is set.
     *
     * @return true if sampled, false otherwise
     */
    boolean isSampled();

}
