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

/**
 * Method router.
 */
public class MethodRouterKey {

    /**
     * Remote service name.
     */
    private final String rpcServiceName;
    /**
     * Remote method name.
     */
    private final String rpcMethodName;
    /**
     * Route function, composition method rule /rpcServiceName/rpcMethodName
     *
     * @see MethodRouterKey#toFunc(String, String)
     */
    private final String func;

    public MethodRouterKey(String rpcServiceName, String rpcMethodName) {
        this.rpcServiceName = rpcServiceName;
        this.rpcMethodName = rpcMethodName;
        this.func = toFunc(rpcServiceName, rpcMethodName);
    }

    /**
     * Build function name
     *
     * @param rpcServiceName remote service name
     * @param rpcMethodName remote method name
     */
    public static String toFunc(String rpcServiceName, String rpcMethodName) {
        return String.format("/%s/%s", rpcServiceName, rpcMethodName);
    }

    public String getRpcServiceName() {
        return rpcServiceName;
    }

    public String getRpcMethodName() {
        return rpcMethodName;
    }

    /**
     * Convert . to / format.
     *
     * @return the converted string
     */
    public String getSlashFunc() {
        return func.replace(".", "/");
    }

    public String getNativeFunc() {
        return func;
    }

}
