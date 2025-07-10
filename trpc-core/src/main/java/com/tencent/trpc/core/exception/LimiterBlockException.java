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
 * Rate limit callback exception, used to identify exceptions in the rate limit callback process.
 */
public class LimiterBlockException extends LimiterException {

    public LimiterBlockException() {
    }

    public LimiterBlockException(String message) {
        super(message);
    }

    public LimiterBlockException(String message, Throwable cause) {
        super(message, cause);
    }

    public LimiterBlockException(Throwable cause) {
        super(cause);
    }

    public LimiterBlockException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
