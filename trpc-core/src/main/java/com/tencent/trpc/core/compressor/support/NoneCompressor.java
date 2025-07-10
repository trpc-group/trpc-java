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

package com.tencent.trpc.core.compressor.support;

import com.tencent.trpc.core.compressor.CompressType;
import com.tencent.trpc.core.compressor.spi.Compressor;
import com.tencent.trpc.core.extension.Extension;
import java.io.IOException;

@Extension(NoneCompressor.NAME)
public class NoneCompressor implements Compressor {

    public static final String NAME = "none";

    @Override
    public byte[] compress(byte[] src) throws IOException {
        return src;
    }

    @Override
    public byte[] decompress(byte[] src) throws IOException {
        return src;
    }

    @Override
    public int type() {
        return CompressType.NONE;
    }

    @Override
    public String name() {
        return NoneCompressor.NAME;
    }

}