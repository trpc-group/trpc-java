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

package com.tencent.trpc.springmvc;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.util.Map;
import java.util.Map.Entry;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;

/**
 * Config SpringMVC server port by the trpc configuration.
 */
public class TRpcWebServerFactoryCustomizer implements WebServerFactoryCustomizer<ConfigurableWebServerFactory>,
        Ordered {

    private static final Logger logger = LoggerFactory.getLogger(TRpcWebServerFactoryCustomizer.class);

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        Map<String, ServiceConfig> serviceMap =
                ConfigManager.getInstance().getServerConfig().getServiceMap();

        int port = 0;
        for (Entry<String, ServiceConfig> serviceConfigEntry : serviceMap.entrySet()) {
            ServiceConfig serviceConfig = serviceConfigEntry.getValue();
            if (!serviceConfig.getProtocol().equals("rest")) {
                continue;
            }

            if (serviceConfig.getPort() > 0) {
                if (port == 0) {
                    port = serviceConfig.getPort();
                } else {
                    logger.warn("trpc springmvc can only using the first configured port");
                }
            }
        }

        if (port > 0) {
            logger.info("set springmvc webserver port to {}", port);
            factory.setPort(port);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
