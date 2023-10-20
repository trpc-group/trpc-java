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

package com.tencent.trpc.proto.http.client;


import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.AbstractRpcClient;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import java.io.IOException;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;

/**
 * Http2c protocol client.
 */
public class Http2cRpcClient extends AbstractRpcClient {

    private static final Logger logger = LoggerFactory.getLogger(HttpRpcClient.class);

    /**
     * Asynchronous HTTP client
     */
    protected CloseableHttpAsyncClient httpAsyncClient;

    public Http2cRpcClient(ProtocolConfig config) {
        setConfig(config);
    }

    /**
     * Configure and start the client
     */
    @Override
    protected void doOpen() {
        httpAsyncClient = HttpAsyncClients.customHttp2().build();
        httpAsyncClient.start();
    }

    /**
     * Close the client
     */
    @Override
    protected void doClose() {
        if (httpAsyncClient != null) {
            try {
                httpAsyncClient.close();
            } catch (IOException e) {
                logger.error("close httpClient of " + protocolConfig.getIp() + ":"
                        + protocolConfig.getPort() + " failed", e);
            }
        }
    }

    /**
     * Generate an invoker and hand it over to the proxy to generate a proxy object.
     * The chain processing of the invoker is wrapped outside.
     *
     * @param consumerConfig the configuration related to the interface set by the method invoker,
     * such as timeout duration, filter configuration, etc.
     */
    @Override
    public <T> ConsumerInvoker<T> createInvoker(ConsumerConfig<T> consumerConfig) {
        return new Http2ConsumerInvoker<>(this, consumerConfig, protocolConfig);
    }

    public CloseableHttpAsyncClient getHttpAsyncClient() {
        return httpAsyncClient;
    }
}
