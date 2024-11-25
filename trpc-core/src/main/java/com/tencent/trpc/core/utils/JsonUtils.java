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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;

/**
 * JSON utility.
 */
public class JsonUtils {

    /**
     * Default global serialization object.
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    static {
        // Serialization does not include null properties
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        // Do not throw an error when deserializing if there are no corresponding properties
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // Do not throw an error when serializing if there are no public fields
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /**
     * ObjectMapper copy method, used for different serialization configurations.
     *
     * @return ObjectMapper object
     */
    public static ObjectMapper copy() {
        return objectMapper.copy();
    }

    /**
     * JSON to Object.
     *
     * @param is JSON input stream
     * @param clz the class of the object to deserialize
     * @param <T> the type of the deserialized object
     * @return the deserialized object
     */
    public static <T> T fromInputStream(InputStream is, Class<T> clz) {
        try {
            return objectMapper.readValue(is, clz);
        } catch (IOException e) {
            logger.error("object mapper readValue error:", e);
            throw TRpcException.newException(ErrorCode.JSON_DESERIALIZATION_ERR, 0,
                    "object mapper readValue error, jsonStream:%s, clz:%s", is, clz);
        }
    }

    /**
     * JSON to Object.
     *
     * @param json JSON string
     * @param clz the class of the object to deserialize
     * @param <T> the type of the deserialized object
     * @return the deserialized object
     */
    public static <T> T fromJson(String json, Class<T> clz) {
        try {
            return objectMapper.readValue(json, clz);
        } catch (IOException e) {
            logger.error("object mapper readValue error:", e);
            throw TRpcException.newException(ErrorCode.JSON_DESERIALIZATION_ERR, 0,
                    "object mapper readValue error, json:%s, clz:%s", json, clz);
        }
    }

    /**
     * JSON to Object.
     *
     * @param json JSON string
     * @param javaType the Java type of the object to deserialize
     * @param <T> the type of the deserialized object
     * @return the deserialized object
     */
    public static <T> T fromJson(String json, JavaType javaType) {
        try {
            return objectMapper.readValue(json, javaType);
        } catch (IOException e) {
            logger.error("object mapper readValue error:", e);
            throw TRpcException.newException(ErrorCode.JSON_DESERIALIZATION_ERR, 0,
                    "object mapper readValue error, json:%s, javaType:%s", json, javaType);
        }
    }

    /**
     * JSON to Object.
     *
     * @param json JSON string
     * @param typeReference the type reference of the object to deserialize
     * @param <T> the type of the deserialized object
     * @return the deserialized object
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (IOException e) {
            logger.error("object mapper readValue error:", e);
            throw TRpcException.newException(ErrorCode.JSON_DESERIALIZATION_ERR, 0,
                    "object mapper readValue error, json:%s, typeReference:%s", json,
                    typeReference);
        }
    }

    /**
     * Byte arrays to Object.
     *
     * @param bytes byte array
     * @param clz the class of the object to deserialize
     * @param <T> the type of the deserialized object
     * @return the deserialized object
     */
    public static <T> T fromBytes(byte[] bytes, Class<T> clz) {
        try {
            return objectMapper.readValue(bytes, clz);
        } catch (IOException e) {
            logger.error("object mapper readValue error:", e);
            throw TRpcException.newException(ErrorCode.JSON_DESERIALIZATION_ERR, 0,
                    "object mapper readValue error, clz:%s", clz);
        }
    }

    /**
     * Byte arrays to Object
     *
     * @param bytes byte array
     * @param typeReference the type reference of the object to deserialize
     * @param <T> the type of the deserialized object
     * @return the deserialized object
     */
    public static <T> T fromBytes(byte[] bytes, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(bytes, typeReference);
        } catch (IOException e) {
            logger.error("object mapper readValue error:", e);
            throw TRpcException.newException(ErrorCode.JSON_DESERIALIZATION_ERR, 0,
                    "object mapper readValue error, typeReference:%s", typeReference);
        }
    }

    /**
     * JSON string to Object.
     *
     * @param jsonStr JSON string
     * @param typeReference the type reference of the object to deserialize
     * @param <T> the type of the deserialized object
     * @return the deserialized object
     */
    public static <T> T fromBytes(String jsonStr, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(jsonStr, typeReference);
        } catch (IOException e) {
            logger.error("object mapper readValue error:", e);
            throw TRpcException.newException(ErrorCode.JSON_DESERIALIZATION_ERR, 0,
                    "object mapper readValue error, typeReference:%s", typeReference);
        }
    }

    /**
     * Object to JSON.
     *
     * @param obj the object to serialize
     * @return the serialized string
     */
    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (IOException e) {
            logger.error("object mapper writeValueAsString error:", e);
            throw TRpcException.newException(ErrorCode.JSON_DESERIALIZATION_ERR, 0,
                    "object mapper writeValueAsString error, obj:%s", obj);
        }
    }

    /**
     * Object to JSON.
     *
     * @param obj the object to serialize
     * @return the serialized string
     */
    public static String toJson(Object obj, String defaultValue) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (IOException e) {
            logger.error("object mapper writeValueAsString error:", e);
            return defaultValue;
        }
    }

    /**
     * Object to bytes.
     *
     * @param obj the object to serialize
     * @return the serialized bytes
     */
    public static byte[] toBytes(Object obj) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (IOException e) {
            logger.error("object mapper writeValueAsBytes error:", e);
            throw TRpcException.newException(ErrorCode.JSON_DESERIALIZATION_ERR, 0,
                    "object mapper writeValueAsBytes error, obj:%s", obj);
        }
    }

    /**
     * Object conversion.
     *
     * @param object the original data
     * @param tClass the class of the target object
     * @param <T> the type of the deserialized object
     * @return the target object
     */
    public static <T> T convertValue(Object object, Class<T> tClass) {
        try {
            return objectMapper.convertValue(object, tClass);
        } catch (Exception e) {
            logger.error("object mapper convertValue error:", e);
            throw TRpcException.newException(ErrorCode.JSON_SERIALIZATION_ERR, 0,
                    "object mapper convertValue error, obj:%s, target class:%s", object, tClass);
        }
    }

}
