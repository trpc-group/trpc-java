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
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Initializer for tRPC {@link ConfigManager}
 *
 * @see TRpcConfigManagerCustomizer
 */
public class TRpcConfigManagerInitializer implements InitializingBean {

    private final ObjectProvider<TRpcConfigManagerCustomizer> customizerProvider;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Construct {@link TRpcConfigManagerInitializer}.
     *
     * @param customizerProvider {@link ObjectProvider} of {@link TRpcConfigManagerCustomizer}s
     */
    public TRpcConfigManagerInitializer(ObjectProvider<TRpcConfigManagerCustomizer> customizerProvider) {
        this.customizerProvider = customizerProvider;
    }

    @Override
    public void afterPropertiesSet() {
        initialize();
    }

    public void initialize() {
        initialize(ConfigManager.getInstance());
    }

    private void initialize(ConfigManager configManager) {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        customize(configManager);
        configManager.setDefault();
    }

    private void customize(ConfigManager configManager) {
        customizerProvider.orderedStream().forEach(customizer -> customizer.customize(configManager));
    }
}
