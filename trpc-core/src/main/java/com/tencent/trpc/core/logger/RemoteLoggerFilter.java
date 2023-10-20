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

package com.tencent.trpc.core.logger;

import com.google.protobuf.Message;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.core.utils.ProtoJsonConverter;
import com.tencent.trpc.core.utils.TimerUtil;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * RequestLogFilter Flow log filter, can print flow logs for all requests.
 */
public abstract class RemoteLoggerFilter implements Filter {

    private static final String REQUEST_LOG_KEY = "RequestLog";

    private static final Logger logger = LoggerFactory.getLogger(RemoteLoggerFilter.class);

    private static final String LOG_SEPARATOR = "|";

    public abstract String getPluginName();

    @Override
    public CompletionStage<Response> filter(Invoker<?> invoker, Request request)
            throws TRpcException {
        RpcContext context = request.getContext();
        TimerUtil timerUtil = TimerUtil.newInstance();
        timerUtil.start();
        RemoteLogger remoteLogger = LoggerFactory.getRemoteLogger(getPluginName());
        CompletionStage<Response> result = invoker.invoke(request);
        result.whenComplete((r, t) -> {
            timerUtil.end();
            String msg = formatRequestLog(context, invoker, request, r, timerUtil.getCost());
            LoggerFactory.getLogger(REQUEST_LOG_KEY).info(msg);
            remoteLogger.info(context, msg);
        });
        return result;
    }

    private String formatRequestLog(RpcContext context, Invoker<?> invoker, Request request,
            Response response, long cost) {
        RpcInvocation inv = request.getInvocation();
        String serviceName = inv.getRpcServiceName();
        String methodName = inv.getRpcMethodName();
        StringBuilder sb = new StringBuilder();
        InetSocketAddress remoteAddress = request.getMeta().getRemoteAddress();
        sb.append(serviceName).append(LOG_SEPARATOR).append(methodName).append(LOG_SEPARATOR)
                .append(remoteAddress.getHostString()).append(LOG_SEPARATOR)
                .append(remoteAddress.getPort())
                .append(LOG_SEPARATOR).append(cost);
        if (inv.getRpcMethodInfo() != null) {
            Class<?>[] types = inv.getRpcMethodInfo().getMethod().getParameterTypes();
            sb.append(LOG_SEPARATOR);
            parseTypeName(sb, types);
            sb.append(LOG_SEPARATOR);
        }

        parseInvokeArguments(inv, sb);
        parseResponseValue(response, sb);
        return sb.toString();
    }

    private void parseResponseValue(Response response, StringBuilder sb) {
        Object value = response.getValue();
        if (value != null) {
            try {
                sb.append(LOG_SEPARATOR);
                // PB classes cannot be directly converted to JSON,
                // so they are first converted to a map and then to JSON.
                if (value instanceof Message) {
                    sb.append(JsonUtils.toJson(ProtoJsonConverter.messageToMap((Message) value)));
                } else {
                    sb.append(JsonUtils.toJson(value));
                }
            } catch (TRpcException e) {
                logger.error("json parse error,{}", e);
            }
        }
    }

    private void parseInvokeArguments(RpcInvocation inv, StringBuilder sb) {
        Object[] args = inv.getArguments();

        if (args != null && args.length > 0) {
            try {
                sb.append(JsonUtils.toJson(Arrays.stream(args)
                        .map(arg -> arg instanceof Message
                                ? ProtoJsonConverter.messageToMap((Message) arg) : Function.identity())
                        .collect(Collectors.toList())));
            } catch (Exception e) {
                logger.error("json parse error,{}", e);
            }
        }
    }

    private void parseTypeName(StringBuilder sb, Class<?>[] types) {
        if (types != null && types.length > 0) {
            boolean first = true;
            for (Class<?> type : types) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(type.getName());
            }
        }
    }

}