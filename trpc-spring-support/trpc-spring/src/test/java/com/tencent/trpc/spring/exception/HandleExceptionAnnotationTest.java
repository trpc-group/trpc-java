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
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.spring.exception.HandleExceptionAnnotationTest.TestHandleExceptionConfiguration;
import com.tencent.trpc.spring.exception.TestMsg.Req;
import com.tencent.trpc.spring.exception.TestMsg.Request;
import com.tencent.trpc.spring.exception.TestMsg.Resp;
import com.tencent.trpc.spring.exception.TestMsg.Response;
import com.tencent.trpc.spring.exception.annotation.EnableTRpcHandleException;
import com.tencent.trpc.spring.exception.annotation.TRpcExceptionHandler;
import com.tencent.trpc.spring.exception.annotation.TRpcExceptionHandlerRegistry;
import com.tencent.trpc.spring.exception.annotation.TRpcHandleException;
import com.tencent.trpc.spring.exception.api.ExceptionResultTransformer;
import com.tencent.trpc.spring.test.TestSpringApplication;
import java.lang.reflect.Method;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test for exception-handling
 */
@SpringBootTest(classes = TestSpringApplication.class)
@ContextConfiguration(classes = TestHandleExceptionConfiguration.class)
public class HandleExceptionAnnotationTest {

    @Autowired
    private TestServiceApi testServiceApi;

    public static RpcClientContext newContext() {
        return new RpcClientContext();
    }

    /**
     * Test for handle Exception fallback
     */
    @Test
    public void testHandleException() {
        String code = String.valueOf(RandomUtils.nextInt());
        Response response = testServiceApi.test(newContext(), Request.newBuilder().setId("1").setName(code).build());
        Assertions.assertEquals("999", response.getResult());
        Assertions.assertEquals("test_Request", response.getResInfo());
    }

    /**
     * Test for handle MyException
     */
    @Test
    public void testHandleMyException() {
        String code = String.valueOf(RandomUtils.nextInt());
        Response response = testServiceApi.test(newContext(),
                Request.newBuilder().setId("3").setName(code).setInfo("testHandleMyException").build());
        Assertions.assertEquals(code, response.getResult());
        Assertions.assertEquals("testHandleMyException", response.getResInfo());
    }

    /**
     * Test for handle SonException, should call MyException handler
     */
    @Test
    public void testHandleSonException() {
        Response response = testServiceApi
                .test(newContext(), Request.newBuilder().setId("2").setName("testHandleSonException")
                        .setInfo("testHandleSonExceptiontestHandleSonException").build());
        Assertions.assertEquals("testHandleSonException", response.getResult());
        Assertions.assertEquals("testHandleSonExceptiontestHandleSonException", response.getResInfo());
    }

    /**
     * Test for handle TRpcException
     */
    @Test
    public void testHandleTRpcException() {
        String code = String.valueOf(RandomUtils.nextInt());
        Response response = testServiceApi.test(newContext(),
                Request.newBuilder().setId("4").setName(code).setInfo("testHandleException").build());
        Assertions.assertEquals(code, response.getResult());
        Assertions.assertEquals("handleTRpcException testHandleException", response.getResInfo());
    }

    /**
     * Test for unhandled exception type 'Error'
     */
    @Test
    public void testNotHandleError() {
        Assertions.assertThrows(Error.class, () -> {
            Response response = testServiceApi.test(newContext(), Request.newBuilder().setId("5").build());
            Assertions.fail();
        });
    }

    /**
     * Test for Excluded exception IllegalArgumentException
     */
    @Test
    public void testExcludeHandleException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Response response = testServiceApi.test(newContext(), Request.newBuilder().setId("6").build());
            Assertions.fail();
        });
    }

    /**
     * Test for throw another exception in handler
     */
    @Test
    public void testThrowIllegalStateException() {
        Assertions.assertThrows(IllegalMonitorStateException.class, () -> {
            Response response = testServiceApi.test(newContext(), Request.newBuilder().setId("7").build());
            Assertions.fail();
        });
    }

    /**
     * Exception thrown by non-tRPC method shouldn't be handled
     */
    @Test
    public void testCustomHandleException() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            String code = String.valueOf(RandomUtils.nextInt());
            Response response = testServiceApi.ex(newContext(), Request.newBuilder().setId("8").build());
            Assertions.fail();
        });
    }

    /**
     * Handled by void method should return null
     */
    @Test
    public void testHandlerReturnNull() {
        Response response = testServiceApi.test(newContext(), Request.newBuilder().setId("9").build());
        Assertions.assertNull(response);
    }

    /**
     * Local @TRpcExceptionHandler method take priority
     */
    @Test
    public void testLocalExceptionHandler() {
        String random = String.valueOf(System.nanoTime());
        Response response = testServiceApi
                .test(newContext(), Request.newBuilder().setId("10").setInfo(random).build());
        Assertions.assertEquals("1111", response.getResult());
        Assertions.assertEquals("local IndexOutOfBoundsException " + random, response.getResInfo());
    }

    /**
     * Set specific transformer in @TRpcHandleException
     */
    @Test
    public void testSpecificHandleResultTransform() {
        String random = String.valueOf(System.nanoTime());
        Resp response = testServiceApi.call(newContext(),
                Req.newBuilder().setInfo(random).setResult("testSpecificHandleResultTransform").build());
        Assertions.assertEquals("8888", response.getRetCode());
        Assertions.assertEquals(random + "_testSpecificHandleResultTransform999", response.getRetMsg());
    }

    /**
     * Exception thrown by non-tRPC method shouldn't be handled
     */
    @Test
    public void testUselessForNotTRpcServiceMethod() {
        Assertions.assertThrows(MyException.class, () -> {
            Response response = testServiceApi.ex(newContext(), Request.newBuilder().setId("3").build());
            Assertions.fail();
        });
    }

    /**
     * Exception thrown by non-tRPC method shouldn't be handled
     */
    @Test
    public void testIllegalMonitorStateException() {
        Assertions.assertThrows(IllegalMonitorStateException.class, () -> {
            Response response = testServiceApi.ex(newContext(), Request.newBuilder().setId("11").build());
            Assertions.fail();
        });
    }

    public static class MyException extends RuntimeException {

        private static final long serialVersionUID = 1152988038087620437L;
        private String code;
        private String msg;

        public MyException(String code, String msg) {
            super(code + msg);
            this.code = code;
            this.msg = msg;
        }

        public String getCode() {
            return this.code;
        }

        public String getMsg() {
            return this.msg;
        }
    }

    public static class SonException extends MyException {

        private static final long serialVersionUID = 4996439953681544170L;

        public SonException(String code, String msg) {
            super(code, msg);
        }
    }

    public static class BaseResponse {

        private String result;
        private String resInfo;

        private BaseResponse(String result, String resInfo) {
            this.result = result;
            this.resInfo = resInfo;
        }

        public static BaseResponse of(String result, String resInfo) {
            return new BaseResponse(result, resInfo);
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

    /**
     * TRpcExceptionHandlerRegistry on Configuration class
     */
    @Configuration
    @EnableTRpcHandleException
    @TRpcExceptionHandlerRegistry
    public static class TestHandleExceptionConfiguration {

        private static final Logger logger = LoggerFactory.getLogger(TestHandleExceptionConfiguration.class);

        @TRpcExceptionHandler
        public BaseResponse handleTRpcException(TRpcException e) {
            logger.error("service encountered TRpcException", e);
            int code = e.isFrameException() ? e.getCode() : e.getBizCode();
            return BaseResponse.of(String.valueOf(code), "handleTRpcException " + e.getMessage());
        }

        @TRpcExceptionHandler
        public BaseResponse handleException(Exception e, Method method, Message message) {
            logger.error("service encountered Exception method={} message={}", method, message, e);
            return BaseResponse.of("999", method.getName() + "_" + message.getClass().getSimpleName());
        }

        @TRpcExceptionHandler(IllegalStateException.class)
        public BaseResponse handleIllegalStateException() {
            logger.error("service encountered IllegalStateException");
            throw new IllegalMonitorStateException("handleIllegalStateException");
        }

        @TRpcExceptionHandler
        public void handleNumberFormatException(NumberFormatException e) {
            logger.error("service encountered NumberFormatException", e);
        }

        @TRpcExceptionHandler
        public void handleIllegalMonitorStateException(IllegalMonitorStateException e) {
            logger.error("service encountered IllegalMonitorStateException", e);
            throw e;
        }

        @TRpcExceptionHandler(UnsupportedOperationException.class)
        public BaseResponse handleUnsupportedOperationException(Req req) {
            logger.error("service encountered UnsupportedOperationException req={}", req);
            return BaseResponse.of("999", req.getInfo() + "_" + req.getResult());
        }

        @Bean
        public MyExceptionHandlerRegistry myExceptionHandlerRegistry() {
            return new MyExceptionHandlerRegistry();
        }

        @Bean
        public TestServiceApi testServiceApi() {
            return new HandleExceptionAnnotationTestImpl();
        }

        @Bean
        public ExceptionResultTransformer testHandleResultTransform() {
            return new MyHandleResultTransformer();
        }
    }

    /**
     * TRpcExceptionHandlerRegistry on separate class
     */
    @TRpcExceptionHandlerRegistry
    public static class MyExceptionHandlerRegistry {

        private static final Logger logger = LoggerFactory.getLogger(MyExceptionHandlerRegistry.class);

        @TRpcExceptionHandler
        public BaseResponse handleMyException(MyException e) {
            logger.error("service encountered MyException", e);
            return BaseResponse.of(e.getCode(), e.getMsg());
        }
    }

    @TRpcHandleException(exclude = IllegalArgumentException.class)
    public static class HandleExceptionAnnotationTestImpl implements TestServiceApi {

        @Override
        public Response test(RpcContext context, Request request) {
            if (request.getId().equals("1")) {
                throw new RuntimeException("RuntimeException");
            } else if (request.getId().equals("2")) {
                throw new SonException(request.getName(), request.getInfo());
            } else if (request.getId().equals("3")) {
                throw new MyException(request.getName(), request.getInfo());
            } else if (request.getId().equals("4")) {
                throw TRpcException.newBizException(Integer.parseInt(request.getName()), request.getInfo());
            } else if (request.getId().equals("5")) {
                throw new Error("Error");
            } else if (request.getId().equals("6")) {
                throw new IllegalArgumentException("IllegalArgumentException");
            } else if (request.getId().equals("7")) {
                throw new IllegalStateException("IllegalStateException");
            } else if (request.getId().equals("8")) {
                throw new NullPointerException();
            } else if (request.getId().equals("9")) {
                throw new NumberFormatException();
            } else if (request.getId().equals("10")) {
                throw new IndexOutOfBoundsException();
            } else if (request.getId().equals("11")) {
                throw new IllegalMonitorStateException("continue throw");
            } else {
                return Response.newBuilder().setResult("0").setResInfo("success").build();
            }
        }

        @Override
        @TRpcHandleException(transform = "testHandleResultTransform")
        public Resp call(RpcContext context, Req request) {
            throw new UnsupportedOperationException("call");
        }

        @Override
        public Response ex(RpcContext context, Request request) {
            return test(context, request);
        }

        @TRpcExceptionHandler(IndexOutOfBoundsException.class)
        public BaseResponse handleIndexOutOfBoundsException(Request message) {
            return BaseResponse.of("1111", "local IndexOutOfBoundsException " + message.getInfo());
        }

    }

    public static class MyHandleResultTransformer implements ExceptionResultTransformer {

        @Override
        public Object transform(Object result, Class<?> targetType) {
            BaseResponse baseResponse = (BaseResponse) result;
            return Resp.newBuilder().setRetCode("8888").setRetMsg(baseResponse.getResInfo() + baseResponse.getResult())
                    .build();
        }
    }

}
