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

package com.tencent.trpc.core.compressor;

import com.tencent.trpc.core.compressor.spi.Compressor;
import com.tencent.trpc.core.compressor.support.GZipCompressor;
import com.tencent.trpc.core.extension.ExtensionClass;
import com.tencent.trpc.core.extension.ExtensionLoader;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

public class CompressorTest {

    @Test
    public void testCompress() {
        CompressorSupport.preLoadCompressors();
        for (ExtensionClass<Compressor> each : ExtensionLoader.getExtensionLoader(Compressor.class)
                .getAllExtensionClass()) {
            Compressor c = CompressorSupport.ofName(each.getName());
            Compressor c2 = CompressorSupport.ofType(c.type());
            Assert.assertEquals(c.type(), c2.type());
            Assert.assertEquals(c.name(), each.getName());
            String s = "中英文压测数据ABC123";
            try {
                byte[] data = c.compress(s.getBytes());
                byte[] result = c.decompress(data);
                Assert.assertEquals(new String(result), s);
                Assert.assertNull(c.compress(null));
                Assert.assertNull(c.decompress(null));
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    @Test
    public void testGzip() {
        GZipCompressor c = new GZipCompressor();
        String s = "A long time ago in a galaxy far, far away...";
        try {
            byte[] data = c.compress(s.getBytes());
            byte[] result = c.decompress(data);
            Assert.assertEquals(new String(result), s);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}