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

package com.tencent.trpc.core.admin;

import com.tencent.trpc.core.admin.spi.AdminService;
import com.tencent.trpc.core.extension.ExtensionManager;

/**
 * Admin service manager.
 * The AdminService is created only once using a convention-based approach.
 */
public class AdminServiceManager {

    private static final ExtensionManager<AdminService> manager = new ExtensionManager<>(
            AdminService.class);

    public static ExtensionManager<AdminService> getManager() {
        return manager;
    }

}
