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

import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.core.utils.ProtoJsonConverter;
import com.tencent.trpc.spring.exception.api.ExceptionResultTransformer;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link ExceptionResultTransformer}.
 * <p>Marshal result Object to json, then unmarshal it to targetType.
 * targetType should extend {@link Message} or is a POJO class
 *
 * @see ProtoJsonConverter
 */
public class DefaultExceptionResultTransformer implements ExceptionResultTransformer {

    private static final Map<Class<? extends Message>, Builder> cache = new ConcurrentHashMap<>();

    private static <T extends Message> Builder newBuilder(Class<T> clazz) {
        Objects.requireNonNull(clazz, "protobuf message class must not be null");
        return cache.computeIfAbsent(clazz, c -> {
            try {
                return (Builder) c.getMethod("newBuilder").invoke(null);
            } catch (Exception e) {
                throw new IllegalStateException("newBuilder for type '" + clazz + "' error", e);
            }
        }).getDefaultInstanceForType().newBuilderForType();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object transform(Object result, Class<?> targetType) {
        if (targetType == null || result == null) {
            return null;
        }
        if (targetType.isInstance(result)) {
            return result;
        }
        String json = toJsonString(result);
        if (Message.class.isAssignableFrom(targetType)) {
            return ProtoJsonConverter.jsonToMessage(json, (Class<Message>) targetType);
        } else {
            if (!Collection.class.isAssignableFrom(targetType)) {
                return JsonUtils.fromJson(json, targetType);
            }
        }
        throw new UnsupportedOperationException(
                "unsupported transform source type '" + result.getClass().getSimpleName() + "' to target type '"
                        + targetType + "'");
    }

    private String toJsonString(Object result) {
        if (result instanceof Message) {
            return ProtoJsonConverter.messageToJson((Message) result, false, false);
        } else {
            return JsonUtils.toJson(result);
        }
    }

}
