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

import java.util.concurrent.ConcurrentHashMap;

/**
 * Sentinel flow control rule data source configuration factory manager. Used for registering and obtaining flow
 * control rule data source configuration factories.
 */
public class DatasourceConfigFactoryManger {

    /**
     * Used for local caching of flow control rule data source configuration factory classes.
     */
    private static final ConcurrentHashMap<String, DatasourceConfigFactory> FACTORY_MAP = new ConcurrentHashMap<>();

    static {
        FACTORY_MAP.put(DatasourceType.LOCAL_FILE.getName(), new LocalFileDatasourceConfigFactory());
        FACTORY_MAP.put(DatasourceType.NACOS.getName(), new NacosDatasourceConfigFactory());
        FACTORY_MAP.put(DatasourceType.REDIS.getName(), new RedisDatasourceConfigFactory());
        FACTORY_MAP.put(DatasourceType.ZOOKEEPER.getName(), new ZookeeperDatasourceConfigFactory());
    }

    /**
     * Get the sentinel flow control rule data source configuration factory.
     *
     * @param name flow control rule data source name
     * @return flow control rule data source configuration factory
     */
    public static DatasourceConfigFactory getDatasourceConfigFactory(String name) {
        return FACTORY_MAP.get(name);
    }

}
