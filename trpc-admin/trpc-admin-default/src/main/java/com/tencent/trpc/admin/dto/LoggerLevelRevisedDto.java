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

package com.tencent.trpc.admin.dto;

/**
 * Log level modification view class
 */
public class LoggerLevelRevisedDto extends CommonDto {

    /**
     * Current log level
     */
    private String level;
    /**
     * Previous log level
     */
    private String prelevel;

    public LoggerLevelRevisedDto() {
    }

    public LoggerLevelRevisedDto(String level, String prelevel) {
        this.level = level;
        this.prelevel = prelevel;
    }

    public LoggerLevelRevisedDto(String errorcode, String message, String level,
            String prelevel) {
        super(errorcode, message);
        this.level = level;
        this.prelevel = prelevel;
    }

    public static LoggerLevelRevisedDto buildFail(String message) {
        return new LoggerLevelRevisedDto(CommonDto.FAIL, message, null, null);
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getPrelevel() {
        return prelevel;
    }

    public void setPrelevel(String prelevel) {
        this.prelevel = prelevel;
    }

    @Override
    public String toString() {
        return "LoggerLevelRevisedDto{" + "level='" + level + '\'' + ", prelevel='" + prelevel
                + '\'' + "} " + super.toString();
    }
}
