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

import static org.mockito.Mockito.when;

import com.tencent.trpc.core.cluster.AbstractClusterInvocationHandler;
import com.tencent.trpc.core.cluster.ClusterInvoker;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.CloseFuture;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClient;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcContextValueKeys;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.core.rpc.def.LeftTimeout;
import com.tencent.trpc.core.rpc.def.LinkInvokeTimeout;
import com.tencent.trpc.core.rpc.spi.RpcClientFactory;
import com.tencent.trpc.core.utils.FutureUtils;
import com.tencent.trpc.core.utils.RpcContextUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ClusterInvoker.class})
public class DefClusterInvocationHandlerTest {

    private ClusterInvoker<BlankRpcServiceName> invoker;

    /**
     * Init invoker
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
        consumerConfig.setBackendConfig(backendConfig);
        invoker = new ClusterInvoker<BlankRpcServiceName>() {
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
    }

    @Test
    public void testParseLeftTime() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        ClusterInvoker invoker = Mockito.mock(ClusterInvoker.class);
        ConsumerConfig consumerConfig = new ConsumerConfig();
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setRequestTimeout(1000);
        consumerConfig.setBackendConfig(backendConfig);
        when(invoker.getConfig()).thenReturn(consumerConfig);
        DefClusterInvocationHandler defClusterInvocationHandler = new DefClusterInvocationHandler(
                invoker);
        RpcClientContext rpcClientContext = new RpcClientContext();
        Method method = DefClusterInvocationHandler.class
                .getDeclaredMethod("parseLeftTime", RpcClientContext.class, String.class);
        method.setAccessible(true);
        LeftTimeout timeout = (LeftTimeout) method
                .invoke(defClusterInvocationHandler, rpcClientContext, "test");
        Assert.assertEquals(1000, timeout.getLeftTimeout());
        Assert.assertEquals(1000, timeout.getOriginTimeout());
        rpcClientContext.setTimeoutMills(2000);
        timeout = (LeftTimeout) method
                .invoke(defClusterInvocationHandler, rpcClientContext, "test");
        Assert.assertEquals(2000, timeout.getLeftTimeout());
        Assert.assertEquals(2000, timeout.getOriginTimeout());
        ConfigManager.getInstance().setDefault();
        RpcContextUtils.putValueMapValue(rpcClientContext, RpcContextValueKeys.CTX_LINK_INVOKE_TIMEOUT,
                LinkInvokeTimeout.builder().startTime(System.currentTimeMillis() - 1)
                        .timeout(2000)
                        .leftTimeout(500)
                        .serviceEnableLinkTimeout(false).build());
        timeout = (LeftTimeout) method
                .invoke(defClusterInvocationHandler, rpcClientContext, "test");
        Assert.assertEquals(2000, timeout.getLeftTimeout());
        Assert.assertEquals(2000, timeout.getOriginTimeout());
        // If the full-link timeout is enabled and the service itself has already taken 9500ms,
        // the remaining time will be based on the remaining time when it is less than the calling setting time.
        RpcContextUtils.putValueMapValue(rpcClientContext, RpcContextValueKeys.CTX_LINK_INVOKE_TIMEOUT,
                LinkInvokeTimeout.builder().startTime(System.currentTimeMillis() - 9500)
                        .timeout(10000)
                        .leftTimeout(10000)
                        .serviceEnableLinkTimeout(true).build());
        ConfigManager.getInstance().setDefault();
        timeout = (LeftTimeout) method
                .invoke(defClusterInvocationHandler, rpcClientContext, "test");
        // left time ms <= 10000 - 9500
        Assert.assertTrue(timeout.getLeftTimeout() > 0);
        Assert.assertTrue(timeout.getLeftTimeout() <= 500);
        // init time Math.min(10000,2000)
        Assert.assertEquals(2000, timeout.getOriginTimeout());
        // If the full-link timeout is enabled and the service itself has taken 100ms,
        // the remaining time will be based on the calling setting time when it is greater than the remaining time.
        RpcContextUtils.putValueMapValue(rpcClientContext, RpcContextValueKeys.CTX_LINK_INVOKE_TIMEOUT,
                LinkInvokeTimeout.builder().startTime(System.currentTimeMillis() - 100)
                        .timeout(10000)
                        .leftTimeout(3000)
                        .serviceEnableLinkTimeout(true).build());
        ConfigManager.getInstance().setDefault();
        timeout = (LeftTimeout) method
                .invoke(defClusterInvocationHandler, rpcClientContext, "test");
        //left time Math.min(3000-100,2000)
        Assert.assertEquals(2000, timeout.getLeftTimeout());
        //init time Math.min(10000,2000)
        Assert.assertEquals(2000, timeout.getOriginTimeout());

        // If the time in the context is null and the client has not configured a timeout,
        // the full-link timeout will be used as the standard.
        rpcClientContext = new RpcClientContext();
        consumerConfig = new ConsumerConfig();
        backendConfig = new BackendConfig();
        backendConfig.setRequestTimeout(0);
        consumerConfig.setBackendConfig(backendConfig);
        when(invoker.getConfig()).thenReturn(consumerConfig);
        defClusterInvocationHandler = new DefClusterInvocationHandler(invoker);
        RpcContextUtils.putValueMapValue(rpcClientContext, RpcContextValueKeys.CTX_LINK_INVOKE_TIMEOUT,
                LinkInvokeTimeout.builder().startTime(System.currentTimeMillis() - 1)
                        .timeout(3000)
                        .leftTimeout(3000)
                        .serviceEnableLinkTimeout(true).build());
        timeout = (LeftTimeout) method
                .invoke(defClusterInvocationHandler, rpcClientContext, "test");
        Assert.assertEquals(3000, timeout.getOriginTimeout());
        Assert.assertTrue(timeout.getLeftTimeout() <= 2999);
    }

    @Test
    public void testParseRpcServiceName() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        DefClusterInvocationHandler defClusterInvocationHandler = new DefClusterInvocationHandler(invoker);
        Method method = AbstractClusterInvocationHandler.class
                .getDeclaredMethod("parseRpcServiceName", Class.class, Object.class);
        method.setAccessible(true);
        RpcClientContext rpcClientContext = new RpcClientContext();
        String rpcServiceName = (String) method
                .invoke(defClusterInvocationHandler, BlankRpcServiceName.class, rpcClientContext);
        Assert.assertEquals(rpcServiceName, "blank");
    }

    @Test
    public void testInvokeLocal() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        DefClusterInvocationHandler defClusterInvocationHandler = new DefClusterInvocationHandler(invoker);
        Method method = AbstractClusterInvocationHandler.class.getDeclaredMethod("invokeLocal",
                Method.class, Object[].class);
        method.setAccessible(true);
        String toString = (String) method.invoke(defClusterInvocationHandler,
                defClusterInvocationHandler.getClass().getMethod("toString"), null);
        Assert.assertTrue(toString.contains("com.tencent.trpc.core.cluster.def.DefClusterInvocationHandler"));
        Integer hashCode = (Integer) method.invoke(defClusterInvocationHandler,
                defClusterInvocationHandler.getClass().getMethod("hashCode"), null);
        Assert.assertNotNull(hashCode);
        boolean equal = (Boolean) method.invoke(defClusterInvocationHandler,
                defClusterInvocationHandler.getClass().getMethod("equals", Object.class), new String[]{"a"});
        Assert.assertFalse(equal);
        try {
            method.invoke(defClusterInvocationHandler, AbstractClusterInvocationHandler.class.getDeclaredMethod(
                    "isLocalMethod", Method.class), null);
        } catch (InvocationTargetException ex) {
            Throwable targetException = ex.getTargetException();
            Assert.assertTrue(targetException instanceof TRpcException);
            TRpcException e = (TRpcException) targetException;
            Assert.assertTrue(e.getMessage().contains("not allow invoke local method:isLocalMethod"));
        }
    }

    @Test
    public void testInvoke() throws Throwable {
        RpcClientContext context = new RpcClientContext();
        context.setRpcMethodName("blank");
        context.setRpcServiceName("blank");
        context.setRpcMethodAlias("/blank_blank");
        Object[] args = new Object[]{context};
        Method method = BlankRpcServiceNameImpl.class.getDeclaredMethod("blank", RpcContext.class);
        BlankRpcServiceNameImpl blankRpcServiceName = new BlankRpcServiceNameImpl();
        DefClusterInvocationHandler clusterInvocationHandler = new DefClusterInvocationHandler(invoker);
        clusterInvocationHandler.invoke(blankRpcServiceName, method, args);
    }

    @TRpcService(name = "blank")
    public interface BlankRpcServiceName {

        @TRpcMethod(name = "blank", alias = {"/blank_blank"})
        String blank(RpcContext context);

        @TRpcMethod(name = "throwing")
        String throwing(RpcContext context);
    }

    public static class RpcClientFactoryImpl implements RpcClientFactory {

        @Override
        public RpcClient createRpcClient(ProtocolConfig config) throws TRpcException {
            return new RpcClient() {
                @Override
                public void open() throws TRpcException {

                }

                @Override
                public <T> ConsumerInvoker<T> createInvoker(ConsumerConfig<T> consumerConfig) {
                    return new ConsumerInvoker<T>() {
                        @Override
                        public ConsumerConfig<T> getConfig() {
                            return null;
                        }

                        @Override
                        public ProtocolConfig getProtocolConfig() {
                            return null;
                        }

                        @Override
                        public Class<T> getInterface() {
                            return null;
                        }

                        @Override
                        public CompletionStage<Response> invoke(Request request) {
                            return FutureUtils.newSuccessFuture(new DefResponse());
                        }
                    };
                }

                @Override
                public void close() {
                }

                @Override
                public CloseFuture<Void> closeFuture() {
                    return new CloseFuture<>();
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
            };
        }
    }

    public static class BlankRpcServiceNameImpl implements BlankRpcServiceName {

        @Override
        public String blank(RpcContext context) {
            return "blank";
        }

        @Override
        public String throwing(RpcContext context) {
            throw new IllegalStateException("boom");
        }
    }
}
