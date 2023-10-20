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

package com.tencent.trpc.core.registry.spi;


import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.registry.RegisterInfo;

/**
 * Common interface for service registration
 * Specific registration logic can be implemented based on this interface, such as implementing registration based on
 * the Polaris registration center.
 */

@Extensible
public interface Registry {

    /**
     * Register service
     *
     * @param registerInfo Service registration instance
     */
    void register(RegisterInfo registerInfo);

    /**
     * Unregister service
     *
     * @param registerInfo Service registration instance
     */
    void unregister(RegisterInfo registerInfo);

}
