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

package com.tencent.trpc.core.serialization.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.protobuf.Message;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.serialization.SerializationType;
import com.tencent.trpc.core.serialization.spi.Serialization;
import com.tencent.trpc.core.utils.ClassUtils;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.core.utils.ProtoJsonConverter;
import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

@Extension(JSONSerialization.NAME)
@SuppressWarnings("unchecked")
public class JSONSerialization implements Serialization {

    public static final String NAME = "json";
    /**
     * PB class method information cache. One class per method, initial cache size 20, maximum 500.
     */
    private static final Cache<Class, Method> CLASS_METHOD_CACHE = Caffeine.newBuilder()
            .initialCapacity(20)
            .maximumSize(500)
            .build();

    /**
     * Default UTF-8 serialize.
     *
     * @param obj the object to be serialized
     * @return the serialized byte array
     * @throws IOException IO exception
     */
    @Override
    public byte[] serialize(Object obj) throws IOException {
        try {
            // pb to bytes
            if (obj instanceof Message) {
                return JsonUtils.toBytes(ProtoJsonConverter.messageToMap((Message) obj));
            } else {
                return JsonUtils.toBytes(obj);
            }
        } catch (Exception ex) {
            throw new IOException("json serialize ex:", ex);
        }
    }

    /**
     * Json deserialization, does not support generics.
     *
     * @param bytes the byte array to be deserialized
     * @param clazz the object type after deserialization
     * @param <T> the instance type after deserialization
     * @return the instance after deserialization
     * @throws IOException IO exception
     */
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws IOException {
        try {
            if (Message.class.isAssignableFrom(clazz)) {
                Method method = CLASS_METHOD_CACHE
                        .get(clazz, clz -> ClassUtils.getDeclaredMethod(clz, "getDefaultInstance"));
                Objects.requireNonNull(method, "the method can't be null");
                Message pbMsg = (Message) method.invoke(null);
                return (T) ProtoJsonConverter.jsonToMessage(new String(bytes, StandardCharsets.UTF_8), pbMsg);
            }
            // bytes 转 java 对象
            return JsonUtils.fromBytes(bytes, clazz);
        } catch (Exception ex) {
            throw new IOException("json deserialize exception:", ex);
        }
    }

    /**
     * Json deserialization, supports generics.
     *
     * @param bytes the byte array to be deserialized
     * @param type the original object type
     * @param <T> the generic type
     * @return the object after deserialization
     * @throws IOException io exception
     */
    @Override
    public <T> T deserialize(byte[] bytes, Type type) throws IOException {
        try {
            if (isGeneric(type)) {
                // bytes to java bean
                return JsonUtils.fromBytes(bytes, new TypeReference<T>() {
                    @Override
                    public Type getType() {
                        return type;
                    }
                });
            }
            return deserialize(bytes, (Class<T>) type);
        } catch (Exception ex) {
            throw new IOException("json deserialize exception:", ex);
        }
    }

    @Override
    public int type() {
        return SerializationType.JSON;
    }

    @Override
    public String name() {
        return NAME;
    }

    /**
     * Check if it's a generic type.
     *
     * @param type the type to be deserialized
     * @return true if it's a generic type, false otherwise
     */
    private boolean isGeneric(Type type) {
        return type instanceof ParameterizedType || type instanceof TypeVariable
                || type instanceof GenericArrayType || type instanceof WildcardType;
    }

}
