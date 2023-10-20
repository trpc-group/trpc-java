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

package com.tencent.trpc.core.rpc.common;

import com.tencent.trpc.core.common.RpcResult;
import com.tencent.trpc.core.rpc.InvokeMode;
import com.tencent.trpc.core.utils.RpcUtils;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * RpcMethod information definition.
 */
public class RpcMethodInfo {

    private final Class<?> serviceInterface;
    private final Method method;
    /**
     * Parameterized types.
     */
    private final Type[] paramsTypes;
    /**
     * Actual types of generic parameters.
     */
    private final Type[] actualParamsTypes;
    /**
     * May be generic.
     *
     * In asynchronous scenarios, if the return type is {@link RpcResult}, set returnType to {@link RpcResult}.
     * In other cases, set returnType to {@link java.util.concurrent.CompletionStage}.
     */
    private final Type returnType;
    /**
     * Actual generic type.
     */
    private final Type actualReturnType;
    /**
     * Invocation mode.
     **/
    private final InvokeMode invokeMode;
    /**
     * Whether it is generic.
     */
    private final boolean isGeneric;

    /**
     * RpcMethodInfo constructor.
     *
     * @param serviceInterface the service interface
     * @param method the method
     */
    public RpcMethodInfo(Class<?> serviceInterface, Method method) {
        this.serviceInterface = serviceInterface;
        this.method = method;
        this.paramsTypes = method.getGenericParameterTypes();
        this.invokeMode = RpcUtils.parseInvokeMode(method);
        if (InvokeMode.isAsync(invokeMode)) {
            this.actualParamsTypes = paramsTypes;
            Type genericReturnType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
            if (genericReturnType instanceof ParameterizedType
                    && ((ParameterizedType) genericReturnType).getRawType() == RpcResult.class) {
                this.returnType = ((ParameterizedType) genericReturnType).getRawType();
                this.actualReturnType = ((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
            } else {
                this.returnType = method.getReturnType();
                this.actualReturnType = genericReturnType;
            }
        } else if (InvokeMode.isStream(invokeMode)) {
            this.returnType = method.getReturnType();
            this.actualParamsTypes = new Type[paramsTypes.length];
            for (int i = 0; i < paramsTypes.length; i++) {
                Type paramType = paramsTypes[i];
                if (paramType instanceof ParameterizedType) {
                    this.actualParamsTypes[i] = ((ParameterizedType) paramType).getActualTypeArguments()[0];
                } else {
                    this.actualParamsTypes[i] = paramType;
                }
            }
            this.actualReturnType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        } else {
            this.returnType = method.getReturnType();
            this.actualParamsTypes = paramsTypes;
            Type genericReturnType = method.getGenericReturnType();
            if (genericReturnType instanceof ParameterizedType && returnType == RpcResult.class) {
                this.actualReturnType = ((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
            } else {
                this.actualReturnType = genericReturnType;
            }
        }
        this.isGeneric = RpcUtils.isGenericClient(serviceInterface) || RpcUtils.isGenericMethod(method);
    }

    public Type[] getParamsTypes() {
        return paramsTypes;
    }

    public Type[] getActualParamsTypes() {
        return actualParamsTypes;
    }

    public Class<?> getServiceInterface() {
        return serviceInterface;
    }

    public InvokeMode getInvokeMode() {
        return invokeMode;
    }

    public Type getActualReturnType() {
        return actualReturnType;
    }

    public Type getReturnType() {
        return returnType;
    }

    public Method getMethod() {
        return method;
    }

    public boolean isGeneric() {
        return isGeneric;
    }

}
