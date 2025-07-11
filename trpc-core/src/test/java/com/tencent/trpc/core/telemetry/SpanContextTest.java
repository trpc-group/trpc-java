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

import junit.framework.TestCase;
import org.junit.Test;

public class SpanContextTest extends TestCase {

    @Test
    public void testInvalidContext() {
        assertEquals(SpanContext.INVALID.getTraceId(), SpanContext.INVALID_TRACE_ID);
        assertEquals(SpanContext.INVALID.getSpanId(), SpanContext.INVALID_SPAN_ID);
        assertFalse(SpanContext.INVALID.isSampled());
    }
}