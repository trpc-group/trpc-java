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

package com.tencent.trpc.spring.boot.starters.listener;

import com.tencent.trpc.core.common.Version;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;

/**
 * Listens to Spring context startup events and prints tRpc logo and version information.
 */
@Order
public class TRpcBannerListener implements ApplicationListener<ApplicationContextInitializedEvent> {

    private static final AtomicBoolean PROCESSED = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ApplicationContextInitializedEvent event) {
        if (PROCESSED.compareAndSet(false, true)) {
            String bannerText = buildBannerText();
            System.out.print(bannerText);
        }
    }

    private String buildBannerText() {
        return "\n"
                + "  _______   _____    _____     _____   \n"
                + " |__   __| |  __ \\  |  __ \\   / ____|  \n"
                + "    | |    | |__) | | |__) | | |       \n"
                + "    | |    |  _  /  |  ___/  | |       \n"
                + "    | |    | | \\ \\  | |      | |____   \n"
                + "    |_|    |_|  \\_\\ |_|       \\_____|  \n"
                + ":: TRPC :: (" + Version.version() + ")\n"
                + "                                       \n";
    }

}