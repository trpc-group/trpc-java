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

package com.tencent.trpc.core.serialization;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.trpc.core.serialization.support.JSONSerialization;
import com.tencent.trpc.core.utils.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.core.utils.JsonUtilsTest;
import java.io.IOException;
import java.util.List;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JsonSerializationTest {

    private JSONSerialization serial;

    private GenericsObj<Obj> genericsObj;

    private Obj obj;

    @Before
    public void setUp() {
        serial = new JSONSerialization();
        genericsObj = new GenericsObj<>();
        genericsObj.setIntField(10);
        obj = new Obj();
        obj.setName("a");
        genericsObj.setData(obj);
    }

    @Test
    public void testJson2Pojo() {
        byte[] bytes = JsonUtils.toBytes(obj);
        Obj1 deserialize1 = JsonUtils.fromBytes(bytes, Obj1.class);
        assertEquals("a", deserialize1.getName());
        assertEquals("b", deserialize1.getBb());
        Obj1 obj1 = new Obj1();
        obj1.setName("obj");
        byte[] bytes1 = JsonUtils.toBytes(obj1);
        Obj obj = JsonUtils.fromBytes(bytes1, Obj.class);
        assertEquals("obj", obj.getName());
    }

    /**
     * Test JSON deserialization exception case.
     */
    @Test
    public void testJson2PojoEx() {
        try {
            JsonUtilsTest.TestObj1 obj1 = new JsonUtilsTest.TestObj1();
            obj1.setTest(123);
            serial.serialize(obj1);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("json serialize ex:"));
        }
        try {
            byte[] bytes = serial.serialize(obj);
            GenericsObj deserialize = serial.deserialize(bytes, GenericsObj.class);
            Assert.assertNull(deserialize.getData());
        } catch (IOException ioe) {
            Assert.assertTrue(ioe.getMessage().contains("json deserialize exception:"));
        }
        try {
            byte[] bytes = serial.serialize(obj);
            TestObj1 deserialize = serial.deserialize(bytes, new TypeReference<TestObj1>() {
            }.getType());
            Assert.assertEquals(0, deserialize.test);
        } catch (IOException ioe) {
            Assert.assertTrue(ioe.getMessage().contains("json deserialize exception:"));
        }
    }

    /**
     * Test deserializing JSON to PB (Protocol Buffers) classes.
     *
     * @throws IOException IOException
     */
    @Test
    public void testJson2Pb() throws IOException {
        HelloRequest request = HelloRequest.newBuilder().setIntField(10).build();
        byte[] serialBytes = serial.serialize(request);
        HelloRequest deserialize = serial.deserialize(serialBytes, HelloRequest.class);
        assertEquals(10, deserialize.getIntField());
        assertEquals(serial.name(), JSONSerialization.NAME);

        deserialize = serial.deserialize(serialBytes, new TypeReference<HelloRequest>() {
        }.getType());
        assertEquals(10, deserialize.getIntField());
        assertEquals(serial.name(), JSONSerialization.NAME);
    }

    /**
     * Test deserializing JSON to generic types.
     *
     * @throws IOException IOException
     */
    @Test
    public void testJson2PojoByTypeReference() throws IOException {
        byte[] serialBytes = serial.serialize(genericsObj);
        GenericsObj<Obj> deserialize = serial.deserialize(serialBytes, new TypeReference<GenericsObj<Obj>>() {
        }.getType());
        assertEquals(10, deserialize.getIntField());
        assertEquals("a", deserialize.getData().getName());
        assertEquals(serial.name(), JSONSerialization.NAME);
        assertEquals(serial.type(), SerializationType.JSON);
        GenericsObj<List<Obj>> testObjList = new GenericsObj<>();
        testObjList.setIntField(20);
        List<Obj> objs = Lists.newArrayList();
        Obj obj1 = new Obj();
        obj1.setName("a");
        objs.add(obj1);
        testObjList.setData(objs);
        byte[] serializeList = serial.serialize(testObjList);
        GenericsObj<List<Obj>> deserializeList =
                serial.deserialize(serializeList, new TypeReference<GenericsObj<List<Obj>>>() {}.getType());
        assertEquals(20, deserializeList.getIntField());
        assertEquals("a", deserializeList.getData().get(0).getName());
        byte[] bytes = serial.serialize(obj);
        Obj deserialize1 = serial.deserialize(bytes, new TypeReference<Obj>() {
        }.getType());
        assertEquals("a", deserialize1.getName());
    }

    public static class GenericsObj<T> {

        private int intField;

        private T data;

        public int getIntField() {
            return intField;
        }

        public void setIntField(int intField) {
            this.intField = intField;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }

    public static class Obj {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Obj1 {

        private String name;

        private String bb = "b";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getBb() {
            return bb;
        }

        public void setBb(String bb) {
            this.bb = bb;
        }
    }

    public static class TestObj1 {

        private int test;

        public void setTest(int test) {
            this.test = test;
        }
    }
}