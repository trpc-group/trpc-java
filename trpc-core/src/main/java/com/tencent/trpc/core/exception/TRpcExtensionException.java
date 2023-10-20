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

public class TRpcExtensionException extends RuntimeException {

    private static final long serialVersionUID = 1281174650572690549L;

    public TRpcExtensionException() {
    }

    public TRpcExtensionException(String message) {
        super(message);
    }

    public TRpcExtensionException(String message, Throwable cause) {
        super(message, cause);
    }

    public TRpcExtensionException(Throwable cause) {
        super(cause);
    }

}
