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

package com.tencent.trpc.core.rpc.def;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.core.rpc.common.RpcMethodInfoAndInvoker;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by youngwwang on 2020/5/20.
 */
public class DefMethodInfoRegisterTest {

    private static final DefMethodInfoRegister DEF_METHOD_INFO_REGISTER =
            new DefMethodInfoRegister();

    private static final ProviderInvoker<Hello> INVOKER = buildProviderInvoker(null);
    private static final ProviderInvoker<Hello> INVOKER_WITH_BASEPATH = buildProviderInvoker("/hello");

    private static ProviderInvoker<Hello> buildProviderInvoker(String basePath) {
        ProviderConfig<Hello> objectProviderConfig = new ProviderConfig<>();
        objectProviderConfig.setServiceInterface(Hello.class);
        objectProviderConfig.setRef(new HelloImpl());
        ServiceConfig serviceConfig = new ServiceConfig();
        objectProviderConfig.setServiceConfig(serviceConfig);
        if (basePath != null) {
            serviceConfig.setBasePath(basePath);
        }
        ProtocolConfig protocolConfig = new ProtocolConfig();
        return new DefProviderInvoker<>(protocolConfig, objectProviderConfig);
    }

    @Test
    public void testRegister() {
        DEF_METHOD_INFO_REGISTER.register(INVOKER);
        RpcMethodInfoAndInvoker sayHello = DEF_METHOD_INFO_REGISTER
                .route("trpc.test.rpc.Hello", "sayHello");
        Assert.assertNotNull(sayHello);
        sayHello = DEF_METHOD_INFO_REGISTER.route("/trpc_test1_rpc/sayHello");
        Assert.assertNotNull(sayHello);
        Assert.assertEquals(sayHello.getInvoker(), INVOKER);

        DEF_METHOD_INFO_REGISTER.unregister(INVOKER.getConfig());
        sayHello = DEF_METHOD_INFO_REGISTER
                .route("trpc.test.rpc.Hello", "sayHello");
        Assert.assertNull(sayHello);

        DEF_METHOD_INFO_REGISTER.register(INVOKER_WITH_BASEPATH);
        RpcMethodInfoAndInvoker sayHelloWithBasePath = DEF_METHOD_INFO_REGISTER
                .route("trpc.test.rpc.Hello", "sayHello");
        Assert.assertEquals(sayHelloWithBasePath.getInvoker().getConfig().getServiceConfig().getBasePath(), "/hello");
        Assert.assertTrue(DEF_METHOD_INFO_REGISTER.validateNativeHttpPath("/hello"));
        Assert.assertEquals(DEF_METHOD_INFO_REGISTER.getNativeHttpFunc("/hello"), "/hello");
        Assert.assertEquals(sayHelloWithBasePath.getMethodRouterKey().getSlashFunc(), "/trpc/test/rpc/Hello/sayHello");
        DEF_METHOD_INFO_REGISTER.unregister(INVOKER_WITH_BASEPATH.getConfig());
    }

    @Test
    public void testDefaultRegister() {
        DEF_METHOD_INFO_REGISTER.register(INVOKER);
        RpcMethodInfoAndInvoker defaultMethod = DEF_METHOD_INFO_REGISTER
                .getDefaultRouter("trpc.test.rpc.Hello");
        Assert.assertNotNull(defaultMethod);

        DEF_METHOD_INFO_REGISTER.unregister(INVOKER.getConfig());
        defaultMethod = DEF_METHOD_INFO_REGISTER.getDefaultRouter("trpc.test.rpc.Hello");
        Assert.assertNull(defaultMethod);
    }

    @Test
    public void testClear() {
        DEF_METHOD_INFO_REGISTER.register(INVOKER);
        RpcMethodInfoAndInvoker sayHello = DEF_METHOD_INFO_REGISTER
                .route("trpc.test.rpc.Hello", "sayHello");
        Assert.assertNotNull(sayHello);
        DEF_METHOD_INFO_REGISTER.clear();
        sayHello = DEF_METHOD_INFO_REGISTER
                .route("trpc.test.rpc.Hello", "sayHello");
        Assert.assertNull(sayHello);
        sayHello = DEF_METHOD_INFO_REGISTER.route("/trpc_test1_rpc/sayHello");
        Assert.assertNull(sayHello);
    }


    @TRpcService(name = "trpc.test.rpc.Hello")
    public interface Hello {

        @TRpcMethod(name = "sayHello", alias = {"/trpc_test1_rpc/sayHello"})
        String sayHello();

        String sayHi();

        @TRpcMethod(name = "", isDefault = true)
        String defaultMethod();
    }

    public static class HelloImpl implements Hello {

        @Override
        public String sayHello() {
            return "hello";
        }

        @Override
        public String sayHi() {
            return null;
        }

        @Override
        public String defaultMethod() {
            return "default";
        }
    }
}