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

package com.tencent.trpc.core.serialization.support.helper;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;
import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProtoCodecManager {

    /**
     * Stores the mapping relationship between JavaBean and codec.
     */
    private static final Map<String, Codec> BEAN_TO_CODEC_MAP = new ConcurrentHashMap<>();

    /**
     * Get the codec for the given JavaBean class.
     *
     * @param pojoClass the JavaBean class
     * @param <T> the type of the JavaBean
     * @return the codec for the JavaBean class
     */
    public static <T> Codec getCodec(Class<T> pojoClass) {
        Preconditions.checkNotNull(pojoClass);
        String key = pojoClass.getName();
        return BEAN_TO_CODEC_MAP.computeIfAbsent(key,
                value -> ProtobufProxy.create(pojoClass, false));
    }

}