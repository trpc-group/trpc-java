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

package com.tencent.trpc.core.selector.loadbalance;

import com.tencent.trpc.core.extension.ExtensionManager;
import com.tencent.trpc.core.selector.spi.LoadBalance;

public class LoadBalanceManager {

    private static ExtensionManager<LoadBalance> manager = new ExtensionManager<>(LoadBalance.class);

    public static final ExtensionManager<LoadBalance> getManager() {
        return manager;
    }
}
