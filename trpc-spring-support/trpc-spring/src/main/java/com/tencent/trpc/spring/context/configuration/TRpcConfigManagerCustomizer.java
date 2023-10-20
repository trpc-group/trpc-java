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

package com.tencent.trpc.spring.context.configuration;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.Lifecycle.LifecycleState;
import org.springframework.core.Ordered;

/**
 * Interface that allow user to customize tRPC Configurations({@link ConfigManager})
 * upon spring context initialization.
 *
 * @apiNote {@link ConfigManager} will be in NEW state when {@link #customize} is triggering.
 * @see ConfigManager
 * @see LifecycleState
 */
@FunctionalInterface
public interface TRpcConfigManagerCustomizer extends Ordered {

    /**
     * Priority of this customizer bean. Defaults to {@link Ordered#LOWEST_PRECEDENCE}
     *
     * @return Customizer execution priority
     */
    @Override
    default int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * Customize the {@link ConfigManager}
     *
     * @param configManager tRPC {@link ConfigManager}, which {@link LifecycleState} is NEW
     *         and {@link ConfigManager#setDefault()} is false.
     */
    void customize(ConfigManager configManager);
}
