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

package com.tencent.trpc.core.rpc.def;

import com.tencent.trpc.core.compressor.CompressorSupport;
import com.tencent.trpc.core.compressor.spi.Compressor;
import com.tencent.trpc.core.serialization.SerializationSupport;
import com.tencent.trpc.core.serialization.spi.Serialization;
import com.tencent.trpc.core.utils.ClassUtils;
import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Encapsulate request body, create a new object for each parameter.
 */
public class DecodableValue {

    /**
     * Compression type.
     */
    protected int compressType;
    /**
     * Serialization type.
     */
    protected int serializeType;
    /**
     * Raw data.
     */
    protected byte[] rawValue;

    public DecodableValue(int compressType, int serializeType, byte[] rawValue) {
        this.compressType = compressType;
        this.serializeType = serializeType;
        this.rawValue = rawValue;
    }

    /**
     * Decompress and deserialize.
     *
     * @param argClazz parameter type
     * @param isGenerice whether it is generic
     * @return decoded object
     */
    public Object decode(Type argClazz, boolean isGenerice /* generic type */) {
        Object v = rawValue;
        if (rawValue == null) {
            return null;
        }
        Compressor compressor = CompressorSupport.ofType(compressType);
        try {
            v = compressor.decompress((byte[]) v);
        } catch (IOException e) {
            throw new RuntimeException(compressor.getClass() + " decompress error", e);
        }
        // if it is a generic type, it is directly a byte here, and no deserialization is done
        if (isGenerice) {
            return v;
        } else {
            Serialization s = SerializationSupport.ofType(serializeType);
            try {
                v = s.deserialize(ClassUtils.cast2ByteArray(v), argClazz);
            } catch (Exception e) {
                throw new RuntimeException(
                        " deserialize to " + ((Class<?>) argClazz).getGenericSuperclass().getTypeName()
                                + " error", e);
            }
        }
        return v;
    }

    public Object getRawValue() {
        return rawValue;
    }

    public int getCompressType() {
        return compressType;
    }

    public int getSerializeType() {
        return serializeType;
    }

}
