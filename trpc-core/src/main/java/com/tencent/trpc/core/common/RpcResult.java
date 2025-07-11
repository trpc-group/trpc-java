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

package com.tencent.trpc.core.common;

import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;

/**
 * RPC call common return result.
 * Includes return value, framework status code, business status code, and exception information.
 * Usage:
 * <pre>
 * public interface Greeter {
 *     {@code RpcResult<HelloReply> hello(Hello hello);}
 * }
 * </pre>
 *
 * It is recommended to put it in the RPC package.
 */
public class RpcResult<T> {

    /**
     * Status code.
     */
    private int code;
    /**
     * Business code.
     */
    private int bizCode;
    /**
     * Error message.
     */
    private String message;
    /**
     * Data body.
     */
    private T data;

    public static RpcResult<Object> fail(TRpcException tRpcException) {
        RpcResult<Object> result = new RpcResult<>();
        result.setCode(tRpcException.getCode());
        result.setBizCode(tRpcException.getBizCode());
        result.setMessage(tRpcException.getMessage());
        return result;
    }

    public static RpcResult<Object> success() {
        RpcResult<Object> result = new RpcResult<>();
        result.setCode(ErrorCode.TRPC_INVOKE_SUCCESS);
        result.setBizCode(ErrorCode.TRPC_INVOKE_SUCCESS);
        return result;
    }

    public static RpcResult<Object> success(Object data) {
        RpcResult<Object> result = new RpcResult<>();
        result.setData(data);
        result.setCode(ErrorCode.TRPC_INVOKE_SUCCESS);
        result.setBizCode(ErrorCode.TRPC_INVOKE_SUCCESS);
        return result;
    }

    /**
     * Whether the invocation result is successful. The request is considered successful only when both the framework
     * status code and the business status code are successful.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return ErrorCode.TRPC_INVOKE_SUCCESS == code && ErrorCode.TRPC_INVOKE_SUCCESS == bizCode;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getBizCode() {
        return bizCode;
    }

    public void setBizCode(int bizCode) {
        this.bizCode = bizCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

}
