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

package com.tencent.trpc.spring.boot.starters.env;

import com.tencent.trpc.core.common.TRpcSystemProperties;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.Test;

public class TRpcConfigurationEnvironmentPostProcessorTest {

    @Test
    public void testGetCustomPropertySourceLocation()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method getCustomPropertySourceLocation = TRpcConfigurationEnvironmentPostProcessor.class
                .getDeclaredMethod("getCustomPropertySourceLocation");
        getCustomPropertySourceLocation.setAccessible(Boolean.TRUE);
        TRpcConfigurationEnvironmentPostProcessor processor = new TRpcConfigurationEnvironmentPostProcessor();
        Object configNull = getCustomPropertySourceLocation.invoke(processor);
        Assert.assertNull(configNull);
        TRpcSystemProperties.setProperties(TRpcSystemProperties.CONFIG_PATH, "/aa");
        Object config = getCustomPropertySourceLocation.invoke(processor);
        Assert.assertEquals(config, "file:///aa");
        TRpcSystemProperties.setProperties(TRpcSystemProperties.CONFIG_PATH, "file:///aa");
        Object configFile = getCustomPropertySourceLocation.invoke(processor);
        Assert.assertEquals(config, "file:///aa");
    }
}