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

package com.tencent.trpc.core.filter;

import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.rpc.ProviderInvoker;

/**
 * Filter chain utility class.
 */
public class FilterChain {

    /**
     * Support "filter" at the method granularity. buildProviderChain.
     *
     * @param providerConfig the provider configuration
     * @param target the target provider invoker
     * @return the provider invoker with applied filter chain
     */
    public static <T> ProviderInvoker<T> buildProviderChain(ProviderConfig<T> providerConfig,
            ProviderInvoker<T> target) {
        return new ProviderFilterInvoker<>(providerConfig, target);
    }

    /**
     * Support "filter" at the method granularity. buildConsumerChain.
     *
     * @param consumerConfig the consumer configuration
     * @param target the target consumer invoker
     * @return the consumer invoker with applied filter chain
     */
    public static <T> ConsumerInvoker<T> buildConsumerChain(ConsumerConfig<T> consumerConfig,
            ConsumerInvoker<T> target) {
        return new ConsumerFilterInvoker<>(consumerConfig, target);
    }

}