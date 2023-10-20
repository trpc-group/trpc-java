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

package com.tencent.trpc.admin.enums;

/**
 * MetricsTypeEnum Report Type Enumeration Class
 */
public enum MetricsTypeEnum {

    ZHIYAN("zhiyan"),
    M007("m007"),
    TPS_TELEMETRY("tpstelemetry");

    private final String name;

    MetricsTypeEnum(String name) {
        this.name = name;
    }

    /**
     * Is it tpstelemetry
     *
     * @param name metric name
     * @return tpstelemetry true，or false
     */
    public static boolean isTpsTelemetry(String name) {
        return TPS_TELEMETRY.getName().equals(name);
    }

    public String getName() {
        return name;
    }
}
