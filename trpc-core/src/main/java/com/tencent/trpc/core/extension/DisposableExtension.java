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

package com.tencent.trpc.core.extension;

import com.tencent.trpc.core.exception.TRpcExtensionException;

/**
 * Plugin destruction interface, timing: when the framework is destroyed,
 * the framework startup only guarantees the destruction operation of singleton plugins.
 * In a multi-instance scenario, after initialization is completed, it is handed over to the program that calls it,
 * and the startup and destruction are both the responsibility of the calling program.
 */
public interface DisposableExtension {

    void destroy() throws TRpcExtensionException;
    
}