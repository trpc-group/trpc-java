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

/**
 * Shutdown listener for components that need to perform cleanup when the container stops.
 * This provides a decoupled way for components to register shutdown hooks without
 * creating circular dependencies.
 */
public interface ShutdownListener {
    
    /**
     * Called when the container is shutting down.
     */
    void onShutdown();

}