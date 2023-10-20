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

import java.util.Objects;

public class TRpcException extends RuntimeException {

    private static final long serialVersionUID = -8244834705694262849L;
    private int code;
    private int bizCode;

    public TRpcException() {
    }

    /**
     * TRpcException Constructor
     *
     * @param code the error code, see @ErrorCode class for definitions.
     * @param bizCode the business error code
     * @param msg the error message
     */
    private TRpcException(int code, int bizCode, String msg) {
        super(msg);
        this.code = code;
        this.bizCode = bizCode;
    }

    private TRpcException(int code, int bizCode, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.bizCode = bizCode;
    }

    public static TRpcException newException(int code, int bizCode, String message,
            Throwable cause) {
        return new TRpcException(code, bizCode, message, cause);
    }

    public static TRpcException newException(int code, int bizCode, String message) {
        return new TRpcException(code, bizCode, message);
    }

    public static TRpcException newException(int code, int bizCode, String format, Object... args) {
        return new TRpcException(code, bizCode, String.format(format, args));
    }

    public static TRpcException newBizException(int bizCode, String message, Throwable cause) {
        return new TRpcException(0, bizCode, message, cause);
    }

    public static TRpcException newBizException(int bizCode, String message) {
        return new TRpcException(0, bizCode, message);
    }

    public static TRpcException newBizException(int bizCode, String format, Object... args) {
        return newException(0, bizCode, format, args);
    }

    public static TRpcException newFrameException(int code, String message, Throwable cause) {
        return new TRpcException(code, 0, message, cause);
    }

    public static TRpcException newFrameException(int code, String message) {
        return new TRpcException(code, 0, message);
    }

    public static TRpcException newFrameException(int code, String format, Object... args) {
        return newException(code, 0, format, args);
    }

    public static void checkArgument(boolean express, int errorCode, int bizCode, String format,
            Object... args) {
        if (!express) {
            throw new TRpcException(errorCode, bizCode, String.format(format, args));
        }
    }

    public static void checkBizArgument(boolean express, int bizCode, String format,
            Object... args) {
        if (!express) {
            throw new TRpcException(0, bizCode, String.format(format, args));
        }
    }

    public static void checkFrameArgument(boolean express, int code, int bizCode, String format,
            Object... args) {
        if (!express) {
            throw new TRpcException(code, 0, String.format(format, args));
        }
    }

    public static TRpcException trans(Throwable cause) {
        Objects.requireNonNull(cause, "cause");
        if (cause instanceof TRpcException) {
            return (TRpcException) cause;
        } else {
            return TRpcException.newFrameException(ErrorCode.TRPC_INVOKE_UNKNOWN_ERR, cause.getMessage(), cause);
        }
    }

    public boolean isFrameException() {
        return code != 0;
    }

    public boolean isBizException() {
        return bizCode != 0;
    }

    public int getBizCode() {
        return bizCode;
    }

    public void setBizCode(int bizCode) {
        this.bizCode = bizCode;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

}
