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
import java.util.Map;

/**
 * Sentinel flow control rule data source configuration factory interface. All sentinel data source configuration
 * factory classes should implement this interface.
 */
public interface DatasourceConfigFactory {

    /**
     * Flow control rule data source configuration factory name, consistent with the data source name.
     */
    String name();

    /**
     * Create a flow control rule data source configuration factory.
     *
     * @param configs data source configuration
     * @return flow control rule data source configuration entity
     */
    DatasourceConfig create(Map<String, Object> configs);

}
