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

package com.tencent.trpc.spring.exception;

import com.google.protobuf.Message;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.spring.exception.CustomConfigHandleExceptionAnnotationTest.CustomConfigHandleExceptionAnnotationTestConfiguration;
import com.tencent.trpc.spring.exception.TestMsg.Req;
import com.tencent.trpc.spring.exception.TestMsg.Request;
import com.tencent.trpc.spring.exception.TestMsg.Resp;
import com.tencent.trpc.spring.exception.TestMsg.Response;
import com.tencent.trpc.spring.exception.annotation.EnableTRpcHandleException;
import com.tencent.trpc.spring.exception.annotation.TRpcExceptionHandler;
import com.tencent.trpc.spring.exception.annotation.TRpcExceptionHandlerRegistry;
import com.tencent.trpc.spring.exception.annotation.TRpcHandleException;
import com.tencent.trpc.spring.exception.api.ExceptionHandlerResolver;
import com.tencent.trpc.spring.exception.api.ExceptionResultTransformer;
import com.tencent.trpc.spring.exception.api.HandleExceptionConfigurer;
import com.tencent.trpc.spring.test.TestSpringApplication;
import java.lang.reflect.Method;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestSpringApplication.class)
@ContextConfiguration(classes = CustomConfigHandleExceptionAnnotationTestConfiguration.class)
public class CustomConfigHandleExceptionAnnotationTest {

    @Autowired
    private TestServiceApi testServiceApi;

    public static RpcClientContext newContext() {
        return new RpcClientContext();
    }

    @Before
    public void setUp() throws Exception {
        CustomConfigHandleExceptionAnnotationTestConfiguration.STATE = 0;
    }

    /**
     * Test for Exception handling
     */
    @Test
    public void testHandleException() {
        String code = String.valueOf(RandomUtils.nextInt());
        Response response = testServiceApi.test(newContext(), Request.newBuilder().setId("1").setName(code).build());
        Assert.assertEquals("12345", response.getResult());
        Assert.assertEquals("custom", response.getResInfo());
        Assert.assertEquals(1, CustomConfigHandleExceptionAnnotationTestConfiguration.STATE);
    }

    /**
     * Test for NullPointerException handling
     */
    @Test
    public void testHandleNullPointerException() {
        String code = String.valueOf(RandomUtils.nextInt());
        Response response = testServiceApi.test(newContext(), Request.newBuilder().setId("2").setName(code).build());
        Assert.assertEquals("12345", response.getResult());
        Assert.assertEquals("custom", response.getResInfo());
        Assert.assertEquals(1, CustomConfigHandleExceptionAnnotationTestConfiguration.STATE);
    }

    /**
     * Test for IndexOutOfBoundsException handling
     */
    @Test
    public void testHandleIndexOutOfBoundsException() {
        String code = String.valueOf(RandomUtils.nextInt());
        Response response = testServiceApi.test(newContext(), Request.newBuilder().setId("3").setName(code).build());
        Assert.assertEquals("12345", response.getResult());
        Assert.assertEquals("custom", response.getResInfo());
        Assert.assertEquals(1, CustomConfigHandleExceptionAnnotationTestConfiguration.STATE);
    }

    /**
     * Test for IllegalArgumentException handling
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleIllegalArgumentException() {
        String code = String.valueOf(RandomUtils.nextInt());
        Response response = testServiceApi.test(newContext(), Request.newBuilder().setId("4").setName(code).build());
        Assert.fail();
    }

    /**
     * Test for handle result type error
     */
    @Test(expected = ClassCastException.class)
    public void testClassCastException() {
        String code = String.valueOf(RandomUtils.nextInt());
        try {
            Resp response = testServiceApi.call(newContext(), Req.newBuilder().setResult("2").setInfo(code).build());
            Assert.fail();
        } finally {
            Assert.assertEquals(3, CustomConfigHandleExceptionAnnotationTestConfiguration.STATE);
        }
    }

    /**
     * Test handler for specific exception
     */
    @Test
    public void testSpecificExceptionHandler() {
        String code = String.valueOf(RandomUtils.nextInt());
        Resp response = testServiceApi.call(newContext(), Req.newBuilder().setResult("1").setInfo(code).build());
        Assert.assertEquals("666666", response.getRetCode());
        Assert.assertEquals("myHandler", response.getRetMsg());
        Assert.assertEquals(3, CustomConfigHandleExceptionAnnotationTestConfiguration.STATE);
    }

    @Configuration
    @EnableTRpcHandleException
    @TRpcExceptionHandlerRegistry
    public static class CustomConfigHandleExceptionAnnotationTestConfiguration implements HandleExceptionConfigurer {

        private static final Logger logger =
                LoggerFactory.getLogger(CustomConfigHandleExceptionAnnotationTestConfiguration.class);
        public static int STATE = 0;

        @Override
        public ExceptionResultTransformer getCustomizedResultTransform() {
            return (result, type) -> result;
        }

        @Override
        public ExceptionHandlerResolver getCustomizedHandlerResolver() {
            return (e, target, targetMethod) -> {
                STATE = 1;
                return (ex, m, a) -> Response.newBuilder().setResult("12345").setResInfo("custom").build();
            };
        }

        @TRpcExceptionHandler
        public Object handleException(Exception e, Method method, Message message) {
            logger.error("service encountered Exception method={} message={}", method, message, e);
            STATE = 2;
            return null;
        }

        @Bean
        public TestServiceApi testServiceApi() {
            return new CustomConfigHandleExceptionAnnotationTestImpl();
        }

        @Bean
        public com.tencent.trpc.spring.exception.api.ExceptionHandler myHandler() {
            return (ex, m, a) -> {
                STATE = 3;
                if (ex instanceof IllegalStateException) {
                    return Resp.newBuilder().setRetCode("666666").setRetMsg("myHandler").build();
                }
                return Response.newBuilder().setResult("12345").setResInfo("custom").build();
            };
        }
    }

    @TRpcHandleException(exclude = IllegalArgumentException.class)
    public static class CustomConfigHandleExceptionAnnotationTestImpl implements TestServiceApi {

        @Override
        public Response test(RpcContext context, Request request) {
            if (request.getId().equals("1")) {
                throw new RuntimeException("RuntimeException");
            }
            if (request.getId().equals("2")) {
                throw new NullPointerException();
            }
            if (request.getId().equals("3")) {
                throw new IndexOutOfBoundsException();
            }
            if (request.getId().equals("4")) {
                throw new IllegalArgumentException();
            }
            return Response.newBuilder().setResult("0").setResInfo("success").build();
        }

        @Override
        @TRpcHandleException(handler = "myHandler")
        public Resp call(RpcContext context, Req request) {
            if ("1".equals(request.getResult())) {
                throw new IllegalStateException();
            }
            throw new UnsupportedOperationException("call");
        }

        @Override
        public Response ex(RpcContext context, Request request) {
            return test(context, request);
        }

        /**
         * Invalid configuration, CustomConfigHandleExceptionAnnotationTestConfiguration takes priority.
         */
        @TRpcExceptionHandler(IndexOutOfBoundsException.class)
        public HandleExceptionAnnotationTest.BaseResponse handleIndexOutOfBoundsException(Request message) {
            return HandleExceptionAnnotationTest.BaseResponse
                    .of("1111", "local IndexOutOfBoundsException " + message.getInfo());
        }

    }
}
