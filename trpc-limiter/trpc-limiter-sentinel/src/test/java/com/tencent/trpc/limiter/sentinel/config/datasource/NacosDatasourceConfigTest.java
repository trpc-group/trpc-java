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

package com.tencent.trpc.limiter.sentinel.config.datasource;

import com.tencent.trpc.core.exception.LimiterDataSourceException;
import com.tencent.trpc.limiter.sentinel.config.datasource.factory.DatasourceConfigFactoryManger;
import com.tencent.trpc.limiter.sentinel.config.datasource.factory.DatasourceType;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * NacosDatasourceConfig test class
 */
public class NacosDatasourceConfigTest {

    @Test
    public void testValidate1() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("remote_address", "localhost");
        configMap.put("group_id", "123");
        NacosDatasourceConfig nacosDatasourceConfig = (NacosDatasourceConfig) DatasourceConfigFactoryManger
                .getDatasourceConfigFactory(DatasourceType.NACOS.getName()).create(configMap);
        try {
            nacosDatasourceConfig.validate();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(e instanceof LimiterDataSourceException);
        }
    }

    @Test
    public void testValidate2() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("remote_address", "localhost");
        configMap.put("data_id", "123");
        NacosDatasourceConfig localFileDatasourceConfig = (NacosDatasourceConfig) DatasourceConfigFactoryManger
                .getDatasourceConfigFactory(DatasourceType.NACOS.getName()).create(configMap);
        try {
            localFileDatasourceConfig.register();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(e instanceof LimiterDataSourceException);
        }
    }

    @Test
    public void testValidate3() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("group_id", "123");
        configMap.put("data_id", "123");
        NacosDatasourceConfig localFileDatasourceConfig = (NacosDatasourceConfig) DatasourceConfigFactoryManger
                .getDatasourceConfigFactory(DatasourceType.NACOS.getName()).create(configMap);
        try {
            localFileDatasourceConfig.register();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(e instanceof LimiterDataSourceException);
        }
    }

    @Test
    public void testRegister() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("remote_address", "localhost");
        configMap.put("group_id", "123");
        configMap.put("data_id", "123");
        NacosDatasourceConfig localFileDatasourceConfig = (NacosDatasourceConfig) DatasourceConfigFactoryManger
                .getDatasourceConfigFactory(DatasourceType.NACOS.getName()).create(configMap);
        try {
            localFileDatasourceConfig.register();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(e instanceof LimiterDataSourceException);
        }
    }

}
