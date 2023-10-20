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

package com.tencent.trpc.server.container;

import com.tencent.trpc.core.container.spi.Container;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;

/**
 * TRPC container startup class
 */
public class TRPC {

    private static final Logger logger = LoggerFactory.getLogger(TRPC.class);

    /**
     * Start the tRPC container
     */
    public static void start() {
        Container container = ContainerLoader.getContainer();
        try {
            logger.debug("TRpc Container starting!");
            container.start();
            logger.debug("TRpc Container started!");
        } catch (Exception e) {
            logger.error("TRpc Container start exception, ", e);
            System.exit(1);
        }
    }

}
