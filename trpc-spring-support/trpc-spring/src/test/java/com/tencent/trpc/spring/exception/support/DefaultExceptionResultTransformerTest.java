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

package com.tencent.trpc.spring.exception.support;

import com.tencent.trpc.spring.exception.TestMsg.Message;
import com.tencent.trpc.spring.exception.TestMsg.Resp;
import com.tencent.trpc.spring.exception.TestMsg.Response;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals(myResponse.getResult(), response.getResult());
        Assert.assertEquals(myResponse.getResInfo(), response.getResInfo());
    }

    /**
     * Test POJO to POJO
     */
    @Test
    public void testObjectToObject() {
        DefaultExceptionResultTransformer transform = new DefaultExceptionResultTransformer();
        MyResponse myResponse = new MyResponse("testObjectToObject", "123456");
        MyResp response = (MyResp) transform.transform(myResponse, MyResp.class);
        Assert.assertEquals(myResponse.getResult(), response.getResult());
        Assert.assertEquals(myResponse.getResInfo(), response.getResInfo());
    }

    /**
     * Test protobuf to POJO
     */
    @Test
    public void testMessageToObject() {
        DefaultExceptionResultTransformer transform = new DefaultExceptionResultTransformer();
        Response response = Response.newBuilder().setResult("testMessageToObject").setResInfo("123456").build();
        MyResponse myResponse = (MyResponse) transform.transform(response, MyResponse.class);
        Assert.assertEquals(response.getResult(), myResponse.getResult());
        Assert.assertEquals(response.getResInfo(), myResponse.getResInfo());
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
        Assert.assertEquals("", resp.getRetCode());
        Assert.assertEquals("", resp.getRetMsg());
        Assert.assertEquals(1, resp.getMessageCount());
        Assert.assertEquals("myKey", resp.getMessage(0).getKey());
        Assert.assertEquals("myValue", resp.getMessage(0).getValue());
    }

    /**
     * Test unsupported result type
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupported() {
        DefaultExceptionResultTransformer transform = new DefaultExceptionResultTransformer();
        MyResponse myResponse = new MyResponse("testObjectToMessage", "123456");
        Object result = transform.transform(myResponse, List.class);
        Assert.fail();
    }

    /**
     * Test for null
     */
    @Test
    public void testTransformNull() {
        DefaultExceptionResultTransformer transform = new DefaultExceptionResultTransformer();
        Object result = transform.transform(null, null);
        Assert.assertNull(result);
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
