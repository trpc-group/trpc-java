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
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * ZookeeperDatasourceConfig test class
 */
public class ZookeeperDatasourceConfigTest {

    private ZookeeperDatasourceConfig zookeeperDatasourceConfig;
    private TestingServer zkServer;

    @BeforeEach
    public void setUp() throws Exception {
        int port = 2183;
        zkServer = new TestingServer(port, new File("/tmp/sentinel"));
        zkServer.start();

        Map<String, Object> configMap = new HashMap<>();
        configMap.put("remote_address", "127.0.0.1:" + port);
        configMap.put("path", "/tmp/sentinel");
        zookeeperDatasourceConfig = (ZookeeperDatasourceConfig) DatasourceConfigFactoryManger
                .getDatasourceConfigFactory(DatasourceType.ZOOKEEPER.getName()).create(configMap);
    }

    @Test
    public void testRegister() {
        zookeeperDatasourceConfig.register();
    }

    @Test
    public void testValidate1() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("path", "/tmp/sentinel");
        zookeeperDatasourceConfig = (ZookeeperDatasourceConfig) DatasourceConfigFactoryManger
                .getDatasourceConfigFactory(DatasourceType.ZOOKEEPER.getName()).create(configMap);
        try {
            zookeeperDatasourceConfig.validate();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof LimiterDataSourceException);
            Assertions.assertTrue(
                    "sentinel zookeeper datasource config error, remote address is empty".equals(e.getMessage()));
        }
    }

    @Test
    public void testValidate2() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("remote_address", "127.0.0.1:2181");
        zookeeperDatasourceConfig = (ZookeeperDatasourceConfig) DatasourceConfigFactoryManger
                .getDatasourceConfigFactory(DatasourceType.ZOOKEEPER.getName()).create(configMap);
        try {
            zookeeperDatasourceConfig.validate();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof LimiterDataSourceException);
            Assertions.assertTrue(
                    "sentinel zookeeper datasource config error, path is empty".equals(e.getMessage()));
        }
    }

    @AfterEach
    public void after() throws Exception {
        if (zkServer != null) {
            zkServer.stop();
        }
    }

}
