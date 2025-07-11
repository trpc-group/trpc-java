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

public class ConfigCenterException extends RuntimeException {

    private static final long serialVersionUID = -9132790440223323702L;

    public ConfigCenterException() {
        super();
    }

    public ConfigCenterException(String message) {
        super(message);
    }

    public ConfigCenterException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigCenterException(Throwable cause) {
        super(cause);
    }

}