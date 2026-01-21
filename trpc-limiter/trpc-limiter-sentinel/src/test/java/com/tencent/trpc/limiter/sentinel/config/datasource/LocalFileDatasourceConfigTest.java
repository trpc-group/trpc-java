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

package com.tencent.trpc.limiter.sentinel.config.datasource;

import com.tencent.trpc.core.exception.LimiterDataSourceException;
import com.tencent.trpc.limiter.sentinel.config.datasource.factory.DatasourceConfigFactoryManger;
import com.tencent.trpc.limiter.sentinel.config.datasource.factory.DatasourceType;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * LocalFileDatasourceConfig test class
 */
public class LocalFileDatasourceConfigTest {

    @Test
    public void testRegister() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("path", "classpath:flow_rule.json");
        LocalFileDatasourceConfig localFileDatasourceConfig = (LocalFileDatasourceConfig) DatasourceConfigFactoryManger
                .getDatasourceConfigFactory(DatasourceType.LOCAL_FILE.getName()).create(configMap);
        localFileDatasourceConfig.register();
    }

    @Test
    public void testRegister1() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("path", "flow_rule1.json");
        LocalFileDatasourceConfig localFileDatasourceConfig = (LocalFileDatasourceConfig) DatasourceConfigFactoryManger
                .getDatasourceConfigFactory(DatasourceType.LOCAL_FILE.getName()).create(configMap);
        try {
            localFileDatasourceConfig.register();
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.assertTrue(e instanceof LimiterDataSourceException);
        }
    }

    @Test
    public void testRegister2() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("path", "classpath:flow_rule1.json");
        LocalFileDatasourceConfig localFileDatasourceConfig = (LocalFileDatasourceConfig) DatasourceConfigFactoryManger
                .getDatasourceConfigFactory(DatasourceType.LOCAL_FILE.getName()).create(configMap);
        try {
            localFileDatasourceConfig.register();
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.assertTrue(e instanceof LimiterDataSourceException);
        }
    }

    @Test
    public void testValidate() {
        Map<String, Object> configMap = new HashMap<>();
        LocalFileDatasourceConfig localFileDatasourceConfig = (LocalFileDatasourceConfig) DatasourceConfigFactoryManger
                .getDatasourceConfigFactory(DatasourceType.LOCAL_FILE.getName()).create(configMap);
        try {
            localFileDatasourceConfig.validate();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof LimiterDataSourceException);
        }
    }

}
