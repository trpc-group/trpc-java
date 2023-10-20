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

package com.tencent.trpc.container.container;

import com.tencent.trpc.container.config.ApplicationConfigParser;
import com.tencent.trpc.container.config.system.Environment;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.TRpcSystemProperties;
import com.tencent.trpc.core.container.spi.Container;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.extension.ExtensionLoader;

@Extension("default")
public class DefaultContainer implements Container {

    private ConfigManager configManager;

    /**
     * Read configuration, initialize services, and start container.
     */
    @Override
    public void start() {
        String name = TRpcSystemProperties.getProperties(TRpcSystemProperties.CONFIG_TYPE,
                Constants.DEFAULT_CONFIG_TYPE);
        ApplicationConfigParser applicationConfigParser =
                ExtensionLoader.getExtensionLoader(ApplicationConfigParser.class).getExtension(name);
        this.configManager = new Environment(applicationConfigParser).parse();
        configManager.start();
    }

    /**
     * Stop TRPC container.
     */
    @Override
    public void stop() {
        if (configManager != null) {
            configManager.stop();
        }
    }

}
