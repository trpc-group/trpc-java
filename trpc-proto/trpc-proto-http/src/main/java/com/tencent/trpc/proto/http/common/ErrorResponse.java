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

package com.tencent.trpc.proto.http.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

/**
 * Error message wrapper class.
 *
 */
public class ErrorResponse {

    /**
     * The name of the RPC service being called.
     */
    private String rpcServiceName;

    /**
     * The method of the RPC service being called.
     */
    private String rpcMethodName;

    /**
     * The error occurred during the call.
     */
    private String error;

    /**
     * The detail error message occurred during the call.
     */
    private String message;

    /**
     * The time when the exception occurred.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date timestamp;

    /**
     * The HTTP response status.
     */
    private int status;

    /**
     * Construct a {@link ErrorResponse}
     *
     * @param request the originial servlet request
     * @param status http status
     * @param error invoke error
     * @param errMsg detail invoke error
     * @return ErrorResponse
     */
    public static ErrorResponse create(HttpServletRequest request, int status, String error, String errMsg) {
        String rpcServiceName = (String) Optional.ofNullable(
                request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE)).orElse(StringUtils.EMPTY);
        String rpcMethodName = (String) Optional.ofNullable(
                request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD)).orElse(StringUtils.EMPTY);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setRpcServiceName(rpcServiceName);
        errorResponse.setRpcMethodName(rpcMethodName);
        errorResponse.setError(StringUtils.isNotEmpty(error) ? error : StringUtils.EMPTY);
        errorResponse.setMessage(StringUtils.isNoneEmpty(errMsg) ? errMsg : StringUtils.EMPTY);
        errorResponse.setTimestamp(new Date(System.currentTimeMillis()));
        errorResponse.setStatus(status);
        return errorResponse;
    }

    /**
     * Construct a {@link ErrorResponse}
     *
     * @param request the originial servlet request
     * @param status http status
     * @param throwable invoke exception
     * @param errMsg detail invoke error
     * @return ErrorResponse
     */
    public static ErrorResponse create(HttpServletRequest request, int status, Throwable throwable, String errMsg) {
        return create(request, status, throwable.toString(), errMsg);
    }

    /**
     * Construct a {@link ErrorResponse}
     *
     * @param request the originial servlet request
     * @param status http status
     * @param throwable invoke exception
     * @return ErrorResponse
     */
    public static ErrorResponse create(HttpServletRequest request, int status, Throwable throwable) {
        return create(request, status, throwable, throwable.getMessage());
    }

    /**
     * Construct a {@link ErrorResponse}
     *
     * @param request the originial servlet request
     * @param status http status
     * @param errMsg detail invoke error
     * @return ErrorResponse
     */
    public static ErrorResponse create(HttpServletRequest request, int status, String errMsg) {
        return create(request, status, errMsg, errMsg);
    }

    public String getRpcServiceName() {
        return rpcServiceName;
    }

    public void setRpcServiceName(String rpcServiceName) {
        this.rpcServiceName = rpcServiceName;
    }

    public String getRpcMethodName() {
        return rpcMethodName;
    }

    public void setRpcMethodName(String rpcMethodName) {
        this.rpcMethodName = rpcMethodName;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
