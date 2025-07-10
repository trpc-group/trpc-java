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

package com.tencent.trpc.spring.context;

import com.tencent.trpc.core.rpc.TRpcProxy;
import java.util.Objects;
import org.springframework.beans.factory.FactoryBean;

/**
 * {@link FactoryBean} for tRPC clients.
 *
 * @see TRpcProxy#getProxy(String, Class)
 */
public class TRpcClientFactoryBean<T> implements FactoryBean<T> {

    private final String name;
    private final Class<T> serviceInterface;

    public TRpcClientFactoryBean(String name, Class<T> serviceInterface) {
        this.name = Objects.requireNonNull(name);
        this.serviceInterface = Objects.requireNonNull(serviceInterface);
    }

    @Override
    public T getObject() {
        return TRpcProxy.getProxy(name, serviceInterface);
    }

    @Override
    public Class<?> getObjectType() {
        return serviceInterface;
    }

}
