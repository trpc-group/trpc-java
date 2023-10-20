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

package com.tencent.trpc.limiter.sentinel.config.datasource.factory;

/**
 * Sentinel flow control rule data source name enumeration.
 */
public enum DatasourceType {

    ZOOKEEPER("zookeeper"),
    REDIS("redis"),
    NACOS("nacos"),
    LOCAL_FILE("file");

    private String name;

    DatasourceType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
