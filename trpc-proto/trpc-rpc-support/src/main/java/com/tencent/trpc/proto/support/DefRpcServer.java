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

package com.tencent.trpc.proto.support;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.ExceptionHelper;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.AbstractRpcServer;
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.RequestMeta;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcContextValueKeys;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.RpcServer;
import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.core.rpc.common.MethodRouterKey;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.rpc.common.RpcMethodInfoAndInvoker;
import com.tencent.trpc.core.rpc.def.DecodableValue;
import com.tencent.trpc.core.rpc.def.DefMethodInfoRegister;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.ServerTransport;
import com.tencent.trpc.core.transport.codec.ServerCodec;
import com.tencent.trpc.core.transport.handler.ChannelHandlerAdapter;
import com.tencent.trpc.core.transport.spi.ServerTransportFactory;
import com.tencent.trpc.core.utils.PreconditionUtils;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.core.utils.RpcUtils;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.RejectedExecutionException;
import org.apache.commons.lang3.StringUtils;

/**
 * Default implementation of {@link RpcServer}
 *
 * @see AbstractRpcServer
 * @see RpcServer
 */
public class DefRpcServer extends AbstractRpcServer {

    private static final Logger LOG = LoggerFactory.getLogger(DefRpcServer.class);
    /**
     * ServerTransport
     */
    private final ServerTransport server;
    /**
     * InternalHandler
     */
    private final InternalHandler handler;
    /**
     * MethodRegister
     */
    private final DefMethodInfoRegister methodRegister = new DefMethodInfoRegister();
    /**
     * ServerCodec
     */
    private final ServerCodec serverCodec;

    public DefRpcServer(ProtocolConfig config, ServerCodec serverCodec) {
        super.setConfig(config);
        String transportType = config.getTransporter();
        ServerTransportFactory serverFactory = ExtensionLoader
                .getExtensionLoader(ServerTransportFactory.class).getExtension(transportType);
        PreconditionUtils.checkArgument(serverFactory != null, "transport[%] not support",
                transportType);
        this.handler = new InternalHandler();
        this.server = serverFactory.create(config, this.handler, serverCodec);
        this.serverCodec = serverCodec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doOpen() {
        Objects.requireNonNull(handler, "handler is null");
        Objects.requireNonNull(server, "server is null");
        server.open();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doClose() {
        if (server != null) {
            try {
                server.close();
            } catch (Throwable ex) {
                LOG.error("", ex);
            }
        }
        if (handler != null) {
            try {
                handler.destroy();
            } catch (Throwable ex) {
                LOG.error("", ex);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <T> void doExport(ProviderInvoker<T> providerInvoker) {
        handler.register(providerInvoker);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <T> void doUnExport(ProviderConfig<T> config) {
    }

    /**
     * Implements {@link com.tencent.trpc.core.transport.ChannelHandler} to handle client request
     */
    private class InternalHandler extends ChannelHandlerAdapter {

        InternalHandler() {
        }

        /**
         * Handles received data from client
         */
        @Override
        public void received(Channel channel, Object message) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(">>>Server receive request:{}", message);
            }
            if (message instanceof List) {
                batchProcess(channel, message);
            } else {
                process(channel, message);
            }
        }

        @Override
        public void destroy() {
            try {
                methodRegister.clear();
            } catch (Throwable ex) {
                LOG.error("", ex);
            }
        }

        private void register(ProviderInvoker<?> providerInvoker) {
            methodRegister.register(providerInvoker);
        }

        private void batchProcess(Channel channel, Object message) {
            ((List<?>) message).forEach(msg -> process(channel, msg));
        }

        private void process(Channel channel, Object message) {
            if (!(message instanceof Request)) {
                if (LOG.isWarnEnabled()) {
                    String config = protocolConfig.toSimpleString();
                    String msgType = (message == null ? "<null>" : message.getClass().getName());
                    LOG.warn("Server received unknown message type(type = {}, config= {},"
                            + "channel={}), only support type(class=Request)", msgType, config, channel);
                }
                return;
            }
            Request request = (Request) message;
            if (!returnIfSignFail(channel, request)) {
                handle(channel, request);
            }
        }

        /**
         * Signature check
         */
        private boolean returnIfSignFail(Channel channel, Request request) {
            RpcContext context = request.getContext();
            Boolean signResult = RpcContextUtils.getValueMapValue(context,
                    RpcContextValueKeys.SERVER_SIGNATURE_VERIFY_RESULT_KEY);
            if (signResult) {
                return false;
            }
            errorReply(channel, request, ErrorCode.SIGNATURE_VERIFY_FAILURE, ErrorCode.TRPC_INVOKE_SUCCESS,
                    "Signature verification failed");
            return true;
        }

        /**
         * Handle request
         */
        private void handle(Channel channel, Request request) {
            RpcMethodInfoAndInvoker rpcMethodInfoAndInvoker;
            try {
                rpcMethodInfoAndInvoker = route(request, false);
                if (null == rpcMethodInfoAndInvoker) {
                    LOG.error("Dispatch request|" + request + " error, not find service");
                    errorReply(channel, request, ErrorCode.TRPC_SERVER_NOFUNC_ERR, 0,
                            "not find func:" + request.getInvocation().getFunc());
                    return;
                }

                prepareRequest(channel, request, rpcMethodInfoAndInvoker);
            } catch (Exception ex) {
                LOG.error("prepare request [" + request + "]  error", ex);
                if (ex instanceof TRpcException) {
                    errorReply(channel, request, ex);
                } else {
                    errorReply(channel, request, ErrorCode.TRPC_SERVER_DECODE_ERR, 0,
                            "codec error");
                }
                return;
            }
            try {
                ProviderInvoker<?> invoker = rpcMethodInfoAndInvoker.getInvoker();
                invoker.getConfig().getWorkerPoolObj().execute(() -> {
                    try {
                        dispatch(channel, invoker, request);
                    } catch (Throwable ex) {
                        LOG.error("Dispatch request|" + request + " error", ex);
                    }
                });
            } catch (Throwable ex) {
                LOG.error("Dispatch request [" + request + "]  error", ex);
                if (ex instanceof RejectedExecutionException) {
                    errorReply(channel, request, ErrorCode.TRPC_SERVER_OVERLOAD_ERR, 0,
                            "queue size full");
                } else if (ex instanceof TRpcException) {
                    errorReply(channel, request, ex);
                } else {
                    errorReply(channel, request, ErrorCode.TRPC_INVOKE_UNKNOWN_ERR, 0,
                            ex.getMessage());
                }
            }
        }

        /**
         * Find {@link RpcMethodInfoAndInvoker} related to the request
         */
        private RpcMethodInfoAndInvoker route(Request req, boolean ex) {
            RpcInvocation invocation = req.getInvocation();
            RpcMethodInfoAndInvoker route = methodRegister.route(invocation.getFunc());

            if (route == null) {
                route = methodRegister.getDefaultRouter(invocation.getRpcServiceName());
            }

            if (route != null) {
                return route;
            } else if (ex) {
                throw TRpcException.newFrameException(ErrorCode.TRPC_SERVER_NOFUNC_ERR,
                        "Not found {func=%s}", invocation.getFunc());
            }
            return null;
        }

        /**
         * Pre-invoking preparations
         */
        private void prepareRequest(Channel channel, Request request,
                                    RpcMethodInfoAndInvoker rpcMethodInfoAndInvoker) {
            if (request.getContext() == null) {
                request.setContext(new RpcServerContext());
            }
            RpcMethodInfo methodInfo = rpcMethodInfoAndInvoker.getMethodInfo();
            setInvocation(request, methodInfo, rpcMethodInfoAndInvoker.getMethodRouterKey());
            decodeRequestBody(request, methodInfo);
            RequestMeta meta = request.getMeta();
            if (meta.getRemoteAddress() == null) {
                meta.setRemoteAddress(channel.getRemoteAddress());
            }
            meta.setLocalAddress(getProtocolConfig().toInetSocketAddress());
        }

        private void setInvocation(Request request, RpcMethodInfo methodInfo, MethodRouterKey methodRouterKey) {
            if (null == request.getInvocation()) {
                throw new IllegalArgumentException(
                        String.format("Server(%s), request(%s), Request invocation is null",
                                protocolConfig.toSimpleString(), requestToString(request)));
            }
            RpcInvocation invocation = request.getInvocation();
            invocation.setRpcMethodInfo(methodInfo);
            invocation.setRpcServiceName(methodRouterKey.getRpcServiceName());
            invocation.setRpcMethodName(methodRouterKey.getRpcMethodName());
            CallInfo callInfo = request.getMeta().getCallInfo();
            if (callInfo != null) {
                if (StringUtils.isBlank(callInfo.getCalleeMethod())) {
                    callInfo.setCalleeMethod(invocation.getRpcMethodName());
                }
            }
        }

        private void decodeRequestBody(Request request, RpcMethodInfo methodInfo) {
            RpcInvocation invocation = request.getInvocation();
            invocation.setArguments(decodeArgument(invocation.getArguments(), methodInfo));
            if (serverCodec instanceof ServerRequestBodyCodec) {
                ((ServerRequestBodyCodec) serverCodec).decode(request, methodInfo);
            }
        }

        private Object[] decodeArgument(final Object[] rawArguments, RpcMethodInfo methodInfo) {
            Object[] realArguments = new Object[rawArguments.length];
            boolean isGeneric = methodInfo.isGeneric();
            for (int i = 0; i < rawArguments.length; i++) {
                if (rawArguments[i] instanceof DecodableValue) {
                    Type paramType = methodInfo.getParamsTypes()[i + 1];
                    DecodableValue decodableValue = (DecodableValue) rawArguments[i];
                    realArguments[i] = decodableValue.decode(paramType, isGeneric);
                } else {
                    realArguments[i] = rawArguments[i];
                }
            }
            return realArguments;
        }

        /**
         * Invoke actual business logic
         */
        protected void dispatch(final Channel channel, ProviderInvoker<?> invoker, Request request) {
            if (request.getMeta().isOneWay()) {
                try {
                    invoke(invoker, request).whenComplete((r, t) ->
                            printException(request, t, "onewayInvoke exception"));
                } catch (Throwable ex) {
                    printException(request, ex, "onewayInvoke exception");
                }
            } else {
                normalInvoke(channel, invoker, request);
            }
        }

        private CompletionStage<Response> invoke(ProviderInvoker<?> invoker, Request request) {
            return invoker.invoke(request);
        }

        private void normalInvoke(Channel channel, ProviderInvoker<?> invoker, Request request) {
            try {
                CompletionStage<Response> future = invoke(invoker, request);
                future.whenComplete((response, t) -> {
                    try {
                        t = ExceptionHelper.unwrapCompletionException(t);
                        response = (t != null) ? RpcUtils.newResponse(request, null, t) : response;
                        reply(channel, request, response);
                    } catch (Throwable e) {
                        printException(request, e, "normalInvoke exception");
                    }
                });
            } catch (Throwable e) {
                errorReply(channel, request, e);
                printException(request, e, "normalInvoke exception");
            }
        }

        /**
         * Write response to client
         */
        private void reply(Channel channel, Request request, Response response) {
            if (channel.isConnected()) {
                if (response != null) {
                    if (response.getException() != null) {
                        printException(request, response.getException(), "response has exception");
                    }
                    channel.send(response).whenComplete((rx, tx) -> {
                        if (tx != null) {
                            printException(request, tx, "sendResponse exception");
                        }
                    });
                } else {
                    LOG.error("Found rpcServiceName={}, rpcMethodName={}, return value is <null>",
                            request.getInvocation().getRpcServiceName(),
                            request.getInvocation().getRpcMethodName());
                }
            } else {
                LOG.error(
                        "Request{" + requestToString(request) + "} reply error, channel={" + channel
                                + "} is close or disconnect");
            }
        }

        private void errorReply(Channel channel, Request request, Throwable ex) {
            if (ex instanceof TRpcException) {
                TRpcException x = (TRpcException) ex;
                errorReply(channel, request, x.getCode(), x.getBizCode(), x.getMessage());
            } else {
                errorReply(channel, request, ErrorCode.TRPC_INVOKE_UNKNOWN_ERR, 0, ex.getMessage());
            }
        }

        private void errorReply(Channel channel, Request request, int errorCode, int bizCode,
                                String msg) {
            Response response =
                    RpcUtils.newResponse(request, null,
                            TRpcException.newException(errorCode, bizCode, msg));
            if (channel.isConnected()) {
                channel.send(response).whenComplete((rx, tx) -> {
                    if (tx != null) {
                        printException(request, tx, "sendResponse exception");
                    }
                });
            } else {
                LOG.error("Request{" + requestToString(request) + "} reply error, channel={" + channel
                        + "} is close or disconnect");
            }
        }

        private void printException(Request r, Throwable e, String info) {
            if (e != null && !ExceptionHelper.isBizException(e)) {
                LOG.error("Request={},info={}, error:", requestToString(r), info, e);
            }
        }

        private String requestToString(Request r) {
            return r == null ? "<null>" : "{requestId=" + r.getRequestId() + ", reqHead={" + r.getAttachReqHead()
                    + "}, createTime=" + r.getMeta().getCreateTime() + "}";
        }
    }
}

