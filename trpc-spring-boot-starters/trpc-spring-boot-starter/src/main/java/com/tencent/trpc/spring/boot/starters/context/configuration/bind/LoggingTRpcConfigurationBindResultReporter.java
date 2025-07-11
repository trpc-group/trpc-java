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

package com.tencent.trpc.spring.boot.starters.context.configuration.bind;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;

/**
 * Outputs {@link BindResultDescription} in the logs
 */
public class LoggingTRpcConfigurationBindResultReporter implements TRpcConfigurationBindResultReporter {

    private static final Logger logger = LoggerFactory.getLogger(TRpcConfigurationBindResultReporter.class);

    @Override
    public void report(BindResultDescription description) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        appendBanner(sb);
        sb.append(description.toString());
        logger.debug(sb.toString());
    }

    private void appendBanner(StringBuilder sb) {
        sb.append("\n\n");
        sb.append("*************************\n");
        sb.append("TRPC CONFIGURATION REPORT\n");
        sb.append("*************************\n");
        sb.append("\n");
    }
}