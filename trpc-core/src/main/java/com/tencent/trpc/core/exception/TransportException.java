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

public class TransportException extends RuntimeException {

    private static final long serialVersionUID = -6406275952373254776L;

    public TransportException(String message) {
        super(message);
    }

    public TransportException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransportException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

    public static TransportException create(String format, Object... args) {
        return create(null, format, args);
    }

    public static TransportException create(Throwable cause, String format, Object... args) {
        if (cause != null) {
            return new TransportException(String.format(format, args), cause);
        } else {
            return new TransportException(String.format(format, args));
        }
    }

    public static TransportException trans(Throwable cause, String msgIfNotTransportException) {
        if (cause instanceof TransportException) {
            return (TransportException) cause;
        } else {
            return new TransportException(msgIfNotTransportException, cause);
        }
    }

    public static TransportException trans(Throwable cause) {
        if (cause instanceof TransportException) {
            return (TransportException) cause;
        } else {
            return new TransportException(cause);
        }
    }

}
