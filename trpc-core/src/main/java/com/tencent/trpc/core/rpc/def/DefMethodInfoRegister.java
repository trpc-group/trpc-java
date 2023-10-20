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

package com.tencent.trpc.core.rpc.def;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.rpc.common.MethodRouterKey;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.rpc.common.RpcMethodInfoAndInvoker;
import com.tencent.trpc.core.utils.PreconditionUtils;
import com.tencent.trpc.core.utils.RpcUtils;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Default method route management class.
 */
public class DefMethodInfoRegister {

    private static final Logger logger = LoggerFactory.getLogger(DefMethodInfoRegister.class);
    /**
     * Service registration path, temporarily not supporting ANT style key: full path (base_path + func), value: func
     */
    private static final Map<String, String> SERVICE_PATH_AND_FUNC = Maps.newConcurrentMap();
    /**
     * Default service path.
     */
    private static final String DEFAULT_SERVICE_PATH = "/trpc";

    private ConcurrentMap<String, RpcMethodInfoAndInvoker> rpcMethodRouterMap =
            Maps.newConcurrentMap();

    private ConcurrentMap<String, RpcMethodInfoAndInvoker> defaultRpcMethodRouterMap = Maps.newConcurrentMap();

    /**
     * Register http mapping.
     *
     * @param providerInvoker provider
     */
    public void register(ProviderInvoker<?> providerInvoker) {
        ProviderConfig<?> providerConfig = providerInvoker.getConfig();
        Class<?> serviceInterface = providerConfig.getServiceInterface();
        String serviceInterfaceName = serviceInterface.getName();
        String rpcServiceName = RpcUtils.parseRpcServiceName(serviceInterface, null);
        PreconditionUtils.checkArgument(rpcServiceName != null,
                "export {interface = %s} error, no rpc service name", serviceInterfaceName);
        Arrays.stream(serviceInterface.getDeclaredMethods()).forEach(method -> {
            String rpcMethodName = RpcUtils.parseRpcMethodName(method, null);
            if (rpcMethodName == null) {
                logger.info("Export ignore {service={}, method={}, rpcService={}, rpcMethod=null}",
                        serviceInterfaceName, method.getName(), rpcServiceName);
                return;
            }
            RpcMethodInfo methodInfo = new RpcMethodInfo(serviceInterface, method);
            MethodRouterKey methodRouterKey = new MethodRouterKey(rpcServiceName, rpcMethodName);
            RpcMethodInfoAndInvoker obj = new RpcMethodInfoAndInvoker(methodInfo, providerInvoker, methodRouterKey);
            PreconditionUtils.checkArgument(!rpcMethodRouterMap.containsKey(methodRouterKey.getNativeFunc()),
                    "Export service fail, found duplicate router key {" + methodRouterKey.getNativeFunc() + "}");
            // Register internal method route 1. /trpc.${app}.${server}.${service}/${method}
            // 2. /trpc/${app}/${server}/${service}/${method}
            rpcMethodRouterMap.put(methodRouterKey.getNativeFunc(), obj);
            rpcMethodRouterMap.put(methodRouterKey.getSlashFunc(), obj);
            if (RpcUtils.isDefaultRpcMethod(method)) {
                defaultRpcMethodRouterMap.put(rpcServiceName, obj);
            }
            String[] rpcMethodAliases = RpcUtils.parseRpcMethodAliases(method, null);
            if (rpcMethodAliases != null) {
                Arrays.stream(rpcMethodAliases).forEach(rpcMethodAlias -> {
                    PreconditionUtils.checkArgument(!rpcMethodRouterMap.containsKey(rpcMethodAlias),
                            "Export service fail, found duplicate router key {" + rpcMethodAlias + "}");
                    rpcMethodRouterMap.put(rpcMethodAlias, obj);
                });
            }
            registerNativeHttpMapping(providerConfig, methodRouterKey, rpcMethodAliases);
            logger.info("Export service {service={}, method={}, rpcService={}, rpcMethod={}}",
                    serviceInterfaceName, method.getName(), rpcServiceName, rpcMethodName);
        });
    }

    /**
     * Register native HTTP internal route mapping.
     *
     * @param providerConfig service provider configuration
     * @param methodRouterKey route key
     * @param rpcMethodAliases array of RPC method aliases
     */
    private void registerNativeHttpMapping(ProviderConfig<?> providerConfig, MethodRouterKey methodRouterKey,
            String[] rpcMethodAliases) {
        String basePath = null != providerConfig.getServiceConfig().getBasePath() ? providerConfig.getServiceConfig()
                .getBasePath() : DEFAULT_SERVICE_PATH;

        // register base_path, base_path, compatible with access via service + name
        SERVICE_PATH_AND_FUNC.put(basePath, basePath);

        // register base_path + slashFunc, slashFunc
        SERVICE_PATH_AND_FUNC.put(buildServicePath(basePath, methodRouterKey.getSlashFunc()),
                methodRouterKey.getSlashFunc());

        // register base_path + nativeFunc, nativeFunc
        SERVICE_PATH_AND_FUNC.put(buildServicePath(basePath, methodRouterKey.getNativeFunc()),
                methodRouterKey.getNativeFunc());

        // register base_path + (slashFunc-trpc), slashFunc
        SERVICE_PATH_AND_FUNC.put(buildServicePath(basePath,
                        methodRouterKey.getSlashFunc().replace("/trpc", "")),
                methodRouterKey.getSlashFunc());

        // register slashFunc, slashFunc
        SERVICE_PATH_AND_FUNC.put(methodRouterKey.getSlashFunc(), methodRouterKey.getSlashFunc());

        // register nativeFunc, nativeFunc
        SERVICE_PATH_AND_FUNC.put(methodRouterKey.getNativeFunc(), methodRouterKey.getNativeFunc());

        if (rpcMethodAliases != null) {
            Arrays.stream(rpcMethodAliases).forEach(rpcMethodAlias -> {
                SERVICE_PATH_AND_FUNC.put(buildServicePath(basePath, rpcMethodAlias), methodRouterKey.getNativeFunc());
                SERVICE_PATH_AND_FUNC.put(rpcMethodAlias, methodRouterKey.getNativeFunc());
            });
        }
    }

    /**
     * Unregister native HTTP internal route.
     *
     * @param providerConfig service provider configuration
     * @param methodRouterKey route key
     * @param rpcMethodAliases array of RPC method aliases
     */
    private void unregisterNativeHttpMapping(ProviderConfig<?> providerConfig, MethodRouterKey methodRouterKey,
            String[] rpcMethodAliases) {
        String basePath = null != providerConfig.getServiceConfig().getBasePath() ? providerConfig.getServiceConfig()
                .getBasePath() : DEFAULT_SERVICE_PATH;

        // unregister base_path + slashFunc, slashFunc
        SERVICE_PATH_AND_FUNC.remove(buildServicePath(basePath, methodRouterKey.getSlashFunc()),
                methodRouterKey.getSlashFunc());

        // unregister base_path + nativeFunc, nativeFunc
        SERVICE_PATH_AND_FUNC.remove(buildServicePath(basePath, methodRouterKey.getNativeFunc()),
                methodRouterKey.getNativeFunc());

        // unregister base_path + (slashFunc-trpc), slashFunc
        SERVICE_PATH_AND_FUNC.remove(buildServicePath(basePath,
                        methodRouterKey.getSlashFunc().replace("/trpc", "")),
                methodRouterKey.getSlashFunc());

        // unregister slashFunc, slashFunc
        SERVICE_PATH_AND_FUNC.remove(methodRouterKey.getSlashFunc(), methodRouterKey.getSlashFunc());

        // unregister nativeFunc, nativeFunc
        SERVICE_PATH_AND_FUNC.remove(methodRouterKey.getNativeFunc(), methodRouterKey.getNativeFunc());

        if (rpcMethodAliases != null) {
            Arrays.stream(rpcMethodAliases).forEach(rpcMethodAlias -> {
                SERVICE_PATH_AND_FUNC.remove(buildServicePath(basePath, rpcMethodAlias),
                        methodRouterKey.getNativeFunc());
                SERVICE_PATH_AND_FUNC.remove(rpcMethodAlias, methodRouterKey.getNativeFunc());
            });
        }
    }

    private String buildServicePath(String basePath, String func) {
        return basePath + func;
    }

    /**
     * Unregister service.
     *
     * @param providerConfig service provider configuration
     */
    public void unregister(ProviderConfig<?> providerConfig) {
        Class<?> serviceInterface = providerConfig.getServiceInterface();
        String serviceInterfaceName = serviceInterface.getName();
        String rpcServiceName = RpcUtils.parseRpcServiceName(serviceInterface, null);
        PreconditionUtils.checkArgument(rpcServiceName != null,
                "UnExport {interface = %s} error, no rpc service name", serviceInterfaceName);
        Arrays.stream(serviceInterface.getDeclaredMethods()).forEach(method -> {
            String rpcMethodName = RpcUtils.parseRpcMethodName(method, null);
            String[] rpcMethodAliases = RpcUtils.parseRpcMethodAliases(method, null);
            if (rpcMethodName != null) {
                MethodRouterKey methodRouterKey = new MethodRouterKey(rpcServiceName, rpcMethodName);
                rpcMethodRouterMap.remove(methodRouterKey.getSlashFunc());
                rpcMethodRouterMap.remove(methodRouterKey.getNativeFunc());
                if (rpcMethodAliases != null) {
                    Arrays.stream(rpcMethodAliases).forEach(rpcMethodAlias ->
                            rpcMethodRouterMap.remove(rpcMethodAlias));
                }
                unregisterNativeHttpMapping(providerConfig, methodRouterKey, rpcMethodAliases);
                logger.info("UnExport service {service={}, method={}, rpcService={}, rpcMethod={}}",
                        serviceInterfaceName, method.getName(), rpcServiceName, rpcMethodName);
            }
        });

        this.defaultRpcMethodRouterMap.remove(rpcServiceName);
    }

    public RpcMethodInfoAndInvoker route(String func) {
        return rpcMethodRouterMap.get(func == null ? "" : func);
    }

    public RpcMethodInfoAndInvoker route(String rpcServiceName, String rpcMethodName) {
        return this.route(MethodRouterKey.toFunc(rpcServiceName, rpcMethodName));
    }

    public RpcMethodInfoAndInvoker getDefaultRouter(String rpcServiceName) {
        return defaultRpcMethodRouterMap.get(rpcServiceName);
    }

    /**
     * Check if the path exists.
     *
     * @param path path to check
     * @return true if the path exists
     */
    public boolean validateNativeHttpPath(String path) {
        return SERVICE_PATH_AND_FUNC.containsKey(path);
    }

    /**
     * Get the func corresponding to the path.
     *
     * @param path path to get the func
     * @return func path
     */
    public String getNativeHttpFunc(String path) {
        return SERVICE_PATH_AND_FUNC.get(path);
    }

    public void clear() {
        rpcMethodRouterMap.clear();
    }

}
