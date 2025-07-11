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

package com.tencent.trpc.validation.pgv;

import com.google.common.collect.Lists;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.rpc.GenericClient;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.validation.pgv.TestValidator.TestBean;
import com.tencent.trpc.validation.pgv.TestValidator.TestRequest;
import com.tencent.trpc.validation.pgv.TestValidator.TestResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Pgv Filter test class.
 */
public class PgvValidationFilterTest {

    @Before
    public void before() {
        ConfigManager.stopTest();
        ConfigManager.startTest();
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("validators", Lists.newArrayList(
                "com.tencent.trpc.validation.pgv.TestInvoker",
                "com.tencent.trpc.validation.pgv.TestValidator"));
        config.put("response_validation", true);
        PluginConfig pluginConfig = new PluginConfig("pgv", Filter.class,
                PgvValidationFilter.class, config);
        PluginConfig testValidatorsPgv = new PluginConfig("test_validators_pgv", Filter.class,
                PgvValidationFilter.class, new LinkedHashMap<String, Object>() {
            {
                put("validators", Lists.newArrayList());
                put("response_validation", true);
            }
        });
        PluginConfig testNotFound = new PluginConfig("test_not_found_pgv", Filter.class,
                PgvValidationFilter.class, new LinkedHashMap<String, Object>() {
            {
                put("validators", Lists.newArrayList("com.tencent.trpc.validation.pgv.NotFound"));
                put("response_validation", true);
            }
        });
        ExtensionLoader.registerPlugin(pluginConfig);
        ExtensionLoader.registerPlugin(testValidatorsPgv);
        ExtensionLoader.registerPlugin(testNotFound);
    }

    @After
    public void after() {
        ConfigManager.stopTest();
    }

    /**
     * Test validation passes.
     *
     * @throws NoSuchMethodException if the method does not exist
     * @throws InterruptedException if the execution is interrupted
     * @throws ExecutionException if the computation threw an exception
     */
    @Test
    public void testPgvValidationFilter() throws NoSuchMethodException, InterruptedException, ExecutionException {
        ExtensionLoader<?> extensionLoader = ExtensionLoader.getExtensionLoader(Filter.class);
        PgvValidationFilter filter = (PgvValidationFilter) extensionLoader.getExtension("pgv");
        Assert.assertNotNull(filter);
        TestRequest request = new TestRequest();
        request.setVar1("111111111");
        TestBean bean = new TestBean();
        bean.setVar1("333333");
        request.setVar3(bean);
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setArguments(new Object[]{request, "b"});
        rpcInvocation.setRpcMethodInfo(new RpcMethodInfo(GenericClient.class,
                GenericClient.class.getMethod("invoke", RpcClientContext.class, byte[].class)));
        DefRequest defRequest = new DefRequest();
        defRequest.setInvocation(rpcInvocation);
        CompletionStage<Response> response = filter.filter(new TestInvoker(), defRequest);
        CompletableFuture resultFuture = response.toCompletableFuture();
        Assert.assertNull(((Response) resultFuture.get()).getException());
    }

    /**
     * Test validation request parameters do not pass, hit validator cache.
     *
     * @throws NoSuchMethodException if the method does not exist
     * @throws InterruptedException if the execution is interrupted
     * @throws ExecutionException if the computation threw an exception
     */
    @Test
    public void testPgvValidationFilter1() throws NoSuchMethodException, InterruptedException, ExecutionException {
        ExtensionLoader<?> extensionLoader = ExtensionLoader.getExtensionLoader(Filter.class);
        PgvValidationFilter filter = (PgvValidationFilter) extensionLoader.getExtension("pgv");
        Assert.assertNotNull(filter);
        TestRequest request = new TestRequest();
        request.setVar1("1111111111111");
        TestBean bean = new TestBean();
        bean.setVar1("333333");
        request.setVar3(bean);
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setArguments(new Object[]{request, "b"});
        rpcInvocation.setRpcMethodInfo(new RpcMethodInfo(GenericClient.class,
                GenericClient.class.getMethod("invoke", RpcClientContext.class, byte[].class)));
        DefRequest defRequest = new DefRequest();
        defRequest.setInvocation(rpcInvocation);
        CompletionStage<Response> response = filter.filter(new TestInvoker(), defRequest);
        CompletableFuture resultFuture = response.toCompletableFuture();
        try {
            resultFuture.get();
        } catch (Exception e) {
            Assert.assertEquals(ExecutionException.class, e.getClass());
        }
        CompletionStage<Response> response1 = filter.filter(new TestInvoker(), defRequest);
        CompletableFuture resultFuture1 = response1.toCompletableFuture();
        try {
            resultFuture1.get();
        } catch (Exception e) {
            Assert.assertEquals(ExecutionException.class, e.getClass());
        }
    }

    /**
     * Test validation response does not pass.
     *
     * @throws NoSuchMethodException if the method does not exist
     * @throws InterruptedException if the execution is interrupted
     * @throws ExecutionException if the computation threw an exception
     */
    @Test
    public void testPgvValidationFilter2() throws NoSuchMethodException, InterruptedException, ExecutionException {
        ExtensionLoader<?> extensionLoader = ExtensionLoader.getExtensionLoader(Filter.class);
        PgvValidationFilter filter = (PgvValidationFilter) extensionLoader.getExtension("pgv");
        Assert.assertNotNull(filter);
        TestRequest request = new TestRequest();
        request.setVar1("111111111");
        TestBean bean = new TestBean();
        bean.setVar1("333333");
        request.setVar3(bean);
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setArguments(new Object[]{request, "b"});
        rpcInvocation.setRpcMethodInfo(new RpcMethodInfo(GenericClient.class,
                GenericClient.class.getMethod("invoke", RpcClientContext.class, byte[].class)));
        DefRequest defRequest = new DefRequest();
        defRequest.setInvocation(rpcInvocation);
        CompletionStage<Response> response = filter.filter(new Invoker() {
            @Override
            public Class getInterface() {
                return null;
            }

            @Override
            public CompletionStage<Response> invoke(Request request) {
                DefResponse defResponse = new DefResponse();
                TestResponse response = new TestResponse();
                response.setVar1("222222222222");
                defResponse.setValue(response);
                return CompletableFuture.completedFuture(defResponse);
            }
        }, defRequest);
        CompletableFuture resultFuture = response.toCompletableFuture();
        Assert.assertNotNull(((Response) resultFuture.get()).getException());
    }

    /**
     * Test that parameters do not need to be validated.
     *
     * @throws NoSuchMethodException if the method does not exist
     * @throws InterruptedException if the execution is interrupted
     * @throws ExecutionException if the computation threw an exception
     */
    @Test
    public void testPgvValidationFilter3() throws NoSuchMethodException, InterruptedException, ExecutionException {
        ExtensionLoader<?> extensionLoader = ExtensionLoader.getExtensionLoader(Filter.class);
        PgvValidationFilter filter = (PgvValidationFilter) extensionLoader.getExtension("pgv");
        Assert.assertNotNull(filter);
        TestInvoker request = new TestInvoker();
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setArguments(new Object[]{request, "testValue"});
        rpcInvocation.setRpcMethodInfo(new RpcMethodInfo(GenericClient.class,
                GenericClient.class.getMethod("invoke", RpcClientContext.class, byte[].class)));
        DefRequest defRequest = new DefRequest();
        defRequest.setInvocation(rpcInvocation);
        CompletionStage<Response> response = filter.filter(new TestInvoker(), defRequest);
        CompletableFuture resultFuture = response.toCompletableFuture();
        Assert.assertNull(((Response) resultFuture.get()).getException());
    }

    @Test
    public void testValidatorsPgv() throws NoSuchMethodException, InterruptedException, ExecutionException {
        ExtensionLoader<?> extensionLoader = ExtensionLoader.getExtensionLoader(Filter.class);
        PgvValidationFilter filter = (PgvValidationFilter) extensionLoader.getExtension("test_validators_pgv");
        Assert.assertNotNull(filter);
    }

    @Test
    public void testNotFound() {
        ExtensionLoader<?> extensionLoader = ExtensionLoader.getExtensionLoader(Filter.class);
        Assert.assertThrows(TRpcExtensionException.class,
                () -> {
                    PgvValidationFilter filter = (PgvValidationFilter) extensionLoader.
                            getExtension("test_not_found_pgv");
                });
    }

}
