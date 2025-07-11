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

package com.tencent.trpc.core.rpc.common;

import com.tencent.trpc.core.rpc.ProviderInvoker;

/**
 * Route mapping information, the mapping of route method information and its implementation.
 */
public class RpcMethodInfoAndInvoker {

    /**
     * RPC method information.
     */
    private RpcMethodInfo methodInfo;
    /**
     * Service provider Invoker.
     */
    private ProviderInvoker<?> invoker;
    /**
     * Route key.
     */
    private MethodRouterKey methodRouterKey;

    public RpcMethodInfoAndInvoker() {
        super();
    }

    public RpcMethodInfoAndInvoker(RpcMethodInfo methodInfo, ProviderInvoker<?> invoker,
            MethodRouterKey methodRouterKey) {
        super();
        this.methodInfo = methodInfo;
        this.invoker = invoker;
        this.methodRouterKey = methodRouterKey;
    }

    public RpcMethodInfo getMethodInfo() {
        return methodInfo;
    }

    public void setMethodInfo(RpcMethodInfo methodInfo) {
        this.methodInfo = methodInfo;
    }

    public ProviderInvoker<?> getInvoker() {
        return invoker;
    }

    public void setInvoker(ProviderInvoker<?> invoker) {
        this.invoker = invoker;
    }

    public MethodRouterKey getMethodRouterKey() {
        return methodRouterKey;
    }

    public void setMethodRouterKey(MethodRouterKey methodRouterKey) {
        this.methodRouterKey = methodRouterKey;
    }

}
