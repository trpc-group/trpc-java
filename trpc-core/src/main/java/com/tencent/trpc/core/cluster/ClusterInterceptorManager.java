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

package com.tencent.trpc.core.cluster;

import com.google.common.base.Preconditions;
import com.tencent.trpc.core.cluster.spi.ClusterInterceptor;
import com.tencent.trpc.core.extension.ExtensionLoader;

public class ClusterInterceptorManager {

    public static ClusterInterceptor get(String name) {
        validate(name);
        return ExtensionLoader.getExtensionLoader(ClusterInterceptor.class).getExtension(name);
    }

    public static void validate(String name) {
        Preconditions.checkArgument(ExtensionLoader.getExtensionLoader(ClusterInterceptor.class).hasExtension(name),
                "Not found cluster invoker interceptor (name=" + name + ")");
    }

}
