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

package com.tencent.trpc.core.utils;

import java.io.InputStream;
import org.yaml.snakeyaml.Yaml;

/**
 * YAML parsing utility.
 */
public class YamlParser {

    public static <T> T parseAs(InputStream inputStream, Class<T> type) {
        Yaml yaml = new Yaml();
        return yaml.loadAs(inputStream, type);
    }

    public static <T> T parseAs(String filePath, Class<T> type) {
        Yaml yaml = new Yaml();
        return yaml.loadAs(filePath, type);
    }

    /**
     * Parse a YAML file from the classpath.
     *
     * @param fileName the name of the file to parse
     * @param type the class of the object to deserialize
     * @param <T> the type of the deserialized object
     * @return the deserialized object
     */
    public static <T> T parseAsFromClassPath(String fileName, Class<T> type) {
        InputStream in = YamlParser.class.getClassLoader().getResourceAsStream(fileName);
        Yaml yaml = new Yaml();
        return yaml.loadAs(in, type);
    }

}
