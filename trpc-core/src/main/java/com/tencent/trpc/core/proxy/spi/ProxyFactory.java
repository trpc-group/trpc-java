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

package com.tencent.trpc.core.proxy.spi;

import com.tencent.trpc.core.extension.Extensible;
import java.lang.reflect.InvocationHandler;

/**
 * By default, bytebuddy is used, mainly because it offers higher performance.
 */
@Extensible("bytebuddy")
public interface ProxyFactory {

    <T> T getProxy(Class<T> serviceInterface, InvocationHandler invocationHandler);

}