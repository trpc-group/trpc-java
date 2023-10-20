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

package com.tencent.trpc.core.exception;

/**
 * Rate limit top-level exception class. All custom rate limit related exceptions should be direct or indirect
 * subclasses of this exception class.
 */
public class LimiterException extends RuntimeException {

    public LimiterException() {
    }

    public LimiterException(String message) {
        super(message);
    }

    public LimiterException(String message, Throwable cause) {
        super(message, cause);
    }

    public LimiterException(Throwable cause) {
        super(cause);
    }

    public LimiterException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
