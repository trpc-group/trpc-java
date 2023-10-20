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
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * HTTP protocol client.
 */
public class HttpRpcClient extends AbstractRpcClient {

    private static final Logger logger = LoggerFactory.getLogger(HttpRpcClient.class);

    private CloseableHttpClient httpClient;

    public HttpRpcClient(ProtocolConfig config) {
        setConfig(config);
    }

    @Override
    protected void doOpen() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        int maxConns = protocolConfig.getMaxConns();
        // Set the maximum number of connections.
        cm.setMaxTotal(maxConns);
        // If there is only one route, the maximum number of connections for a single route is the same
        // as the maximum number of connections for the entire connection pool.
        cm.setDefaultMaxPerRoute(maxConns);
        httpClient = HttpClients.custom().setConnectionManager(cm).build();
    }

    @Override
    protected void doClose() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                logger.error("close httpClient of " + protocolConfig.getIp() + ":"
                        + protocolConfig.getPort() + " failed", e);
            }
        }
    }

    @Override
    public <T> ConsumerInvoker<T> createInvoker(ConsumerConfig<T> consumerConfig) {
        return new HttpConsumerInvoker<>(this, consumerConfig, protocolConfig);
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }
}
