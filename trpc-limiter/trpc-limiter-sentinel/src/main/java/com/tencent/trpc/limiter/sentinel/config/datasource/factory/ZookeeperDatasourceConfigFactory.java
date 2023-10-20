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

import com.tencent.trpc.limiter.sentinel.config.datasource.DatasourceConfig;
import com.tencent.trpc.limiter.sentinel.config.datasource.ZookeeperDatasourceConfig;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;

/**
 * Zookeeper data source configuration factory.
 */
public class ZookeeperDatasourceConfigFactory implements DatasourceConfigFactory {

    /**
     * Zookeeper address configuration item.
     */
    private static final String REMOTE_ADDRESS = "remote_address";
    /**
     * Zookeeper path configuration item for flow control rules.
     */
    private static final String FLOW_RULE_PATH = "path";

    @Override
    public String name() {
        return DatasourceType.ZOOKEEPER.getName();
    }

    @Override
    public DatasourceConfig create(Map<String, Object> configs) {
        String zkRemoteAddress = MapUtils.getString(configs, REMOTE_ADDRESS);
        String path = MapUtils.getString(configs, FLOW_RULE_PATH);

        ZookeeperDatasourceConfig zookeeperDataSourceConfig = new ZookeeperDatasourceConfig();
        zookeeperDataSourceConfig.setRemoteAddress(zkRemoteAddress);
        zookeeperDataSourceConfig.setPath(path);
        return zookeeperDataSourceConfig;
    }

}
