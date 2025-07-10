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

import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.rpc.Invoker;

/**
 * The {@link ClusterInvoker} extends {@link Invoker} ,Provides client cluster call capability.
 * <p>That is, multiple client interfaces correspond to one: {@link BackendConfig#getNamingUrl()} (String)}</p>
 * <p>See the specific implementation in {@link AbstractClusterInvoker}</p>
 */
public interface ClusterInvoker<T> extends Invoker<T> {

    Class<T> getInterface();

    ConsumerConfig<T> getConfig();

    BackendConfig getBackendConfig();

}