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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.protobuf.Message;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.serialization.SerializationSupport;
import com.tencent.trpc.core.serialization.SerializationType;
import com.tencent.trpc.core.serialization.spi.Serialization;
import com.tencent.trpc.core.utils.ClassUtils;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Objects;

@Extension(PBSerialization.NAME)
public class PBSerialization implements Serialization {

    public static final String NAME = "pb";
    /**
     * PB class method information cache. One class per method, initial cache size 20, maximum 500.
     */
    private static final Cache<Class, Method> CLASS_METHOD_CACHE = Caffeine.newBuilder()
            .initialCapacity(20)
            .maximumSize(500)
            .build();

    @Override
    public byte[] serialize(Object obj) throws IOException {
        if (obj instanceof Message) {
            return ((Message) obj).toByteArray();
        }
        // when the parameter type is not a pb message, use jpb serialization
        Serialization jpb = SerializationSupport.ofName(JavaPBSerialization.NAME);
        return jpb.serialize(obj);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws IOException {
        if (Message.class.isAssignableFrom(clazz)) {
            try {
                Method method = CLASS_METHOD_CACHE.get(clazz, clz -> ClassUtils
                        .getDeclaredMethod(clz, "parseFrom", byte[].class));
                Objects.requireNonNull(method, "the method can't be null");
                return (T) method.invoke(null, bytes);
            } catch (Exception e) {
                throw TRpcException
                        .newFrameException(ErrorCode.TRPC_INVOKE_UNKNOWN_ERR, e.getMessage(), e);
            }
        } else {
            // when the parameter type is not a pb message, use jpb serialization
            Serialization jpb = SerializationSupport.ofName(JavaPBSerialization.NAME);
            return jpb.deserialize(bytes, clazz);
        }
    }

    @Override
    public int type() {
        return SerializationType.PB;
    }

    @Override
    public String name() {
        return NAME;
    }

}
