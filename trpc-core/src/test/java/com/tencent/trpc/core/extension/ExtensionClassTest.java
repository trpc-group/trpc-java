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

package com.tencent.trpc.core.extension;

import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import java.util.concurrent.CompletionStage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class ExtensionClassTest {

    @After
    public void after() {
        ExtensionLoader.destroyAllPlugin();
    }

    @Test
    public void testCreateInstance() {
        ExtensionLoader.registerPlugin(new PluginConfig("testFilter", Filter.class, TestFilter.class));
        ExtensionClass<Filter> extensionClass = ExtensionLoader.getExtensionLoader(Filter.class)
                .getExtensionClass("testFilter");
        Assert.assertNotNull(extensionClass);
        Filter instance = extensionClass.getExtInstance();
        Assert.assertNotNull(instance);
        Assert.assertTrue(instance instanceof TestFilter);
    }

    @Test
    public void testDebugLog() {
        ExtensionLoader.registerPlugin(new PluginConfig("testFilter", Filter.class, TestFilter.class));
        ExtensionClass<Filter> extensionClass = ExtensionLoader.getExtensionLoader(Filter.class)
                .getExtensionClass("testFilter");
        Assert.assertNotNull(extensionClass);
        // 测试创建实例时的 debug 日志
        Filter instance = extensionClass.getExtInstance();
        Assert.assertNotNull(instance);
        // 再次获取实例，应该返回缓存的实例
        Filter instance2 = extensionClass.getExtInstance();
        Assert.assertSame(instance, instance2);
    }

    public static class TestFilter implements Filter {

        @Override
        public CompletionStage<Response> filter(Invoker<?> invoker, Request request) {
            return invoker.invoke(request);
        }
    }
}
