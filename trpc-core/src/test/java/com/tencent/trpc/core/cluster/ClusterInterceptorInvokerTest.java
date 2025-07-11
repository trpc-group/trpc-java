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

package com.tencent.trpc.core.cluster;

import com.tencent.trpc.core.cluster.def.DefClusterInvoker;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.core.utils.FutureUtils;
import java.util.concurrent.CompletionStage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ClusterInterceptorInvokerTest {

    private ClusterInvoker<BlankRpcServiceName> clusterInvoker;
    private ClusterInvoker<BlankRpcServiceName> clusterInvokerNoInterceptors;

    /**
     * Init clusterInvoker & clusterInvokerNoInterceptors
     */
    @Before
    public void setUp() {
        ConsumerConfig<BlankRpcServiceName> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setMock(false);
        consumerConfig.setServiceInterface(BlankRpcServiceName.class);
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:8080");
        backendConfig.setServiceInterface(BlankRpcServiceName.class);
        backendConfig.setDefault();
        backendConfig.getInterceptors().add("log");
        consumerConfig.setBackendConfig(backendConfig);
        clusterInvoker = new ClusterInvoker<BlankRpcServiceName>() {
            @Override
            public Class<BlankRpcServiceName> getInterface() {
                return BlankRpcServiceName.class;
            }

            @Override
            public ConsumerConfig<BlankRpcServiceName> getConfig() {
                return consumerConfig;
            }

            @Override
            public BackendConfig getBackendConfig() {
                return backendConfig;
            }

            @Override
            public CompletionStage<Response> invoke(Request request) {
                return FutureUtils.newSuccessFuture(new DefResponse());
            }
        };
        ConsumerConfig<BlankRpcServiceName> consumerConfigNoInterceptor = new ConsumerConfig<>();
        consumerConfigNoInterceptor.setMock(false);
        consumerConfigNoInterceptor.setServiceInterface(BlankRpcServiceName.class);
        BackendConfig backendConfigNoInterceptor = new BackendConfig();
        backendConfigNoInterceptor.setNamingUrl("ip://127.0.0.1:8080");
        backendConfigNoInterceptor.setServiceInterface(BlankRpcServiceName.class);
        backendConfigNoInterceptor.setDefault();
        consumerConfigNoInterceptor.setBackendConfig(backendConfigNoInterceptor);
        clusterInvokerNoInterceptors = new DefClusterInvoker<>(consumerConfigNoInterceptor);
    }

    @Test
    public void testNew() {
        ClusterInterceptorInvoker<BlankRpcServiceName> invoker = new ClusterInterceptorInvoker<>(clusterInvoker);
        ConsumerConfig<BlankRpcServiceName> config = invoker.getConfig();
        Assert.assertNotNull(config);
        Assert.assertFalse(config.getMock());
        Assert.assertTrue(invoker.getInterface().isAssignableFrom(BlankRpcServiceName.class));
        Assert.assertNotNull(invoker.getBackendConfig());
        Assert.assertEquals(invoker.getBackendConfig().getInterceptors().get(0), "log");
        Assert.assertNotNull(invoker.invoke(new DefRequest()));
    }

    @Test
    public void testNewNoInterceptor() {
        ClusterInterceptorInvoker<BlankRpcServiceName> invokerWithoutInterceptor = new ClusterInterceptorInvoker<>(
                clusterInvokerNoInterceptors);
        Assert.assertNotNull(invokerWithoutInterceptor);
        Assert.assertTrue(invokerWithoutInterceptor.getBackendConfig().getInterceptors().isEmpty());
    }

    @TRpcService
    public interface BlankRpcServiceName {

    }

}
