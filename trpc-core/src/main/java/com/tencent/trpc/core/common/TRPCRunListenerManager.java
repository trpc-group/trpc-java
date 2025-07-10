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

package com.tencent.trpc.core.common;

import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.utils.PreconditionUtils;

/**
 * TRPCRunListener plugin manager.
 */
public class TRPCRunListenerManager {

    /**
     * TRPCRunListener SPI loader.
     */
    private static final ExtensionLoader<TRPCRunListener> runListener = ExtensionLoader
            .getExtensionLoader(TRPCRunListener.class);

    /**
     * Check if the specified TRPCRunListener plugin name has a corresponding TRPCRunListener plugin.
     *
     * @param name TRPCRunListener plugin name
     */
    public static void checkExist(String name) {
        PreconditionUtils.checkArgument(hasExtension(name),
                "the name[%s] has not found tRPCRunListener extension", name);
    }

    /**
     * Whether there is a specified TRPCRunListener plugin name.
     *
     * @param name TRPCRunListener plugin name
     * @return true if it exists, false otherwise
     */
    public static boolean hasExtension(String name) {
        return runListener.hasExtension(name);
    }

    /**
     * Get the specified TRPCRunListener plugin by name.
     *
     * @param name TRPCRunListener plugin name
     * @return the plugin if it exists, otherwise throw an IllegalArgumentException
     */
    public static TRPCRunListener getExtension(String name) {
        checkExist(name);
        return runListener.getExtension(name);
    }

}
