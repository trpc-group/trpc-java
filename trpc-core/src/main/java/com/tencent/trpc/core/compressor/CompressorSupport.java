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

import com.google.common.collect.Maps;
import com.tencent.trpc.core.compressor.spi.Compressor;
import com.tencent.trpc.core.extension.ExtensionClass;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.util.Collection;
import java.util.Map;

public class CompressorSupport {

    private static final Logger logger = LoggerFactory.getLogger(CompressorSupport.class);

    private static final Map<String, Compressor> nameToCompressor = Maps.newHashMap();

    private static final Map<Integer, Compressor> typeToCompressor = Maps.newHashMap();

    static {
        preLoadCompressors();
    }

    public static void preLoadCompressors() {
        Collection<ExtensionClass<Compressor>> compressors = ExtensionLoader.getExtensionLoader(Compressor.class)
                .getAllExtensionClass();
        compressors.stream()
                .map(ExtensionClass::getExtInstance)
                .forEach(compressor -> {
                    nameToCompressor.putIfAbsent(compressor.name(), compressor);
                    typeToCompressor.putIfAbsent(compressor.type(), compressor);
                });
        logger.debug("the name2compressorMap:{} , type2compressorMap:{}", nameToCompressor, typeToCompressor);
    }

    public static Compressor ofName(String name) {
        return nameToCompressor.get(name);
    }

    public static Compressor ofType(int type) {
        return typeToCompressor.get(type);
    }

}