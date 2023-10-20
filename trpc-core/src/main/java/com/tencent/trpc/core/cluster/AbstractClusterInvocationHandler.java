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

package com.tencent.trpc.core.cluster;

import com.google.common.base.Preconditions;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.rpc.def.LeftTimeout;
import com.tencent.trpc.core.utils.RpcUtils;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.commons.lang3.StringUtils;

/**
 * Abstract cluster proxy handler.
 * Mainly abstracts the common process part of standard and streaming services.
 * Validate local method -> check parameters -> build RpcInvocation -> validate timeout -> build Request ->
 * send request -> parse response.
 */
public abstract class AbstractClusterInvocationHandler implements InvocationHandler {

    protected final ClusterInvoker<?> invoker;

    protected final Class<?> clazz;

    protected final ConsumerConfig<?> consumerConfig;

    public AbstractClusterInvocationHandler(ClusterInvoker<?> invoker) {
        this.invoker = Objects.requireNonNull(invoker);
        this.consumerConfig = invoker.getConfig();
        this.clazz = consumerConfig.getServiceInterface();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (isLocalMethod(method)) {
            return invokeLocal(method, args);
        }
        validateArgs(method, args);
        RpcInvocation rpcInvocation = buildRpcInvocation(method, args);
        LeftTimeout leftTimeout = validateTimeout(rpcInvocation, args);
        Request request = buildRequest(leftTimeout, rpcInvocation, args);
        CompletionStage<Response> response = invoker.invoke(request);
        return parseResponse(request, leftTimeout, response);
    }

    /**
     * Validate parameters.
     *
     * @param method method reference
     * @param args parameters
     */
    protected void validateArgs(Method method, Object[] args) {
        // check1: The first parameter must be RpcContext
        Preconditions.checkArgument((args.length > 0 && (args[0] instanceof RpcClientContext)),
                "args[0] type should be RpcContext");
    }

    /**
     * Build RpcInvocation.
     *
     * @param method method reference
     * @param args parameters
     * @return RpcInvocation
     */
    protected RpcInvocation buildRpcInvocation(Method method, Object[] args) {
        Class<?> serviceType = invoker.getInterface();
        String rpcServiceName = parseRpcServiceName(serviceType, args[0]);
        String rpcMethodName = parseRpcMethodName(method, args[0]);
        Objects.requireNonNull(rpcServiceName, " Rpc service name is null");
        Objects.requireNonNull(rpcMethodName, " Rpc method name is null");
        RpcInvocation invocation = new RpcInvocation();
        invocation.setRpcServiceName(rpcServiceName);
        invocation.setRpcMethodName(rpcMethodName);
        invocation.setFunc(String.format("/%s/%s", rpcServiceName, rpcMethodName));
        String rpcMethodAlias = parseRpcMethodAlias(method, args[0]);
        if (StringUtils.isNotBlank(rpcMethodAlias)) {
            invocation.setFunc(rpcMethodAlias);
        }
        invocation.setRpcMethodInfo(new RpcMethodInfo(clazz, method));
        Object[] actualArgs = getActualArgs(args);
        invocation.setArguments(actualArgs);
        return invocation;
    }

    /**
     * Get the actual parameter types.
     *
     * @param args parameter list
     * @return actual parameter list
     */
    protected Object[] getActualArgs(Object[] args) {
        Object[] argsWithoutContext = new Object[args.length - 1];
        System.arraycopy(args, 1, argsWithoutContext, 0, args.length - 1);
        return argsWithoutContext;
    }

    /**
     * Parse method name.
     * Now it is to be compatible with Context and RpcContext, modify Context when unifying.
     *
     * @param method method reference
     * @param context context
     * @return method name
     */
    protected String parseRpcMethodName(Method method, Object context) {
        RpcClientContext ctx = (RpcClientContext) context;
        return StringUtils.isNotBlank(ctx.getRpcMethodName()) ? ctx.getRpcMethodName()
                : RpcUtils.parseRpcMethodName(method, null);
    }

    /**
     * Parse method alias.
     *
     * @param method the method reference
     * @param context the context
     * @return the method alias
     */
    protected String parseRpcMethodAlias(Method method, Object context) {
        RpcClientContext ctx = (RpcClientContext) context;
        String rpcMethodAlias = ctx.getRpcMethodAlias();
        if (StringUtils.isNotBlank(rpcMethodAlias)) {
            return rpcMethodAlias;
        }
        String[] rpcMethodAliases = RpcUtils.parseRpcMethodAliases(method, null);
        return rpcMethodAliases != null && rpcMethodAliases.length > 0 ? rpcMethodAliases[0] : rpcMethodAlias;
    }

    /**
     * Parse service name.
     *
     * @param serviceType the interface type
     * @param context the context
     * @return the service name
     */
    protected String parseRpcServiceName(Class<?> serviceType, Object context) {
        RpcClientContext ctx = (RpcClientContext) context;
        // Priority: context specified > interface annotation > 123 platform TRPC framework configuration
        String serviceName = ctx.getRpcServiceName();
        if (StringUtils.isBlank(serviceName)) {
            serviceName = RpcUtils.parseRpcServiceName(serviceType, null);
        }
        if (StringUtils.isBlank(serviceName)) {
            serviceName = consumerConfig.getBackendConfig().getCallee();
        }
        return serviceName;
    }

    /**
     * Validate if timeout.
     *
     * @param invocation the RpcInvocation object
     * @param args the parameters
     * @return the LeftTimeout object
     */
    protected abstract LeftTimeout validateTimeout(RpcInvocation invocation, Object[] args);

    /**
     * Build request.
     *
     * @param leftTimeout the LeftTimeout object
     * @param invocation the RpcInvocation object
     * @param args the parameters
     * @return the request
     */
    protected abstract Request buildRequest(LeftTimeout leftTimeout, RpcInvocation invocation, Object[] args);

    /**
     * Parse response.
     *
     * @param request the request
     * @param leftTimeout the LeftTimeout object
     * @param response the response future
     * @return the response object
     */
    protected abstract Object parseResponse(Request request, LeftTimeout leftTimeout,
            CompletionStage<Response> response);

    /**
     * Determine if it is a local method.
     *
     * @param method the method reference
     * @return true if it is a local method, false otherwise
     */
    protected boolean isLocalMethod(Method method) {
        if (method.getDeclaringClass().equals(Object.class)) {
            try {
                clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
                return false;
            } catch (Exception e) {
                return true;
            }
        }
        return false;
    }

    /**
     * Call local method.
     *
     * @param method the method reference
     * @param args the parameters
     * @return the return value of the call
     */
    protected Object invokeLocal(Method method, Object[] args) {
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if ("toString".equals(methodName) && parameterTypes.length == 0) {
            return this.toString();
        }
        if ("hashCode".equals(methodName) && parameterTypes.length == 0) {
            return this.hashCode();
        }
        if ("equals".equals(methodName) && parameterTypes.length == 1) {
            return this.equals(args[0]);
        }
        throw TRpcException.newFrameException(ErrorCode.TRPC_INVOKE_UNKNOWN_ERR,
                "not allow invoke local method:" + method.getName());
    }

    /**
     * Set the properties of the called party.
     * CalleeAPP, CalleeServer, CalleeService, CalleeMethod.
     *
     * @param inv the RpcInvocation object
     * @param newCallInfo the CallInfo object
     */
    protected void setCallee(RpcInvocation inv, CallInfo newCallInfo) {
        if (StringUtils.isBlank(newCallInfo.getCallee())) {
            newCallInfo.setCallee(consumerConfig.getBackendConfig().getCallee());
        }
        if (StringUtils.isBlank(newCallInfo.getCalleeApp())) {
            newCallInfo.setCalleeApp(consumerConfig.getBackendConfig().getCalleeApp());
        }
        if (StringUtils.isBlank(newCallInfo.getCalleeServer())) {
            newCallInfo.setCalleeServer(consumerConfig.getBackendConfig().getCalleeServer());
        }
        if (StringUtils.isBlank(newCallInfo.getCalleeService())) {
            newCallInfo.setCalleeService(consumerConfig.getBackendConfig().getCalleeService());
        }
        if (StringUtils.isBlank(newCallInfo.getCalleeMethod()) && inv != null) {
            newCallInfo.setCalleeMethod(inv.getRpcMethodName());
        }
    }

    /**
     * Set the properties of the caller, such as app, server, caller, etc.
     *
     * @param newCallInfo CallInfo object
     * @param serverConfig server configuration
     */
    protected void setCaller(CallInfo newCallInfo, ServerConfig serverConfig) {
        if (StringUtils.isBlank(newCallInfo.getCallerApp())) {
            newCallInfo.setCallerApp(serverConfig.getApp());
        }
        if (StringUtils.isBlank(newCallInfo.getCallerServer())) {
            newCallInfo.setCallerServer(serverConfig.getServer());
        }
        if (StringUtils.isBlank(newCallInfo.getCaller())) {
            String defCaller = "trpc." + serverConfig.getApp() + "." + serverConfig.getServer();
            newCallInfo.setCaller(defCaller);
        }
        Optional.ofNullable(ConfigManager.getInstance().getGlobalConfig().getContainerName())
                .ifPresent(newCallInfo::setCallerContainerName);
        if (ConfigManager.getInstance().getGlobalConfig().isEnableSet()) {
            Optional.ofNullable(ConfigManager.getInstance().getGlobalConfig().getFullSetName())
                    .ifPresent(newCallInfo::setCallerSetName);
        }
    }

}
