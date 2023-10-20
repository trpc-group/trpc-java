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

import com.tencent.trpc.core.container.spi.ServerListener;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;

public class DefaultServerListener implements ServerListener {

    private static final Logger logger = LoggerFactory.getLogger(DefaultServerListener.class);

    @Override
    public void onServerStarted() {
        logger.info("application started!");
    }

    @Override
    public void onServerStopped() {
        logger.info("application stopped!");
    }

}
