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

package com.tencent.trpc.proto.support;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.AbstractRpcClient;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClient;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.rpc.def.DecodableValue;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.ClientTransport;
import com.tencent.trpc.core.transport.codec.ClientCodec;
import com.tencent.trpc.core.transport.handler.ChannelHandlerAdapter;
import com.tencent.trpc.core.transport.spi.ClientTransportFactory;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

/**
 * Default implementation of {@link RpcClient}
 *
 * @see AbstractRpcClient
 * @see RpcClient
 */
public class DefRpcClient extends AbstractRpcClient {

    private static final Logger LOG = LoggerFactory.getLogger(DefRpcClient.class);
    /**
     * ClientTransport
     */
    private final ClientTransport transport;
    /**
     * InternalHandler
     */
    private final InternalHandler handler;
    /**
     * Map of rpc interface to {@link ConsumerInvoker}
     */
    private final ConcurrentMap<Class<?>, ConsumerInvoker<?>> invokerMap = Maps.newConcurrentMap();
    /**
     * Manager for response futures
     */
    private final DefResponseFutureManager futureManager = new DefResponseFutureManager();
    /**
     * ClientCodec
     */
    private final ClientCodec clientCodec;

    public DefRpcClient(ProtocolConfig config, ClientCodec clientCodec) throws TRpcException {
        Objects.requireNonNull(config).init();
        ClientTransportFactory clientFactory = ExtensionLoader
                .getExtensionLoader(ClientTransportFactory.class)
                .getExtension(config.getTransporter());
        this.protocolConfig = config;
        this.handler = new InternalHandler();
        this.transport = clientFactory.create(config, this.handler, clientCodec);
        this.clientCodec = clientCodec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAvailable() {
        return super.isAvailable() && (transport != null && transport.isConnected());
    }

    /**
     * {@inheritDoc}
     *
     * @param consumerConfig client configurations
     * @return created {@link ConsumerInvoker}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> ConsumerInvoker<T> createInvoker(ConsumerConfig<T> consumerConfig) {
        Class<T> serviceType = consumerConfig.getServiceInterface();
        Objects.requireNonNull(serviceType, "serviceType is null");
        if (invokerMap.containsKey(serviceType)) {
            return (ConsumerInvoker<T>) invokerMap.get(serviceType);
        }
        ConsumerInvoker<T> invoker = new DefConsumerInvoker<>(this, consumerConfig);
        invokerMap.putIfAbsent(serviceType, invoker);
        return (ConsumerInvoker<T>) invokerMap.get(serviceType);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doOpen() {
        Objects.requireNonNull(handler, "handler is null");
        Objects.requireNonNull(transport, "client is null");
        transport.open();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doClose() {
        if (transport != null) {
            try {
                transport.close();
            } catch (Exception ex) {
                LOG.error("", ex);
            }
            try {
                futureManager.closeClient(transport);
            } catch (Exception ex) {
                LOG.error("", ex);
            }
        }
        if (handler != null) {
            try {
                handler.destroy();
            } catch (Exception ex) {
                LOG.error("", ex);
            }
        }
    }

    ClientTransport getTransport() {
        return transport;
    }

    DefResponseFutureManager getFutureManager() {
        return futureManager;
    }

    /**
     * Implements {@link com.tencent.trpc.core.transport.ChannelHandler} to handle server response
     */
    private class InternalHandler extends ChannelHandlerAdapter {

        InternalHandler() {
        }

        /**
         * Handles received data from server
         */
        @Override
        public void received(Channel channel, Object message) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(">>>Client receive response:{}", message);
            }
            if (message instanceof List) {
                batchProcess(channel, message);
            } else {
                process(channel, message);
            }
        }

        @Override
        public void destroy() {
            super.destroy();
        }

        private void batchProcess(Channel channel, Object message) {
            ((List<?>) message).forEach(msg -> process(channel, msg));
        }

        private void process(Channel channel, Object message) {
            if (!(message instanceof Response)) {
                if (LOG.isWarnEnabled()) {
                    String config = protocolConfig.toSimpleString();
                    String msgType = (message == null ? "<null>" : message.getClass().getName());
                    LOG.warn(
                            "Client receive unknown message(type={},config={},channel={}), only "
                                    + "support type(class=Response)",
                            msgType, config, channel);
                }
                return;
            }
            handle((Response) message);
        }

        /**
         * Handle response
         */
        private void handle(Response response) {
            DefResponseFuture future = futureManager.remove(response.getRequestId());
            if (future != null && future.getRequest() != null) {
                Request request = future.getRequest();
                try {
                    response.setRequest(request);
                    decodeResponseBody(request, response);
                } catch (Exception ex) {
                    response.setException(
                            TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_DECODE_ERR,
                                    ex.getMessage(), ex));
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug(">>>Client receive response:{}", response);
                }
                try {
                    // complete related ResponseFuture asynchronously
                    future.getInvoker().getConfig().getBackendConfig().getWorkerPoolObj()
                            .execute(() -> {
                                try {
                                    futureManager.complete(future, response);
                                } catch (Throwable ex) {
                                    LOG.error("", ex);
                                }
                            });
                } catch (Throwable ex) {
                    LOG.error("response callback exception, request [" + request + "]", ex);
                }
            }
        }

        private void decodeResponseBody(Request request, Response response) {
            RpcMethodInfo methodInfo = request.getInvocation().getRpcMethodInfo();
            if (response.getValue() instanceof DecodableValue) {
                Type returnTypeClazz = methodInfo.getActualReturnType();
                DecodableValue decodableValue = (DecodableValue) (response.getValue());
                Object decode = decodableValue.decode(returnTypeClazz, request.getInvocation().isGeneric());
                response.setValue(decode);
            } else {
                response.setValue(response.getValue());
            }
            if (clientCodec instanceof ClientResponseBodyCodec) {
                ((ClientResponseBodyCodec) clientCodec).decode(response, request, methodInfo);
            }
        }
    }


}
