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

package com.tencent.trpc.core.exception;

/**
 * Rate limit fallback exception, used to identify exceptions in the rate limit fallback process.
 */
public class LimiterFallbackException extends LimiterException {

    public LimiterFallbackException() {
    }

    public LimiterFallbackException(String message) {
        super(message);
    }

    public LimiterFallbackException(String message, Throwable cause) {
        super(message, cause);
    }

    public LimiterFallbackException(Throwable cause) {
        super(cause);
    }

    public LimiterFallbackException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
