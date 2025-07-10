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

package com.tencent.trpc.core.serialization;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.extension.ExtensionClass;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.serialization.spi.Serialization;
import java.util.Collection;
import java.util.Map;

/**
 * Serialization plugin helper class. Mainly obtain the corresponding serialization plugin instance through the
 * serialization plugin name or type.
 */
public class SerializationSupport {

    private static final Logger logger = LoggerFactory.getLogger(SerializationSupport.class);
    private static final Map<String, Serialization> nameToSerialization = Maps.newHashMap();
    private static final Map<Integer, Serialization> typeToSerialization = Maps.newHashMap();

    static {
        preLoadSerialization();
    }

    /**
     * Serialization registration method.
     */
    public static void preLoadSerialization() {
        Collection<ExtensionClass<Serialization>> serializations = ExtensionLoader
                .getExtensionLoader(Serialization.class)
                .getAllExtensionClass();
        serializations.stream()
                .map(ExtensionClass::getExtInstance)
                .forEach(serialization -> {
                    nameToSerialization.putIfAbsent(serialization.name(), serialization);
                    typeToSerialization.putIfAbsent(serialization.type(), serialization);
                });
        logger.debug("the name2compressorMap:{} , type2compressorMap:{}", nameToSerialization, typeToSerialization);
    }

    /**
     * Get the corresponding serialization plugin by the serialization plugin name.
     *
     * @param name the serialization plugin name
     * @return the corresponding plugin instance if it exists, otherwise return null
     */
    public static Serialization ofName(String name) {
        return nameToSerialization.get(name);
    }

    /**
     * Get the corresponding serialization plugin by the serialization plugin type.
     *
     * @param type the serialization plugin type
     * @return the corresponding plugin instance if it exists, otherwise return null
     */
    public static Serialization ofType(int type) {
        return typeToSerialization.get(type);
    }

}
