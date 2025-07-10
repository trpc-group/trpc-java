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

package com.tencent.trpc.codegen.protoc;

/**
 * Result of invoking protoc executable.
 */
public class ProtocExecutionResult {
    private final boolean success;
    private final String message;
    private final String errorMessage;
    private final Throwable cause;

    public ProtocExecutionResult(boolean success, String message, String errorMessage, Throwable cause) {
        this.success = success;
        this.message = message;
        this.errorMessage = errorMessage;
        this.cause = cause;
    }

    /**
     * Creates a successful result
     *
     * @param message message
     * @return {@link ProtocExecutionResult}
     */
    public static ProtocExecutionResult success(String message) {
        return new ProtocExecutionResult(true, message, null, null);
    }

    /**
     * Creates a failure result
     *
     * @param message message
     * @param errorMessage errorMessage
     * @param cause cause
     * @return {@link ProtocExecutionResult}
     */
    public static ProtocExecutionResult fail(String message, String errorMessage, Throwable cause) {
        return new ProtocExecutionResult(false, message, errorMessage, cause);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return "ProtocExecutionResult{"
                + "success=" + success
                + ", message='" + message + '\''
                + ", errorMessage='" + errorMessage + '\''
                + '}';
    }
}
