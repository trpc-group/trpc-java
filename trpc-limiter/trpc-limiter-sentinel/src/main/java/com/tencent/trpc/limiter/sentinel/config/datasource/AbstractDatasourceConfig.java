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

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;

/**
 * Sentinel flow control rule data source configuration abstract class.
 */
public abstract class AbstractDatasourceConfig implements DatasourceConfig {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractDatasourceConfig.class);

    /**
     * Validate data source configuration.
     */
    protected abstract void validate();

    /**
     * Register data source.
     */
    @Override
    public void register() {
        validate();
    }
    
}
