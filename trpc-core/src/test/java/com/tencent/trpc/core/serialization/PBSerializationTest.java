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

import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import com.google.protobuf.ByteString;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.serialization.support.JavaPBSerialization;
import com.tencent.trpc.core.serialization.support.PBSerialization;
import com.tencent.trpc.core.utils.HelloRequestProtocol;
import com.tencent.trpc.core.utils.HelloRequestProtocol.HelloRequest;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PBSerializationTest {

    private static final Logger logger = LoggerFactory.getLogger(PBSerializationTest.class);

    @Test
    public void testPBSerialize() throws IOException {
        PBSerialization serial = new PBSerialization();
        HelloRequest request = HelloRequest.newBuilder()
                .setIntField(1)
                .setStringField("string")
                .setBooleanField(true)
                .setDoubleField(0.2d)
                .setFloatField(1.1f)
                .setLongField(2L)
                .setByteStringField(ByteString.EMPTY)
                .setOther(HelloRequestProtocol.Other.newBuilder().setIntField(20).build())
                .build();
        byte[] serialBytes = serial.serialize(request);
        HelloRequest deserialize = serial.deserialize(serialBytes, HelloRequest.class);
        Assertions.assertEquals(request.getIntField(), deserialize.getIntField());
        Assertions.assertEquals(request.getStringField(), deserialize.getStringField());
        Assertions.assertEquals(request.getBooleanField(), deserialize.getBooleanField());
        Assertions.assertEquals(request.getLongField(), deserialize.getLongField());
        Assertions.assertEquals(request.getByteStringField(), deserialize.getByteStringField());
        Assertions.assertEquals(request.getOther().getIntField(), deserialize.getOther().getIntField());
        Assertions.assertEquals(serial.name(), PBSerialization.NAME);
        Assertions.assertEquals(serial.type(), SerializationType.PB);
        PBSerializationTest.TestObj testObj = new PBSerializationTest.TestObj();
        testObj.setIntField(10);
        testObj.setStringField("string");
        testObj.setBooleanField(false);
        testObj.setLongField(200L);
        Other other = new Other();
        other.setIntField(1);
        testObj.setOther(other);
        try {
            serial.serialize(testObj);
        } catch (Exception e) {
            logger.error("serialize error:", e);
        }
        try {
            serial.deserialize(serialBytes, TestObj.class);
        } catch (Exception e) {
            logger.error("deserialize error:", e);
        }
        JavaPBSerialization javaPBSerialization = new JavaPBSerialization();
        byte[] serialize = javaPBSerialization.serialize(testObj);
        HelloRequest helloRequest = HelloRequest.parseFrom(serialize);
        Assertions.assertEquals(10, helloRequest.getIntField());
        Assertions.assertEquals("string", helloRequest.getStringField());
        Assertions.assertFalse(helloRequest.getBooleanField());
        Assertions.assertEquals(200L, helloRequest.getLongField());
        Assertions.assertEquals(1, helloRequest.getOther().getIntField());
    }

    public static class TestObj {

        @Protobuf(order = 1)
        private int intField;
        @Protobuf(order = 2)
        private String stringField;
        @Protobuf(order = 3)
        private boolean booleanField;
        @Protobuf(order = 4)
        private double doubleField;
        @Protobuf(order = 5)
        private float floatField;
        @Protobuf(order = 6)
        private Long longField;
        @Protobuf(order = 8)
        private Other other;


        public int getIntField() {
            return intField;
        }

        public void setIntField(int intField) {
            this.intField = intField;
        }

        public String getStringField() {
            return stringField;
        }

        public void setStringField(String stringField) {
            this.stringField = stringField;
        }

        public boolean isBooleanField() {
            return booleanField;
        }

        public void setBooleanField(boolean booleanField) {
            this.booleanField = booleanField;
        }

        public double getDoubleField() {
            return doubleField;
        }

        public void setDoubleField(double doubleField) {
            this.doubleField = doubleField;
        }

        public float getFloatField() {
            return floatField;
        }

        public void setFloatField(float floatField) {
            this.floatField = floatField;
        }

        public Long getLongField() {
            return longField;
        }

        public void setLongField(Long longField) {
            this.longField = longField;
        }

        public Other getOther() {
            return other;
        }

        public void setOther(Other other) {
            this.other = other;
        }
    }

    public static class Other {

        @Protobuf(order = 1)
        private Integer intField;

        public Integer getIntField() {
            return intField;
        }

        public void setIntField(Integer intField) {
            this.intField = intField;
        }
    }
}
