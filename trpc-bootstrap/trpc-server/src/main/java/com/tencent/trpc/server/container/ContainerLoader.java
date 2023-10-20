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

import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.TRpcSystemProperties;
import com.tencent.trpc.core.container.spi.Container;
import com.tencent.trpc.core.extension.ExtensionLoader;

/**
 * TRPC container loader
 */
public class ContainerLoader {

    public static Container getContainer() {
        String name = TRpcSystemProperties.getProperties(TRpcSystemProperties.CONTAINER_TYPE,
                Constants.DEFAULT_CONTAINER);
        return ExtensionLoader.getExtensionLoader(Container.class).getExtension(name);
    }

}
