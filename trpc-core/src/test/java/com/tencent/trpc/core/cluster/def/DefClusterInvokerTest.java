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

package com.tencent.trpc.core.cluster.def;

import com.tencent.trpc.core.cluster.def.DefClusterInvoker.ConsumerInvokerProxy;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.NamingOptions;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.proxy.support.ByteBuddyProxyFactory;
import com.tencent.trpc.core.rpc.CloseFuture;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.rpc.GenericClient;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClient;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.utils.FutureUtils;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import com.tencent.trpc.core.worker.handler.TrpcThreadExceptionHandler;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import com.tencent.trpc.core.worker.support.thread.ThreadWorkerPool;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({WorkerPoolManager.class})
@PowerMockIgnore({"javax.management.*", "javax.security.*", "javax.ws.*"})
public class DefClusterInvokerTest {

    private DefClusterInvoker<GenericClient> defClusterInvoker;
    private ConsumerInvokerProxy<GenericClient> consumerInvokerProxy;

    /**
     * Create ConsumerConfig & create DefClusterInvoker & create ConsumerInvokerProxy
     */
    @Before
    public void setUp() {
        ConsumerConfig<GenericClient> consumerConfig = getConsumerConfig();
        this.defClusterInvoker = new DefClusterInvoker<>(consumerConfig);
        consumerInvokerProxy = new ConsumerInvokerProxy<>(new ConsumerInvoker<GenericClient>() {
            @Override
            public ConsumerConfig<GenericClient> getConfig() {
                return consumerConfig;
            }

            @Override
            public ProtocolConfig getProtocolConfig() {
                return null;
            }

            @Override
            public Class<GenericClient> getInterface() {
                return GenericClient.class;
            }

            @Override
            public CompletionStage<Response> invoke(Request request) {
                if (Objects.equals(request.getInvocation().getFunc(), "a")) {
                    return FutureUtils.newSuccessFuture(new DefResponse());
                }
                return FutureUtils.failed(new IllegalAccessException());
            }
        }, new RpcClient() {
            @Override
            public void open() throws TRpcException {

            }

            @Override
            public <T> ConsumerInvoker<T> createInvoker(ConsumerConfig<T> consumerConfig) {
                return null;
            }

            @Override
            public void close() {
            }

            @Override
            public CloseFuture<Void> closeFuture() {
                return null;
            }

            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public boolean isClosed() {
                return false;
            }

            @Override
            public ProtocolConfig getProtocolConfig() {
                return null;
            }
        });
    }

    private ConsumerConfig<GenericClient> getConsumerConfig() {
        BackendConfig backendConfig = PowerMockito.mock(BackendConfig.class);
        backendConfig.setServiceInterface(GenericClient.class);
        backendConfig.setProxyType(ByteBuddyProxyFactory.NAME);
        backendConfig.setNamingUrl("ip://127.0.0.1:12345");
        backendConfig.setName("GenericClient");
        backendConfig.setNamespace("development");
        backendConfig.setDefault();
        NamingOptions options = new NamingOptions();
        options.setSelectorId("ip");
        options.setServiceNaming("127.0.0.1:12345");
        PowerMockito.when(backendConfig.getNamingOptions()).thenReturn(options);
        // mock BackendConfig.getProxyType
        PowerMockito.when(backendConfig.getProxyType()).thenReturn(ByteBuddyProxyFactory.NAME);
        // mock workerPoolObj
        WorkerPool workerPool = PowerMockito.mock(ThreadWorkerPool.class);
        AtomicLong atomicLong = new AtomicLong();
        PowerMockito.when(workerPool.getUncaughtExceptionHandler())
                .thenReturn(new TrpcThreadExceptionHandler(atomicLong, atomicLong, atomicLong));
        PowerMockito.when(backendConfig.getWorkerPoolObj()).thenReturn(workerPool);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        PowerMockito.when(workerPool.toExecutor()).thenReturn(executor);
        backendConfig.setWorkerPool(WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_NAME);
        ConsumerConfig<GenericClient> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setBackendConfig(backendConfig);
        consumerConfig.setServiceInterface(GenericClient.class);
        return consumerConfig;
    }

    @Test
    public void testTestToString() {
        Assert.assertTrue(defClusterInvoker.toString().contains("NamingClusterInvoker"));
    }

    @Test
    public void testGetConfig() {
        ConsumerConfig<GenericClient> config = defClusterInvoker.getConfig();
        Assert.assertEquals(ByteBuddyProxyFactory.NAME, config.getBackendConfig().getProxyType());
    }

    @Test
    public void testGetInterface() {
        Class<GenericClient> anInterface = defClusterInvoker.getInterface();
        Assert.assertNotNull(anInterface);
    }

    @Test
    public void testGetBackendConfig() {
        BackendConfig backendConfig = defClusterInvoker.getBackendConfig();
        Assert.assertEquals(ByteBuddyProxyFactory.NAME, backendConfig.getProxyType());
    }

    @Test
    public void testProxyInvoke() {
        DefRequest defRequest = new DefRequest();
        RpcInvocation invocation = new RpcInvocation();
        invocation.setFunc("a");
        defRequest.setInvocation(invocation);
        consumerInvokerProxy.invoke(defRequest, new ServiceInstance());
        invocation.setFunc("n");
        consumerInvokerProxy.invoke(defRequest, new ServiceInstance());
        Assert.assertNotNull(consumerInvokerProxy.getInvoker());
    }

    @Test
    public void testDoInvoke() {
        DefRequest defRequest = new DefRequest();
        CompletionStage<ServiceInstance> instance = CompletableFuture.completedFuture(null);
        instance.thenApply((ins) -> null);
        try {
            defClusterInvoker.doInvoke(defRequest, instance).toCompletableFuture().join();
        } catch (CompletionException exception) {
            ConsumerConfig<GenericClient> consumerConfig = defClusterInvoker.getConfig();
            String expect = "com.tencent.trpc.core.exception.TRpcException: Service(name="
                    + consumerConfig.getServiceInterface().getName()
                    + ", naming=" + consumerConfig.getBackendConfig().getNamingOptions().getServiceNaming()
                    + "), Client router error [found no available instance]";
            Assert.assertEquals(expect, exception.getMessage());
        }
    }

}
