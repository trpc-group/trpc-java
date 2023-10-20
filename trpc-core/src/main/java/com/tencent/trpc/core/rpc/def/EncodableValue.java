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

import com.tencent.trpc.core.compressor.spi.Compressor;
import com.tencent.trpc.core.serialization.spi.Serialization;
import com.tencent.trpc.core.utils.ClassUtils;
import java.io.IOException;

/**
 * Utility class for encoding, used by codec.
 */
public class EncodableValue {

    /**
     * Compression type.
     */
    protected Compressor compressor;
    /**
     * Minimum bytes to enable compression.
     */
    protected int compressMinBytes;
    /**
     * Whether it's compressed.
     */
    protected boolean compressed;
    protected Serialization serialization;
    /**
     * Whether it's generic data.
     */
    protected boolean generic;
    /**
     * Raw data.
     */
    protected Object rawValue;

    public EncodableValue(Compressor compressor, int compressMinBytes, Serialization serialization,
            boolean isGeneric, Object rawValue) {
        this.compressor = compressor;
        this.compressMinBytes = compressMinBytes;
        this.serialization = serialization;
        this.generic = isGeneric;
        this.rawValue = rawValue;
    }

    public byte[] encode() {
        Object v = rawValue;
        if (v == null) {
            return null;
        }
        // If it's a byte type, it means to get the raw data information and not to do the following (serialization
        // or other) operations
        if (!generic) {
            try {
                v = serialization.serialize(v);
            } catch (IOException e) {
                throw new RuntimeException(serialization.getClass() + " serialize "
                        + rawValue.getClass().getName() + " error", e);
            }
        }
        byte[] byteValues = ClassUtils.cast2ByteArray(v);
        if (byteValues.length < compressMinBytes) {
            return byteValues;
        }
        try {
            compressed = Boolean.TRUE;
            return compressor.compress(byteValues);
        } catch (IOException e) {
            throw new RuntimeException(compressor.getClass() + " compress error", e);
        }
    }

    public Object getRawValue() {
        return rawValue;
    }

    public boolean getCompressed() {
        return this.compressed;
    }

    public Compressor getCompressor() {
        return compressor;
    }

}
