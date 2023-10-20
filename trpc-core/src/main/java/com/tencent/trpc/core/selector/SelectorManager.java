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

package com.tencent.trpc.core.selector;

import com.tencent.trpc.core.extension.ExtensionManager;
import com.tencent.trpc.core.selector.spi.Selector;

public class SelectorManager {

    private static ExtensionManager<Selector> manager = new ExtensionManager<>(Selector.class);

    public static final ExtensionManager<Selector> getManager() {
        return manager;
    }
}
