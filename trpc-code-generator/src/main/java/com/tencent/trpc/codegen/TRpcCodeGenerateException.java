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

package com.tencent.trpc.codegen;

/**
 * Exception occurring during the tRPC code generating
 */
public class TRpcCodeGenerateException extends RuntimeException {
    public TRpcCodeGenerateException(String message) {
        super(message);
    }

    public TRpcCodeGenerateException(String message, Throwable cause) {
        super(message, cause);
    }
}
