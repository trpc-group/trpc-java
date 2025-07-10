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

package com.tencent.trpc.container.config.yaml;

import com.tencent.trpc.core.common.config.GlobalConfig;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.utils.BinderUtils;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;

/**
 * Global configuration parser.
 */
public class GlobalConfigParser {

    /**
     * Parse global configuration.
     *
     * @param config global configuration
     * @return parsed GlobalConfig
     */
    public static GlobalConfig parseGlobalConfig(Map<String, Object> config) {
        GlobalConfig globalConfig = new GlobalConfig();
        if (MapUtils.isEmpty(config)) {
            return globalConfig;
        }
        BinderUtils.bind(globalConfig, config);
        BinderUtils.bind(BinderUtils.UNDERSCORES_TO_UPPERCASE, globalConfig, config,
                ConfigConstants.ENABLE_SET, o -> "Y".equalsIgnoreCase(String.valueOf(o)));
        return globalConfig;
    }

}
