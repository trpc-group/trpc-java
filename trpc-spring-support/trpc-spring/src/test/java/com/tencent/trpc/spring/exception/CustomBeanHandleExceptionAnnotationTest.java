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

package com.tencent.trpc.spring.exception;

import com.google.protobuf.Message;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.spring.exception.CustomBeanHandleExceptionAnnotationTest.CustomBeanHandleExceptionAnnotationTestConfiguration;
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
import com.tencent.trpc.spring.exception.support.HandleExceptionConfiguration;
import com.tencent.trpc.spring.test.TestSpringApplication;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(classes = TestSpringApplication.class)
@ContextConfiguration(classes = CustomBeanHandleExceptionAnnotationTestConfiguration.class)
public class CustomBeanHandleExceptionAnnotationTest {

    @Autowired
    private TestServiceApi testServiceApi;

    public static RpcClientContext newContext() {
        return new RpcClientContext();
    }

    @BeforeEach
    public void setUp() throws Exception {
        CustomBeanHandleExceptionAnnotationTestConfiguration.STATE = 0;
    }

    /**
     * Test for Exception handling
     */
    @Test
    public void testHandleException() {
        String code = String.valueOf(RandomUtils.nextInt());
        Response response = testServiceApi.test(newContext(), Request.newBuilder().setId("1").setName(code).build());
        Assertions.assertEquals("12345", response.getResult());
        Assertions.assertEquals("customHandleException", response.getResInfo());
        Assertions.assertEquals(2, CustomBeanHandleExceptionAnnotationTestConfiguration.STATE);
    }

    /**
     * Test for NullPointerException handling
     */
    @Test
    public void testHandleNullPointerException() {
        String code = String.valueOf(RandomUtils.nextInt());
        Response response = testServiceApi.test(newContext(), Request.newBuilder().setId("2").setName(code).build());
        Assertions.assertEquals("98765", response.getResult());
        Assertions.assertEquals("{name=NullPointerException}", response.getResInfo());
        Assertions.assertEquals(1, CustomBeanHandleExceptionAnnotationTestConfiguration.STATE);
    }

    /**
     * Test for IndexOutOfBoundsException handling
     */
    @Test
    public void testHandleIndexOutOfBoundsException() {
        String code = String.valueOf(RandomUtils.nextInt());
        Response response = testServiceApi.test(newContext(), Request.newBuilder().setId("3").setInfo(code).build());
        Assertions.assertEquals("1111", response.getResult());
        Assertions.assertEquals("local IndexOutOfBoundsException " + code, response.getResInfo());
        Assertions.assertEquals(1, CustomBeanHandleExceptionAnnotationTestConfiguration.STATE);
    }

    /**
     * Test for IllegalArgumentException handling
     */
    @Test
    public void testHandleIllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            String code = String.valueOf(RandomUtils.nextInt());
            Response response = testServiceApi.test(newContext(),
                    Request.newBuilder().setId("4").setName(code).build());
            Assertions.fail();
        });
    }

    /**
     * Test for UnsupportedOperationException handling
     */
    @Test
    public void testUnsupportedOperationException() {
        String code = String.valueOf(RandomUtils.nextInt());
        Resp response = testServiceApi.call(newContext(), Req.newBuilder().setResult("2").setInfo(code).build());
        Assertions.assertEquals("54321", response.getRetCode());
        Assertions.assertEquals("{name=UnsupportedOperationException}", response.getRetMsg());
    }

    @Configuration
    @EnableTRpcHandleException
    @TRpcExceptionHandlerRegistry
    public static class CustomBeanHandleExceptionAnnotationTestConfiguration {

        private static final Logger logger = LoggerFactory
                .getLogger(CustomBeanHandleExceptionAnnotationTestConfiguration.class);

        public static int STATE = 0;

        @Lazy
        @Autowired
        private ExceptionHandlerResolver defaultExceptionHandlerResolver;
        @Lazy
        @Autowired
        private ExceptionResultTransformer defaultExceptionResultTransformer;

        /**
         * Higher priority than defaults of ExceptionResultTransformer
         */
        @Bean
        @Order(HandleExceptionConfiguration.DEFAULT_ORDER - 1)
        public ExceptionResultTransformer myResultTransform() {
            return (result, type) -> {
                if (result instanceof Map) {
                    if (Resp.class.isAssignableFrom(type)) {
                        return Resp.newBuilder().setRetCode("54321").setRetMsg(result.toString()).build();
                    }
                    return Response.newBuilder().setResult("98765").setResInfo(result.toString()).build();
                }
                return defaultExceptionResultTransformer.transform(result, type);
            };
        }

        /**
         * Higher priority than defaults of ExceptionHandlerResolver
         */
        @Bean
        @Order(HandleExceptionConfiguration.DEFAULT_ORDER - 1)
        public ExceptionHandlerResolver myHandlerResolver() {
            return (e, target, targetMethod) -> {
                STATE = 1;
                if (e instanceof UnsupportedOperationException || e instanceof NullPointerException) {
                    return (ex, m, a) -> {
                        Map<String, String> map = new HashMap<>();
                        map.put("name", ex.getClass().getSimpleName());
                        return map;
                    };
                }
                return defaultExceptionHandlerResolver.resolveExceptionHandler(e, target, targetMethod);
            };
        }

        @TRpcExceptionHandler
        public Response handleException(Exception e, Method method, Message message) {
            logger.error("service encountered Exception method={} message={}", method, message, e);
            STATE = 2;
            return Response.newBuilder().setResult("12345").setResInfo("customHandleException").build();
        }

        @Bean
        public TestServiceApi testServiceApi() {
            return new CustomBeanHandleExceptionAnnotationTestImpl();
        }

    }

    @TRpcHandleException(exclude = IllegalArgumentException.class)
    public static class CustomBeanHandleExceptionAnnotationTestImpl implements TestServiceApi {

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
        public Resp call(RpcContext context, Req request) {
            throw new UnsupportedOperationException("call");
        }

        @Override
        public Response ex(RpcContext context, Request request) {
            return test(context, request);
        }

        @TRpcExceptionHandler(IndexOutOfBoundsException.class)
        public CustomResponse handleIndexOutOfBoundsException(Request message) {
            return CustomResponse.of("1111", "local IndexOutOfBoundsException " + message.getInfo());
        }

    }

    public static class CustomResponse {

        private String result;
        private String resInfo;

        private CustomResponse(String result, String resInfo) {
            this.result = result;
            this.resInfo = resInfo;
        }

        public static CustomResponse of(String result, String resInfo) {
            return new CustomResponse(result, resInfo);
        }

        public String getResult() {
            return this.result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getResInfo() {
            return this.resInfo;
        }

        public void setResInfo(String resInfo) {
            this.resInfo = resInfo;
        }

    }
}
