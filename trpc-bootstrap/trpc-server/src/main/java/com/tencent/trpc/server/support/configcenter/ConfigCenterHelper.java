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

package com.tencent.trpc.server.support.configcenter;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.configcenter.ConfigurationManager;
import com.tencent.trpc.core.configcenter.spi.ConfigurationLoader;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Configuration center loader class, soon we will move this class into trpc-core/configcenter
 */
@Deprecated
public final class ConfigCenterHelper {

    private static final ConfigCenterHelper INSTANCE = new ConfigCenterHelper();

    private final AtomicReference<ConfigurationLoader> loaderAtomicReference =
            new AtomicReference<>();
    private final Object lock = new Object();

    private ConfigCenterHelper() {
    }

    public static ConfigCenterHelper getInstance() {
        return INSTANCE;
    }

    /**
     * Obtain the specific configuration center implementation through the plugin name
     */
    public ConfigurationLoader getConfigurationLoader() {
        ConfigurationLoader configurationLoader = loaderAtomicReference.get();
        if (configurationLoader == null) {
            synchronized (lock) {
                if (configurationLoader == null) {
                    String configCenter = ConfigManager.getInstance().getServerConfig()
                            .getConfigCenter();
                    loaderAtomicReference
                            .set(ConfigurationManager.getConfigurationLoader(configCenter));
                    configurationLoader = loaderAtomicReference.get();
                }
            }
        }
        return configurationLoader;
    }

}
