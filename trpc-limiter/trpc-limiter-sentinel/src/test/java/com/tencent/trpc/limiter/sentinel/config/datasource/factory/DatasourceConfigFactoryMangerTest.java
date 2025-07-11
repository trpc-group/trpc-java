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

package com.tencent.trpc.limiter.sentinel.config.datasource.factory;

import org.junit.Assert;
import org.junit.Test;

/**
 * DatasourceConfigFactoryManger test class
 */
public class DatasourceConfigFactoryMangerTest {

    @Test
    public void testGetDatasourceConfigFactory() {
        Assert.assertTrue(DatasourceConfigFactoryManger.getDatasourceConfigFactory(
                DatasourceType.LOCAL_FILE.getName()) instanceof LocalFileDatasourceConfigFactory);

        Assert.assertTrue(DatasourceConfigFactoryManger.getDatasourceConfigFactory(
                DatasourceType.REDIS.getName()) instanceof RedisDatasourceConfigFactory);

        Assert.assertTrue(DatasourceConfigFactoryManger.getDatasourceConfigFactory(
                DatasourceType.NACOS.getName()) instanceof NacosDatasourceConfigFactory);

        Assert.assertTrue(DatasourceConfigFactoryManger.getDatasourceConfigFactory(
                DatasourceType.ZOOKEEPER.getName()) instanceof ZookeeperDatasourceConfigFactory);
    }

}
