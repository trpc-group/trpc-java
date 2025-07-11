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

package com.tencent.trpc.registry.center;

import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.core.registry.spi.Registry;

/**
 * Registry center interface, fully abstracts the core methods of the registry center.
 * Inherits from the {@link Registry} interface, which only implements registration and unregistration functions.
 */
public interface RegistryCenter extends Registry {

    /**
     * Consumer subscription interface.
     *
     * @param registerInfo The registry information.
     * @param notifyListener The listener.
     */
    void subscribe(RegisterInfo registerInfo, NotifyListener notifyListener);

    /**
     * Consumer unsubscribe interface.
     *
     * @param registerInfo The registry information.
     * @param notifyListener The listener.
     */
    void unsubscribe(RegisterInfo registerInfo, NotifyListener notifyListener);

    /**
     * Whether the registered service is valid.
     *
     * @return true or false.
     */
    default boolean isAvailable() {
        return false;
    }

}
