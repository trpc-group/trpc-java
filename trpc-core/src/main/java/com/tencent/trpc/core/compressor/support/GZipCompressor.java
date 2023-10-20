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

package com.tencent.trpc.core.compressor.support;

import com.tencent.trpc.core.compressor.CompressType;
import com.tencent.trpc.core.compressor.spi.Compressor;
import com.tencent.trpc.core.extension.Extension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Extension(GZipCompressor.NAME)
public class GZipCompressor implements Compressor {

    public static final String NAME = "gzip";

    @Override
    public byte[] compress(byte[] src) throws IOException {
        if (src == null || src.length == 0) {
            return src;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(src);
        }
        return out.toByteArray();
    }

    @Override
    public byte[] decompress(byte[] src) throws IOException {
        if (src == null || src.length == 0) {
            return src;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(src);
        try (GZIPInputStream ungzip = new GZIPInputStream(in)) {
            byte[] buffer = new byte[2048];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    @Override
    public int type() {
        return CompressType.GZIP;
    }

    @Override
    public String name() {
        return GZipCompressor.NAME;
    }

}