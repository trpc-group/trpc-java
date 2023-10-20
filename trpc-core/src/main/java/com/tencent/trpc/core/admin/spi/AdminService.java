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

package com.tencent.trpc.core.admin.spi;

import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.extension.Extensible;

/**
 * Admin service can customize the management capabilities provided to the outside world using the SPI plugin.
 * Suggestion: To avoid security risks, do not provide write capabilities.
 * {@see com.tencent.trpc.admin.service.DefaultAdminServiceImpl}
 */
@Extensible("defaultAdminService")
public interface AdminService {

    void setServerConfig(ServerConfig serverConfig);

    void init();

    void start();

    void stop();

}
