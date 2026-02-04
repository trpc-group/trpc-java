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

package com.tencent.trpc.spring.exception.support;

import com.tencent.trpc.spring.exception.TestMsg.Message;
import com.tencent.trpc.spring.exception.TestMsg.Resp;
import com.tencent.trpc.spring.exception.TestMsg.Response;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for exception-handling result transformer
 */
public class DefaultExceptionResultTransformerTest {

    /**
     * Test POJO to protobuf
     */
    @Test
    public void testObjectToMessage() {
        DefaultExceptionResultTransformer transform = new DefaultExceptionResultTransformer();
        MyResponse myResponse = new MyResponse("testObjectToMessage", "123456");
        Response response = (Response) transform.transform(myResponse, Response.class);
        Assertions.assertEquals(myResponse.getResult(), response.getResult());
        Assertions.assertEquals(myResponse.getResInfo(), response.getResInfo());
    }

    /**
     * Test POJO to POJO
     */
    @Test
    public void testObjectToObject() {
        DefaultExceptionResultTransformer transform = new DefaultExceptionResultTransformer();
        MyResponse myResponse = new MyResponse("testObjectToObject", "123456");
        MyResp response = (MyResp) transform.transform(myResponse, MyResp.class);
        Assertions.assertEquals(myResponse.getResult(), response.getResult());
        Assertions.assertEquals(myResponse.getResInfo(), response.getResInfo());
    }

    /**
     * Test protobuf to POJO
     */
    @Test
    public void testMessageToObject() {
        DefaultExceptionResultTransformer transform = new DefaultExceptionResultTransformer();
        Response response = Response.newBuilder().setResult("testMessageToObject").setResInfo("123456").build();
        MyResponse myResponse = (MyResponse) transform.transform(response, MyResponse.class);
        Assertions.assertEquals(response.getResult(), myResponse.getResult());
        Assertions.assertEquals(response.getResInfo(), myResponse.getResInfo());
    }

    /**
     * Test protobuf to protobuf
     */
    @Test
    public void testMessageToMessage() {
        DefaultExceptionResultTransformer transform = new DefaultExceptionResultTransformer();
        Response response = Response.newBuilder().setResult("testMessageToMessage").setResInfo("123456")
                .addMessage(Message.newBuilder().setKey("myKey").setValue("myValue").build()).build();
        Resp resp = (Resp) transform.transform(response, Resp.class);
        Assertions.assertEquals("", resp.getRetCode());
        Assertions.assertEquals("", resp.getRetMsg());
        Assertions.assertEquals(1, resp.getMessageCount());
        Assertions.assertEquals("myKey", resp.getMessage(0).getKey());
        Assertions.assertEquals("myValue", resp.getMessage(0).getValue());
    }

    /**
     * Test unsupported result type
     */
    @Test
    public void testUnsupported() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            DefaultExceptionResultTransformer transform = new DefaultExceptionResultTransformer();
            MyResponse myResponse = new MyResponse("testObjectToMessage", "123456");
            Object result = transform.transform(myResponse, List.class);
            Assertions.fail();
        });
    }

    /**
     * Test for null
     */
    @Test
    public void testTransformNull() {
        DefaultExceptionResultTransformer transform = new DefaultExceptionResultTransformer();
        Object result = transform.transform(null, null);
        Assertions.assertNull(result);
    }

    public static class MyResponse {

        private String result;
        private String resInfo;

        public MyResponse() {
        }

        public MyResponse(String result, String resInfo) {
            this.result = result;
            this.resInfo = resInfo;
        }

        public String getResult() {
            return result;
        }

        public String getResInfo() {
            return resInfo;
        }
    }

    public static class MyResp {

        private String result;
        private String resInfo;

        public MyResp() {
        }

        public MyResp(String result, String resInfo) {
            this.result = result;
            this.resInfo = resInfo;
        }

        public String getResult() {
            return result;
        }

        public String getResInfo() {
            return resInfo;
        }
    }

}
