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
 * Rate limit rule data source exception class. If there is an exception when registering a rate limit rule data
 * source, throw this class or its subclass.
 */
public class LimiterDataSourceException extends LimiterException {

    public LimiterDataSourceException() {
    }

    public LimiterDataSourceException(String message) {
        super(message);
    }

    public LimiterDataSourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public LimiterDataSourceException(Throwable cause) {
        super(cause);
    }

    public LimiterDataSourceException(String message, Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
