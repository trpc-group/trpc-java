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

package com.tencent.trpc.core.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.selector.spi.Discovery;
import com.tencent.trpc.core.utils.HelloRequestProtocol.HelloRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.assertj.core.util.Strings;
import org.junit.Assert;
import org.junit.Test;

public class JsonUtilsTest {

    private static final String JSON = "{\"test\":123}";
    private static final String JSON_LIST = "[{\"test\":123},{\"test\":123}]";
    private static final String ERROR_JSON = "{\"test\":123,A}";
    private static final String EMPTY_JSON = "{}";

    @Test
    public void testCopy() {
        ObjectMapper objectMapper = JsonUtils.copy();
        Assert.assertNotNull(objectMapper);
    }

    @Test(expected = TRpcException.class)
    public void testFromInputStream() {
        JsonUtils.fromInputStream(new InputStream() {
            @Override
            public int read() throws IOException {
                return 0;
            }
        }, Object.class);
    }

    @Test
    public void testFromJson() {
        TestObj obj = JsonUtils.fromJson(JSON, TestObj.class);
        Assert.assertEquals(obj.getTest(), 123);
        TestObj objRef = JsonUtils.fromJson(JSON, new TypeReference<TestObj>() {
        });
        Assert.assertEquals(objRef.getTest(), 123);
        List<TestObj> testObjs = JsonUtils.fromJson(JSON_LIST, JsonUtils.copy().getTypeFactory()
                .constructCollectionType(Collection.class, TestObj.class));
        Assert.assertEquals(testObjs.get(0).getTest(), 123);
        try {
            JsonUtils.fromJson(ERROR_JSON, TestObj.class);
        } catch (TRpcException e) {
            Assert.assertEquals(e.getCode(), 2001);
        }
        try {
            JsonUtils.fromJson(ERROR_JSON, JsonUtils.copy().getTypeFactory().constructCollectionType(
                            Collection.class, TestObj.class));
        } catch (TRpcException e) {
            Assert.assertEquals(e.getCode(), 2001);
        }
        try {
            JsonUtils.fromJson(ERROR_JSON, new TypeReference<TestObj>() {
            });
        } catch (TRpcException e) {
            Assert.assertEquals(e.getCode(), 2001);
        }
    }

    @Test
    public void testFromBytes() {
        TestObj obj = JsonUtils.fromBytes(JSON.getBytes(Charsets.UTF_8), TestObj.class);
        Assert.assertEquals(obj.getTest(), 123);
        TestObj objRef = JsonUtils.fromBytes(JSON.getBytes(Charsets.UTF_8), new TypeReference<TestObj>() {
        });
        Assert.assertEquals(objRef.getTest(), 123);
        objRef = JsonUtils.fromBytes(JSON, new TypeReference<TestObj>() {
        });
        Assert.assertEquals(objRef.getTest(), 123);

        try {
            JsonUtils.fromBytes(ERROR_JSON.getBytes(Charsets.UTF_8), TestObj.class);
        } catch (TRpcException e) {
            Assert.assertEquals(e.getCode(), 2001);
        }
        try {
            JsonUtils.fromBytes(ERROR_JSON.getBytes(Charsets.UTF_8), new TypeReference<TestObj>() {
            });
        } catch (TRpcException e) {
            Assert.assertEquals(e.getCode(), 2001);
        }
        try {
            JsonUtils.fromBytes(ERROR_JSON, new TypeReference<TestObj>() {
            });
        } catch (TRpcException e) {
            Assert.assertEquals(e.getCode(), 2001);
        }
    }

    @Test
    public void testToJson() {
        TestObj obj = new TestObj();
        obj.setTest(123);
        String json = JsonUtils.toJson(obj);
        Assert.assertEquals(json, JSON);
        TestObj1 obj1 = new TestObj1();
        obj1.setTest(123);
        try {
            JsonUtils.toJson(obj1);
        } catch (TRpcException e) {
            Assert.assertEquals(e.getCode(), 2001);
        }
        Assert.assertFalse(Strings.isNullOrEmpty(
                JsonUtils.toJson(ProtoJsonConverter.messageToMap(HelloRequest.getDefaultInstance()))));
    }

    @Test
    public void testToJsonWithDefaultValue() {
        TestObj obj = new TestObj();
        obj.setTest(123);
        String json = JsonUtils.toJson(obj, "aaa");
        Assert.assertEquals(json, JSON);
        TestObj1 obj1 = new TestObj1();
        obj1.setTest(123);
        String aaa = JsonUtils.toJson(obj1, "aaa");
        Assert.assertEquals(EMPTY_JSON, aaa);
    }

    @Test
    public void testToBytes() {
        TestObj obj = new TestObj();
        obj.setTest(123);
        byte[] bytes = JsonUtils.toBytes(obj);
        Assert.assertEquals(new String(bytes), JSON);
        TestObj1 obj1 = new TestObj1();
        obj1.setTest(123);
        try {
            JsonUtils.toBytes(obj1);
        } catch (TRpcException e) {
            Assert.assertEquals(e.getCode(), 2001);
        }
    }

    @Test
    public void testGeneric() {
        TestObj2<TestObj> obj = new TestObj2<>();
        TestObj testObj = new TestObj();
        testObj.setTest(11);
        obj.setData(testObj);
        String json = JsonUtils.toJson(obj);
        Assert.assertEquals(json, "{\"data\":{\"test\":11}}");
        TestObj2<TestObj> obj2 = JsonUtils.fromJson(json, new TypeReference<TestObj2<TestObj>>() {
        });
        Assert.assertEquals(obj2.getData().getTest(), 11);
    }


    @Test
    public void testConvertValue() {
        Map<String, Object> mapValue = Maps.newHashMap();
        mapValue.put("data", "123");
        mapValue.put("unknown", 111);
        mapValue.put("12", 12);
        TestConfigObject testConfigObject = JsonUtils.convertValue(mapValue, TestConfigObject.class);
        Assert.assertEquals("123", testConfigObject.getData());
        try {
            JsonUtils.convertValue(mapValue, Discovery.class);
        } catch (Exception e) {
            Assert.assertNotNull(e);
            return;
        }
        Assert.fail();
    }


    public static class TestObj {

        private int testA;

        public int getTest() {
            return testA;
        }

        public void setTest(int test) {
            this.testA = test;
        }
    }

    public static class TestObj1 {

        private int test;

        public void setTest(int test) {
            this.test = test;
        }
    }

    public static class TestObj2<T> {

        private T data;

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }

    public static class TestConfigObject {

        @JsonProperty
        private String data;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }
}
