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

package com.tencent.trpc.core.rpc;

import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import org.apache.commons.lang3.ArrayUtils;

/**
 * RPC invocation context.
 */
public class RpcInvocation {

    /**
     * Remote service name.
     */
    private String rpcServiceName;
    /**
     * Remote method name.
     */
    private String rpcMethodName;
    /**
     * Routing func, rule: /remote service name/remote method name.
     */
    private String func;
    /**
     * Service parameters.
     */
    private Object[] arguments;
    /**
     * Corresponding method metadata information.
     */
    private RpcMethodInfo rpcMethodInfo;

    /**
     * Get the first parameter value, suitable for most scenarios.
     */
    public Object getFirstArgument() {
        return (arguments == null || arguments.length <= 0) ? null : arguments[0];
    }

    public void setFirstArgument(Object obj) {
        if (ArrayUtils.isEmpty(arguments)) {
            arguments = new Object[]{obj};
        } else {
            arguments[0] = obj;
        }
    }

    @Override
    public String toString() {
        return "RpcInvocation {"
                + "  rpcServiceName=" + rpcServiceName
                + ", rpcMethodName=" + rpcMethodName
                + ", func=" + func
                + '}';
    }

    public InvokeMode getInvokeMode() {
        return rpcMethodInfo != null ? rpcMethodInfo.getInvokeMode() : null;
    }

    public boolean isGeneric() {
        return rpcMethodInfo != null && rpcMethodInfo.isGeneric();
    }

    public String getRpcServiceName() {
        return rpcServiceName;
    }

    public void setRpcServiceName(String rpcServiceName) {
        this.rpcServiceName = rpcServiceName;
    }

    public String getRpcMethodName() {
        return rpcMethodName;
    }

    public void setRpcMethodName(String rpcMethodName) {
        this.rpcMethodName = rpcMethodName;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public void setArguments(Object[] arguments) {
        this.arguments = arguments;
    }

    public RpcMethodInfo getRpcMethodInfo() {
        return rpcMethodInfo;
    }

    public void setRpcMethodInfo(RpcMethodInfo rpcMethodInfo) {
        this.rpcMethodInfo = rpcMethodInfo;
    }

    public String getFunc() {
        return func;
    }

    public void setFunc(String func) {
        this.func = func;
    }

}
