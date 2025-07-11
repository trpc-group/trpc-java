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
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class YamlParserTest {

    private static final String defaultFileName = "trpc_java_default-test.yml";

    @Test
    public void testParseAsInputStream() {
        InputStream resourceAsStream = ClassLoaderUtils.
                getClassLoader(YamlParser.class).getResourceAsStream(defaultFileName);
        Map map = YamlParser.parseAs(resourceAsStream, Map.class);
        Assert.assertNotNull(map);
    }

    @Test
    public void testParseAs() {
        String path = YamlParser.class.getClassLoader().getResource(defaultFileName).getPath();
        Object map = YamlParser.parseAs(path, Object.class);
        Assert.assertNotNull(map);
    }

    @Test
    public void testParseAsFromClassPath() {
        Map map = YamlParser.parseAsFromClassPath(defaultFileName, Map.class);
        Assert.assertNotNull(map);
    }
}