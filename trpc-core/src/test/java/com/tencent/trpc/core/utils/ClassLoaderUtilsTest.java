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

package com.tencent.trpc.core.utils;

import com.tencent.trpc.core.common.ConfigManager;
import org.junit.Assert;
import org.junit.Test;

public class ClassLoaderUtilsTest {

    @Test
    public void testGetClassLoader() throws ClassNotFoundException {
        String className = "com.tencent.trpc.core.utils.ClassLoaderUtilsTest";
        Class<?> clazz = ClassLoaderUtils.getClassLoader(this.getClass()).loadClass(className);
        Assert.assertEquals(clazz.getName(), className);
        Thread.currentThread().setContextClassLoader(null);
        clazz = ClassLoaderUtils.getClassLoader(null).loadClass(className);
        Assert.assertNotNull(clazz);
        clazz = ClassLoaderUtils.getClassLoader(this.getClass()).loadClass(className);
        Assert.assertEquals(clazz.getName(), className);
        ClassLoader pre = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(pre);
    }

    @Test
    public void testGetCachedClassLoader() {
        Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        ConfigManager.getInstance().start();
        ClassLoader cachedClassLoader = ClassLoaderUtils.getClassLoader(null);
        Assert.assertEquals(cachedClassLoader, classLoader);
    }
}
