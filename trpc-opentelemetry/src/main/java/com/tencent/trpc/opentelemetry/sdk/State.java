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

package com.tencent.trpc.opentelemetry.sdk;

import io.opentelemetry.api.common.Attributes;

/**
 * Define the state value container for the indicator context
 */
public class State {

    private final Attributes attributes;
    private final long startTimeNanos;

    public State(Attributes attributes, long startTimeNanos) {
        this.attributes = attributes;
        this.startTimeNanos = startTimeNanos;
    }

    public Attributes startAttributes() {
        return this.attributes;
    }

    public long startTimeNanos() {
        return this.startTimeNanos;
    }
}
