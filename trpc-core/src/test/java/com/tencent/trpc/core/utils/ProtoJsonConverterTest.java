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

package com.tencent.trpc.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.ByteString;
import com.tencent.trpc.core.utils.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.core.utils.HelloRequestProtocol.HelloRequest.Builder;
import com.tencent.trpc.core.utils.HelloRequestProtocol.Other;
import com.tencent.trpc.core.utils.HelloRequestProtocol.Week;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * pb to json test class
 */
public class ProtoJsonConverterTest {

    private static final HelloRequestProtocol.HelloRequest REQUEST =
            HelloRequestProtocol.HelloRequest
                    .newBuilder()
                    .setIntField(1).setStringField("string$中文").setBooleanField(true)
                    .setDoubleField(1.0)
                    .setFloatField(2.0f)
                    .setLongField(100)
                    .setByteStringField(ByteString.copyFrom("a".getBytes(Charsets.UTF_8)))
                    .setWeek(Week.FIRST)
                    .setOther(Other.newBuilder().setIntField(2).build()).addIntFields(1)
                    .addStringFields("string$").addBooleanFields(true).addDoubleFields(1.0)
                    .addFloatFields(2.0f)
                    .addLongFields(100)
                    .addByteStringFields(ByteString.copyFrom("a".getBytes(Charsets.UTF_8)))
                    .addWeeks(Week.FIRST).addOthers(Other.newBuilder().setIntField(2).build())
                    .build();

    @Test
    public void testToString() {
        String toString = ProtoJsonConverter.toString(REQUEST);
        Assertions.assertEquals(toString, "{\n"
                + "  \"intField\": 1,\n"
                + "  \"stringField\": \"string$中文\",\n"
                + "  \"booleanField\": true,\n"
                + "  \"doubleField\": 1.0,\n"
                + "  \"floatField\": 2.0,\n"
                + "  \"longField\": \"100\",\n"
                + "  \"byteStringField\": \"YQ==\",\n"
                + "  \"other\": {\n"
                + "    \"intField\": 2\n"
                + "  },\n"
                + "  \"week\": 0,\n"
                + "  \"intFields\": [1],\n"
                + "  \"stringFields\": [\"string$\"],\n"
                + "  \"booleanFields\": [true],\n"
                + "  \"doubleFields\": [1.0],\n"
                + "  \"floatFields\": [2.0],\n"
                + "  \"longFields\": [\"100\"],\n"
                + "  \"bytestringFields\": [\"YQ==\"],\n"
                + "  \"others\": [{\n"
                + "    \"intField\": 2\n"
                + "  }],\n"
                + "  \"weeks\": [0]\n"
                + "}");
        String nullString = ProtoJsonConverter.toString(null);
        Assertions.assertNull(nullString);
    }

    @Test
    public void testMessageToMapDefault() {
        Builder newRequest = HelloRequestProtocol.HelloRequest.newBuilder();
        newRequest.setIntField(0);
        assertEquals(
                ((Integer) (ProtoJsonConverter.messageToMap(newRequest.build()).get("intField"))).intValue(), 0);
        Map<String, Object> messageToMap = new HashMap<String, Object>();
        messageToMap.put("intField", "0");
        Builder newRequest1 = HelloRequestProtocol.HelloRequest.newBuilder();
        ProtoJsonConverter.mapToBuilder(messageToMap, newRequest1);
        assertEquals(newRequest1.getIntField(), 0);
    }

    /**
     * Test pb message to map， map to pb message , map to pb builder
     */
    @Test
    public void testMessageToMap() {
        // pb message to map
        Map<String, Object> messageToMap = ProtoJsonConverter.messageToMap(REQUEST);
        assertEquals(messageToMap.get("intField"), 1);
        assertEquals(messageToMap.get("stringField"), "string$中文");
        assertTrue(Boolean.parseBoolean(messageToMap.get("booleanField").toString()));
        assertEquals(messageToMap.get("doubleField"), 1.0D);
        assertEquals(messageToMap.get("floatField"), 2.0D);
        assertEquals(Long.parseLong(messageToMap.get("longField").toString()), 100L);
    }

    @Test
    public void testMessageToMapToMessageBuilder() {
        //message to map
        Map<String, Object> messageToMap = ProtoJsonConverter.messageToMap(REQUEST);
        //map to message builder
        Builder newRequest = HelloRequestProtocol.HelloRequest.newBuilder();
        ProtoJsonConverter.mapToBuilder(messageToMap, newRequest);
        assertEquals(newRequest.getIntField(), 1);
        assertEquals(newRequest.getStringField(), "string$中文");
        assertTrue(newRequest.getBooleanField());
        assertEquals(1.0d, newRequest.getDoubleField(), 0.0);
        assertEquals(2.0f, newRequest.getFloatField(), 0.0);
        assertEquals(100d, newRequest.getLongField(), 0.0);
        assertEquals(new String(newRequest.getByteStringField().toByteArray(), Charsets.UTF_8), "a");
        assertEquals(newRequest.getWeek(), Week.FIRST);
        assertEquals(newRequest.getOther().getIntField(), 2);
        assertEquals(newRequest.getIntFields(0), 1);
        assertEquals(newRequest.getStringFields(0), "string$");
        assertTrue(newRequest.getBooleanFields(0));
        assertEquals(1.0d, newRequest.getDoubleFields(0), 0.0);
        assertEquals(2.0f, newRequest.getFloatFields(0), 0.0);
        assertEquals(100d, newRequest.getLongFields(0), 0.0);
        assertEquals(new String(newRequest.getByteStringFields(0).toByteArray(), Charsets.UTF_8), "a");
        assertEquals(newRequest.getWeeks(0), Week.FIRST);
        assertEquals(newRequest.getOthers(0).getIntField(), 2);
    }

    @Test
    public void testMessageToMapToMessage() {
        //pb message to map
        Map<String, Object> messageToMap = ProtoJsonConverter.messageToMap(REQUEST);
        // map to pb message
        HelloRequestProtocol.HelloRequest requestMessage = HelloRequest.newBuilder().build();
        requestMessage = (HelloRequest) ProtoJsonConverter
                .mapToMessage(messageToMap, requestMessage);
        assertEquals(requestMessage.getIntField(), 1);
        assertEquals(requestMessage.getStringField(), "string$中文");
        assertTrue(requestMessage.getBooleanField());
        assertEquals(1.0d, requestMessage.getDoubleField(), 0.0);
        assertEquals(2.0f, requestMessage.getFloatField(), 0.0);
        assertEquals(100d, requestMessage.getLongField(), 0.0);
        assertEquals(new String(requestMessage.getByteStringField().toByteArray(),
                Charsets.UTF_8), "a");
        assertEquals(requestMessage.getWeek(), Week.FIRST);
        assertEquals(requestMessage.getOther().getIntField(), 2);
        assertEquals(requestMessage.getIntFields(0), 1);
        assertEquals(requestMessage.getStringFields(0), "string$");
        assertTrue(requestMessage.getBooleanFields(0));
        assertEquals(1.0d, requestMessage.getDoubleFields(0), 0.0);
        assertEquals(2.0f, requestMessage.getFloatFields(0), 0.0);
        assertEquals(100d, requestMessage.getLongFields(0), 0.0);
        assertEquals(
                new String(requestMessage.getByteStringFields(0).toByteArray(), Charsets.UTF_8), "a");
        assertEquals(requestMessage.getWeeks(0), Week.FIRST);
        assertEquals(requestMessage.getOthers(0).getIntField(), 2);
    }

    @Test
    public void testMapString2MessageBuilder() {
        Map<String, Object> messageToMap = new HashMap<String, Object>();
        messageToMap.put("intField", "1");
        messageToMap.put("stringField", "string");
        messageToMap.put("booleanField", "true");
        messageToMap.put("doubleField", "1.0");
        messageToMap.put("floatField", "2.0");
        messageToMap.put("longField", "100");
        messageToMap.put("byteStringField",
                Base64.getEncoder().encodeToString("abc中文".getBytes(Charsets.UTF_8)));
        //map to pb builder
        Builder newRequest = HelloRequestProtocol.HelloRequest.newBuilder();
        ProtoJsonConverter.mapToBuilder(messageToMap, newRequest);
        assertEquals(newRequest.getIntField(), 1);
        assertEquals(newRequest.getStringField(), "string");
        assertTrue(newRequest.getBooleanField());
        assertEquals(1.0d, newRequest.getDoubleField(), 0.0);
        assertEquals(2.0f, newRequest.getFloatField(), 0.0);
        assertEquals(100d, newRequest.getLongField(), 0.0);
        assertEquals(newRequest.getByteStringField().toStringUtf8(), "abc中文");
    }

    @Test
    public void testMessageToJson() {
        String json = ProtoJsonConverter.messageToJson(REQUEST);
        HelloRequestProtocol.HelloRequest requestMessage = HelloRequest.newBuilder().build();
        requestMessage = (HelloRequest) ProtoJsonConverter.jsonToMessage(json, requestMessage);
        assertEquals(requestMessage.getIntField(), 1);
        assertEquals(requestMessage.getStringField(), "string$中文");
        assertTrue(requestMessage.getBooleanField());
        assertEquals(1.0d, requestMessage.getDoubleField(), 0.0);
        assertEquals(2.0f, requestMessage.getFloatField(), 0.0);
        assertEquals(100d, requestMessage.getLongField(), 0.0);
        assertEquals(new String(requestMessage.getByteStringField().toByteArray(), Charsets.UTF_8), "a");
        assertEquals(requestMessage.getWeek(), Week.FIRST);
        assertEquals(requestMessage.getOther().getIntField(), 2);
        assertEquals(requestMessage.getIntFields(0), 1);
        assertEquals(requestMessage.getStringFields(0), "string$");
        assertTrue(requestMessage.getBooleanFields(0));
        assertEquals(1.0d, requestMessage.getDoubleFields(0), 0.0);
        assertEquals(2.0f, requestMessage.getFloatFields(0), 0.0);
        assertEquals(100d, requestMessage.getLongFields(0), 0.0);
        assertEquals(
                new String(requestMessage.getByteStringFields(0).toByteArray(), Charsets.UTF_8), "a");
        assertEquals(requestMessage.getWeeks(0), Week.FIRST);
        assertEquals(requestMessage.getOthers(0).getIntField(), 2);
    }

    @Test
    public void testJsonToMessage() {
        String json = ProtoJsonConverter.messageToJson(REQUEST, false, false);
        Assertions.assertEquals("{\"intField\":1,\"stringField\":\"string$中文\",\"booleanField\":true,"
                + "\"doubleField\":1.0,\"floatField\":2.0,\"longField\":\"100\",\"byteStringField\":\"YQ==\","
                + "\"other\":{\"intField\":2},\"intFields\":[1],\"stringFields\":[\"string$\"],"
                + "\"booleanFields\":[true],\"doubleFields\":[1.0],\"floatFields\":[2.0],"
                + "\"longFields\":[\"100\"],\"bytestringFields\":[\"YQ==\"],"
                + "\"others\":[{\"intField\":2}],\"weeks\":[0]}", json);

        HelloRequestProtocol.HelloRequest requestMessage = ProtoJsonConverter
                .jsonToMessage(json, HelloRequestProtocol.HelloRequest.class);

        assertEquals(requestMessage.getIntField(), 1);
        assertEquals(requestMessage.getStringField(), "string$中文");
        assertTrue(requestMessage.getBooleanField());
        assertEquals(1.0d, requestMessage.getDoubleField(), 0.0);
        assertEquals(2.0f, requestMessage.getFloatField(), 0.0);
        assertEquals(100d, requestMessage.getLongField(), 0.0);
        assertEquals(new String(requestMessage.getByteStringField().toByteArray(),
                Charsets.UTF_8), "a");
        assertEquals(requestMessage.getWeek(), Week.FIRST);
        assertEquals(requestMessage.getOther().getIntField(), 2);

        assertEquals(requestMessage.getIntFields(0), 1);
        assertEquals(requestMessage.getStringFields(0), "string$");
        assertTrue(requestMessage.getBooleanFields(0));
        assertEquals(1.0d, requestMessage.getDoubleFields(0), 0.0);
        assertEquals(2.0f, requestMessage.getFloatFields(0), 0.0);
        assertEquals(100d, requestMessage.getLongFields(0), 0.0);
        assertEquals(
                new String(requestMessage.getByteStringFields(0).toByteArray(),
                        Charsets.UTF_8), "a");
        assertEquals(requestMessage.getWeeks(0), Week.FIRST);
        assertEquals(requestMessage.getOthers(0).getIntField(), 2);
    }

}
