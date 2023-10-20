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

package com.tencent.trpc.server.main;

import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.TRpcSystemProperties;
import com.tencent.trpc.server.container.TRPC;
import org.apache.commons.lang3.StringUtils;

/**
 * Another startup class that supports configuring the container to start loading yaml files through the args parameter.
 */
public class Main {

    public static void main(String[] args) {
        if (args != null && args.length >= 1) {
            for (String arg : args) {
                String[] cs = arg.split("=");
                if (cs.length == 2) {
                    // The yaml file to start loading can be configured through the "config_path" parameter
                    if (StringUtils.equalsIgnoreCase(cs[0], Constants.OLD_CONFIG_PATH)) {
                        System.setProperty(TRpcSystemProperties.CONFIG_PATH, cs[1]);
                    }
                }
            }
        }
        TRPC.start();
    }

}
