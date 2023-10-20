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

package com.tencent.trpc.core.transport.codec;

import com.tencent.trpc.core.compressor.CompressType;
import com.tencent.trpc.core.compressor.CompressorSupport;
import com.tencent.trpc.core.compressor.spi.Compressor;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.def.EncodableValue;
import com.tencent.trpc.core.serialization.SerializationSupport;
import com.tencent.trpc.core.serialization.spi.Serialization;
import java.util.Objects;

public abstract class AbstractCodec implements Codec {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected Serialization checkAndGetSerialization(String name) {
        return Objects.requireNonNull(SerializationSupport.ofName(name), "serialization " + name + " is not exists");
    }

    protected Serialization checkAndGetSerialization(int type) {
        return Objects.requireNonNull(SerializationSupport.ofType(type), "serialization " + type + " is not exists");
    }

    protected Compressor checkAndGetCompressor(String name) {
        return Objects.requireNonNull(CompressorSupport.ofName(name), "compressor " + name + " is not exists");
    }

    protected EncodableValue getEncodableValue(int compressMinBytes, Serialization serialization,
            Compressor compressor, boolean isGeneric, Object data) {
        return new EncodableValue(compressor, compressMinBytes, serialization, isGeneric, data);
    }

    public int getContentEncoding(EncodableValue value) {
        return value.getCompressed() ? value.getCompressor().type() : CompressType.NONE;
    }

}
