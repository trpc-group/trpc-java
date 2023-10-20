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

package com.tencent.trpc.opentelemetry.sdk.support;

import com.google.common.base.Joiner;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.GlobalConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.RequestMeta;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.opentelemetry.sdk.Constants;
import com.tencent.trpc.opentelemetry.spi.ITrpcAttributesExtractor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * Default implementation of {@link ITrpcAttributesExtractor}
 */
public class DefaultTrpcAttributesExtractor implements ITrpcAttributesExtractor {

    private static volatile Map<String, String> serverData = null;

    /**
     * Parse service configuration from resource
     */
    private static void initServerData() {
        if (serverData != null) {
            return;
        }
        synchronized (DefaultTrpcAttributesExtractor.class) {
            if (serverData != null) {
                return;
            }
            Map<String, String> data = new HashMap<>();
            ServerConfig serverConfig = ConfigManager.getInstance().getServerConfig();
            if (serverConfig != null) {
                String callee = Joiner.on(".")
                        .join(notNullStr(serverConfig.getApp(), ""), notNullStr(serverConfig.getServer(), ""));
                data.put(Constants.CALLEE_SERVICE_KEY, callee);
            }
            GlobalConfig globalConfig = ConfigManager.getInstance().getGlobalConfig();
            if (globalConfig != null) {
                String namespace = ConfigManager.getInstance().getGlobalConfig().getNamespace();
                String envName = ConfigManager.getInstance().getGlobalConfig().getEnvName();
                data.put(Constants.ENV_NAME_KEY, envName);
                data.put(Constants.NAMESPACE_KEY, namespace);
            }
            serverData = data;
        }
    }

    private static String notNullStr(String str1, String str2) {
        return (str1 != null ? str1 : str2);
    }

    @Override
    public Map<String, String> extract(Request request) {
        initServerData();
        Map<String, String> reportData = new HashMap<>(serverData);
        reportData.put(SemanticAttributes.RPC_SYSTEM.getKey(), "META-INF/trpc");
        reportData.put(SemanticAttributes.NET_TRANSPORT.getKey(), "ip_tcp");
        RequestMeta meta = request.getMeta();
        if (meta != null) {
            // Get remote ip info
            InetSocketAddress clientInfo = meta.getRemoteAddress();
            if (clientInfo != null) {
                reportData.put(SemanticAttributes.NET_PEER_IP.getKey(), clientInfo.getHostString());
                reportData.put(SemanticAttributes.NET_PEER_NAME.getKey(), clientInfo.getHostName());
                reportData.put(SemanticAttributes.NET_PEER_PORT.getKey(), String.valueOf(clientInfo.getPort()));
            }
            // Get server ip info
            InetSocketAddress serverInfo = meta.getLocalAddress();
            if (serverInfo != null) {
                reportData.put(SemanticAttributes.NET_HOST_IP.getKey(), serverInfo.getHostString());
                reportData.put(SemanticAttributes.NET_HOST_NAME.getKey(), serverInfo.getHostName());
                reportData.put(SemanticAttributes.NET_HOST_PORT.getKey(), String.valueOf(serverInfo.getPort()));
            }
            reportData.put(Constants.DYEING_KEY, meta.getDyeingKey());
            CallInfo callInfo = meta.getCallInfo();
            if (callInfo != null) {
                String calleeService = callInfo.getCalleeService();
                if (StringUtils.isBlank(calleeService)) {
                    calleeService = Joiner.on(".")
                            .join(notNullStr(callInfo.getCalleeApp(), ""), notNullStr(callInfo.getCalleeServer(), ""));
                }
                reportData.put(Constants.CALLEE_SERVICE_KEY, calleeService);
                reportData.put(Constants.CALLEE_METHOD_KEY, callInfo.getCalleeMethod());

                String callerService = callInfo.getCallerService();
                if (StringUtils.isBlank(callerService)) {
                    callerService = Joiner.on(".")
                            .join(notNullStr(callInfo.getCallerApp(), ""), notNullStr(callInfo.getCallerServer(), ""));
                }
                reportData.put(Constants.CALLER_SERVICE_KEY, callerService);
                reportData.put(Constants.CALLER_METHOD_KEY, callInfo.getCallerMethod());
            }
        }
        RpcInvocation invocation = request.getInvocation();
        if (invocation != null) {
            reportData.put(SemanticAttributes.RPC_SERVICE.getKey(), invocation.getRpcServiceName());
            reportData.put(SemanticAttributes.RPC_SERVICE.getKey(), invocation.getRpcServiceName());
        }

        return reportData;
    }
}
