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

import com.google.common.base.Preconditions;

/**
 * TRPC Client Context.
 */
public class RpcClientContext extends RpcContext {

    /**
     * Remote service name to be called, commonly used for generic calls.
     */
    private String rpcServiceName;
    /**
     * Remote method name to be called, commonly used for generic calls.
     */
    private String rpcMethodName;
    /**
     * Method alias, commonly used for generic calls, equivalent to /rpcServiceName/rpcMethodName.
     */
    private String rpcMethodAlias;
    /**
     * Timeout setting.
     */
    private int timeoutMills;
    /**
     * Consistent hash key setting.
     */
    private String hashVal;

    public RpcClientContext clone() {
        RpcClientContext newClientContext = new RpcClientContext();
        cloneTo(newClientContext);
        newClientContext.setRpcServiceName(rpcServiceName);
        newClientContext.setRpcMethodName(rpcMethodName);
        newClientContext.setRpcMethodAlias(rpcMethodAlias);
        newClientContext.setTimeoutMills(timeoutMills);
        newClientContext.setHashVal(hashVal);
        return newClientContext;
    }

    @Override
    public String toString() {
        return "RpcClientContext [rpcServiceName=" + rpcServiceName + ", rpcMethodName="
                + rpcMethodName + ", rpcMethodAlias=" + rpcMethodAlias
                + ", timeoutMills=" + timeoutMills + ", hashVal=" + hashVal + ", superToString()="
                + super.toString() + "]";
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

    public String getRpcMethodAlias() {
        return rpcMethodAlias;
    }

    public void setRpcMethodAlias(String rpcMethodAlias) {
        this.rpcMethodAlias = rpcMethodAlias;
    }

    public int getTimeoutMills() {
        return timeoutMills;
    }

    public void setTimeoutMills(int timeoutMills) {
        Preconditions.checkArgument(timeoutMills >= 0, "timeoutMills < 0");
        this.timeoutMills = timeoutMills;
    }

    public String getHashVal() {
        return hashVal;
    }

    public void setHashVal(String hashVal) {
        this.hashVal = hashVal;
    }

}
