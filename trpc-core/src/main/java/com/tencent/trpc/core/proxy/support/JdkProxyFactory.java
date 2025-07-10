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

package com.tencent.trpc.core.proxy.support;

import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.proxy.spi.ProxyFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Objects;

@Extension(JdkProxyFactory.NAME)
public class JdkProxyFactory implements ProxyFactory {

    public static final String NAME = "jdk";

    @Override
    public <T> T getProxy(Class<T> serviceType, InvocationHandler invocationHandler) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = this.getClass().getClassLoader();
        }
        Objects.requireNonNull(classLoader, "classLoader is null");
        return (T) Proxy.newProxyInstance(classLoader, new Class<?>[]{serviceType}, invocationHandler);
    }

}