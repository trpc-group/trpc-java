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

package com.tencent.trpc.spring.context.configuration;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.ClientConfig;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;


public class AddFilterTRpcConfigManagerCustomizerTest {


    private static final String KEY = "OrderKey:b131d74c753540db859c626c564aa8ce";

    @Test
    public void testConstructor() {
        AddFilterTRpcConfigManagerCustomizer addFilterTRpcConfigManagerCustomizer = new AddFilterTRpcConfigManagerCustomizer();
        Assert.assertNotNull(addFilterTRpcConfigManagerCustomizer);
    }

    @Test
    public void testAddClientFilters() {
        AddFilterTRpcConfigManagerCustomizer addFilterTRpcConfigManagerCustomizer = new AddFilterTRpcConfigManagerCustomizer();
        AddFilterTRpcConfigManagerCustomizer customizer = addFilterTRpcConfigManagerCustomizer.addClientFilters(
                "filter1", "filter2");
        Assert.assertEquals(addFilterTRpcConfigManagerCustomizer, customizer);
    }

    @Test
    public void testAddServerFilters() {
        AddFilterTRpcConfigManagerCustomizer addFilterTRpcConfigManagerCustomizer = new AddFilterTRpcConfigManagerCustomizer();
        AddFilterTRpcConfigManagerCustomizer customizer = addFilterTRpcConfigManagerCustomizer.addServerFilters(
                "filter1", "filter2");
        Assert.assertEquals(addFilterTRpcConfigManagerCustomizer, customizer);
    }

    @Test
    public void testCustomize() {
        AddFilterTRpcConfigManagerCustomizer addFilterTRpcConfigManagerCustomizer = new AddFilterTRpcConfigManagerCustomizer();

        ConfigManager instance = ConfigManager.getInstance();
        ClientConfig clientConfig = new ClientConfig();
        List<String> list = Arrays.asList("filter1", "filter2");
        clientConfig.setFilters(list);
        instance.setClientConfig(clientConfig);
        addFilterTRpcConfigManagerCustomizer.customize(instance);
        List<String> filters = instance.getClientConfig().getFilters();

        Assert.assertEquals(filters.size(), list.size());
        Assert.assertEquals(list, instance.getClientConfig().getFilters());
    }

    @Test
    public void testGetOrder() {
        AddFilterTRpcConfigManagerCustomizer addFilterTRpcConfigManagerCustomizer = new AddFilterTRpcConfigManagerCustomizer2();
        Assert.assertEquals(Integer.MAX_VALUE, addFilterTRpcConfigManagerCustomizer.getOrder());
        Integer value = 1024;
        System.setProperty(KEY, String.valueOf(value));
        Assert.assertEquals((long) value, addFilterTRpcConfigManagerCustomizer.getOrder());
    }

    static final class AddFilterTRpcConfigManagerCustomizer2 extends AddFilterTRpcConfigManagerCustomizer {

        @Override
        public int getOrder() {
            return System.getProperty(KEY) == null ? super.getOrder() : Integer.parseInt(System.getProperty(KEY));
        }
    }

    @Test
    public void testEmptyIfNull() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        AddFilterTRpcConfigManagerCustomizer customizer = new AddFilterTRpcConfigManagerCustomizer();

        Method method = customizer.getClass().getDeclaredMethod("emptyIfNull", List.class);
        boolean accessible = method.isAccessible();
        method.setAccessible(true);
        List<String> list1 = Arrays.asList("filter1", "filter2", "filter3");
        Object invoke = method.invoke(customizer, list1);
        if (invoke instanceof List) {
            List<String> resList = (List<String>) invoke;
            Assert.assertEquals(list1.size(), resList.size());
        }
        method.setAccessible(accessible);
    }

    @Test
    public void testMerge() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        AddFilterTRpcConfigManagerCustomizer customizer = new AddFilterTRpcConfigManagerCustomizer();
        List<String> list1 = Arrays.asList("filter1", "filter2", "filter3");
        List<String> list2 = Arrays.asList("filter4", "filter5", "filter6");
        Method method = customizer.getClass().getDeclaredMethod("merge", List.class, List.class);
        boolean accessible = method.isAccessible();
        method.setAccessible(true);
        Object invoke = method.invoke(customizer, list1, list2);
        if (invoke instanceof List) {
            List<String> resList = (List<String>) invoke;
            Assert.assertEquals(list1.size() + list2.size(), resList.size());
        }
        method.setAccessible(accessible);
    }
}
