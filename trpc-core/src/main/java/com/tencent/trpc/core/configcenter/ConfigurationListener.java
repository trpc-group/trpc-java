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

package com.tencent.trpc.core.configcenter;

import com.tencent.trpc.core.exception.ConfigCenterException;

/**
 * Configuration change listener. If you need to enable monitoring of changes in
 * the remote configuration center, you can register an instance of {@code ConfigurationListener} through
 * the {@link com.tencent.trpc.core.configcenter.spi.ConfigurationLoader#addListener} method.
 *
 * @see com.tencent.trpc.core.configcenter.ConfigurationEvent
 */
public interface ConfigurationListener {

    /**
     * Reload remote config change.
     *
     * @param event config change event
     * @throws ConfigCenterException ConfigCenterException
     */
    default void onReload(ConfigurationEvent event) throws ConfigCenterException {
        onChange(event);
    }

    /**
     * Handle remote config change.
     *
     * @param event config change event
     * @throws ConfigCenterException ConfigCenterException
     */
    void onChange(ConfigurationEvent event) throws ConfigCenterException;
    
}