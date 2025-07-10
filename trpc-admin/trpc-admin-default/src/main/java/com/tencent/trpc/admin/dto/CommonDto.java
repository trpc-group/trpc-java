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

package com.tencent.trpc.admin.dto;

/**
 * Generic view class
 */
public class CommonDto {

    /**
     * Success status code
     */
    public static final String SUCCESS = "0";
    /**
     * Failure status code
     */
    public static final String FAIL = "-1";
    /**
     * Error code
     */
    private String errorcode = CommonDto.SUCCESS;
    /**
     * Error message
     */
    private String message = "";

    public CommonDto() {
    }

    public CommonDto(String errorcode, String message) {
        this.errorcode = errorcode;
        this.message = message;
    }

    public String getErrorcode() {
        return errorcode;
    }

    public void setErrorcode(String errorcode) {
        this.errorcode = errorcode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "CommonDto{" + "errorcode='" + errorcode + '\'' + ", message='" + message + '\''
                + '}';
    }
}
