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

package com.tencent.trpc.core.rpc;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;

/**
 * Provides a wrapper for the interface implementation class.
 */
public interface ProviderInvoker<T> extends Invoker<T> {

    /**
     * Get the specific interface implementation.
     */
    T getImpl();

    /**
     * Get the service configuration related to the service provider.
     */
    ProviderConfig<T> getConfig();

    /**
     * Get the protocol configuration information.
     */
    ProtocolConfig getProtocolConfig();

}
