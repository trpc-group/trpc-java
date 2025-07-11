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

import com.tencent.trpc.limiter.sentinel.config.datasource.DatasourceConfig;
import com.tencent.trpc.limiter.sentinel.config.datasource.LocalFileDatasourceConfig;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;

/**
 * Local file data source configuration factory.
 */
public class LocalFileDatasourceConfigFactory implements DatasourceConfigFactory {

    /**
     * Local file path configuration item.
     */
    private static final String FILE_PATH = "path";

    @Override
    public String name() {
        return DatasourceType.LOCAL_FILE.getName();
    }

    @Override
    public DatasourceConfig create(Map<String, Object> configs) {
        String filePath = MapUtils.getString(configs, FILE_PATH);
        LocalFileDatasourceConfig localFileDataSourceConfig = new LocalFileDatasourceConfig();
        localFileDataSourceConfig.setPath(filePath);
        return localFileDataSourceConfig;
    }

}
