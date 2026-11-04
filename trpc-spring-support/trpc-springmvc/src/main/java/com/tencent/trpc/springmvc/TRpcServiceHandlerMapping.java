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

package com.tencent.trpc.springmvc;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.filter.FilterChain;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.common.RpcMethodInfoAndInvoker;
import com.tencent.trpc.core.rpc.def.DefMethodInfoRegister;
import com.tencent.trpc.core.rpc.def.DefProviderInvoker;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

/**
 * TRPC service routing management class.
 * <p>tRPC services are exported by {@link TRpcServiceExportListener} after tRPC has started. This class
 * does not listen to context events itself, because its {@link #getOrder()} is the handler mapping priority and
 * must not be coupled with the event listener order.</p>
 */
public class TRpcServiceHandlerMapping extends AbstractHandlerMapping {

    private static final Logger logger = LoggerFactory.getLogger(TRpcServiceHandlerMapping.class);

    private final DefMethodInfoRegister methodRegister = new DefMethodInfoRegister();
    /**
     * Flag used to detect whether the tRPC method registered with the Spring HandlerMapping
     */
    private final AtomicBoolean registered = new AtomicBoolean(false);

    public TRpcServiceHandlerMapping() {
        setOrder(Ordered.HIGHEST_PRECEDENCE + 50000);
    }

    /**
     * Export tRPC rest services to spring mvc. Must be called after tRPC has started,
     * so that {@link ProviderConfig} refs are replaced by spring beans and initialized.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void exportTRpcServices() {
        if (!registered.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
            logger.info("already exported trpc backends to springmvc");
            return;
        }
        logger.info("begin exporting trpc backends to springmvc");
        Map<String, ServiceConfig> serviceMap = ConfigManager.getInstance().getServerConfig().getServiceMap();
        for (Entry<String, ServiceConfig> serviceConfigEntry : serviceMap.entrySet()) {
            String serviceName = serviceConfigEntry.getKey();
            ServiceConfig serviceConfig = serviceConfigEntry.getValue();
            if (!serviceConfig.getProtocol().equals("rest")) {
                continue;
            }
            logger.debug("regitst trpc springmvc, serviceConfig: {}, serviceProviders: {}",
                    serviceConfig, serviceConfig.getProviderConfigs());
            ProtocolConfig protocolConfig = serviceConfig.getProtocolConfig();
            for (ProviderConfig providerConfig : serviceConfig.getProviderConfigs()) {
                try {
                    methodRegister.register(FilterChain.buildProviderChain(providerConfig,
                            new DefProviderInvoker(protocolConfig, providerConfig)));
                    logger.info("trpc springmvc exported {}, basePath is {}",
                            providerConfig.getServiceInterface(), serviceConfig.getBasePath());
                } catch (Exception e) {
                    logger.error("trpc springmvc export " + providerConfig.getServiceInterface() + " in "
                            + serviceName + " failed", e);
                }
            }
        }
        logger.info("exporting trpc backends to springmvc finished");
    }

    @Override
    protected RpcMethodInfoAndInvoker getHandlerInternal(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        logger.debug("got trpc springmvc request {}", requestPath);
        String method = request.getMethod();
        if (!TRpcHttpConstants.HTTP_METHOD_GET.equals(method)
                && !TRpcHttpConstants.HTTP_METHOD_POST.equals(method)) {
            return null;
        }
        RpcMethodInfoAndInvoker route = getRpcMethodInfoAndInvoker(request, requestPath);
        setServiceMethod(request, route);
        return route;
    }

    /**
     * Set service method in http request attribute
     *
     * @param request http request
     * @param route router info
     */
    private void setServiceMethod(HttpServletRequest request, RpcMethodInfoAndInvoker route) {
        if (null != route) {
            request.setAttribute(TRpcHttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE,
                    route.getMethodRouterKey().getRpcServiceName());
            request.setAttribute(TRpcHttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD,
                    route.getMethodRouterKey().getRpcMethodName());
        }
    }

    /**
     * Get the mapped internal method.
     *
     * @param request HttpServletRequest
     * @param requestPath the request url path
     * @return the mapped internal method
     */
    private RpcMethodInfoAndInvoker getRpcMethodInfoAndInvoker(HttpServletRequest request, String requestPath) {
        String func = methodRegister.getNativeHttpFunc(requestPath);
        RpcMethodInfoAndInvoker route = methodRegister.route(func);
        if (null == route) {
            String rpcMethodName = request.getParameter(TRpcHttpConstants.TRPC_PARAM_METHOD);
            String rpcServiceName = request.getParameter(TRpcHttpConstants.TRPC_PARAM_SERVICE);
            route = methodRegister.route(rpcServiceName, rpcMethodName);
        }
        return route;
    }

}
