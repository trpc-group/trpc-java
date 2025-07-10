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
import java.lang.reflect.Method;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;

@Extension(ByteBuddyProxyFactory.NAME)
public class ByteBuddyProxyFactory implements ProxyFactory {

    public static final String NAME = "bytebuddy";

    @Override
    public <T> T getProxy(Class<T> serviceInterface, InvocationHandler invocationHandler) {
        Class<? extends T> cls = new ByteBuddy().subclass(serviceInterface)
                .method(ElementMatchers.isDeclaredBy(serviceInterface)
                        .or(ElementMatchers.isEquals())
                        .or(ElementMatchers.isToString().or(ElementMatchers.isHashCode())))
                .intercept(MethodDelegation
                        .to(new BytebuddyInvocationHandler<>(serviceInterface, invocationHandler)))
                .make()
                .load(serviceInterface.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        try {
            return cls.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("getProxy [" + serviceInterface.getName() + "] exception", e);
        }
    }

    public static class BytebuddyInvocationHandler<T> {

        private Class<T> clazz;
        private InvocationHandler invocationHandler;

        public BytebuddyInvocationHandler(Class<T> clazz, InvocationHandler invocationHandler) {
            this.clazz = clazz;
            this.invocationHandler = invocationHandler;
        }

        @RuntimeType
        public Object invoke(@This Object proxy, @Origin Method method,
                @AllArguments @RuntimeType Object[] args) throws Throwable {
            return invocationHandler.invoke(proxy, method, args);
        }
    }

}