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
import com.tencent.trpc.limiter.sentinel.config.datasource.NacosDatasourceConfig;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;

/**
 * Nacos data source configuration factory.
 */
public class NacosDatasourceConfigFactory implements DatasourceConfigFactory {

    /**
     * Nacos groupId configuration item.
     */
    public static final String GROUP_ID = "group_id";
    /**
     * Nacos dataId configuration item.
     */
    public static final String DATA_ID = "data_id";
    /**
     * Nacos address configuration item.
     */
    public static final String REMOTE_ADDRESS = "remote_address";

    @Override
    public String name() {
        return DatasourceType.NACOS.getName();
    }

    @Override
    public DatasourceConfig create(Map<String, Object> configs) {
        String remoteAddress = MapUtils.getString(configs, REMOTE_ADDRESS);
        String groupId = MapUtils.getString(configs, GROUP_ID);
        String dataId = MapUtils.getString(configs, DATA_ID);

        NacosDatasourceConfig nacosDataSourceConfig = new NacosDatasourceConfig();
        nacosDataSourceConfig.setRemoteAddress(remoteAddress);
        nacosDataSourceConfig.setGroupId(groupId);
        nacosDataSourceConfig.setDataId(dataId);
        return nacosDataSourceConfig;
    }
    
}
