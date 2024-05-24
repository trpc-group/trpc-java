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
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ClientConfig;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AddFilterTRpcConfigManagerCustomizerTest {

    private static final String FILTER_ONE = "filter1";

    private static final String FILTER_TWO = "filter2";

    private static final String FILTER_THREE = "filter3";

    private static final String FILTER_FOUR = "filter4";

    private static final String BACKEND_MAP_KEY = "backend";

    private static final Integer ORDER_VALUE = 1024;

    private static AddFilterTRpcConfigManagerCustomizer addFilterTRpcConfigManagerCustomizer;

    @Before
    public void setUp() {
        addFilterTRpcConfigManagerCustomizer = new AddFilterTRpcConfigManagerCustomizer();
    }

    @Test
    public void testConstructor() {
        Assert.assertNotNull(addFilterTRpcConfigManagerCustomizer);
    }

    @Test
    public void testAddClientFilters() {
        AddFilterTRpcConfigManagerCustomizer customizer = addFilterTRpcConfigManagerCustomizer.addClientFilters(
                FILTER_ONE, FILTER_TWO);
        Assert.assertEquals(addFilterTRpcConfigManagerCustomizer, customizer);
    }

    @Test
    public void testAddServerFilters() {
        AddFilterTRpcConfigManagerCustomizer addFilterTRpcConfigManagerCustomizer = new AddFilterTRpcConfigManagerCustomizer();
        AddFilterTRpcConfigManagerCustomizer customizer = addFilterTRpcConfigManagerCustomizer.addServerFilters(
                FILTER_ONE, FILTER_TWO);
        Assert.assertEquals(addFilterTRpcConfigManagerCustomizer, customizer);
    }

    @Test
    public void testCustomize() {
        ConfigManager instance = ConfigManager.getInstance();
        ClientConfig clientConfig = new ClientConfig();
        List<String> list = Arrays.asList(FILTER_ONE, FILTER_TWO);
        clientConfig.setFilters(list);
        instance.setClientConfig(clientConfig);
        addFilterTRpcConfigManagerCustomizer.customize(instance);
        List<String> filters = instance.getClientConfig().getFilters();

        Assert.assertEquals(filters.size(), list.size());
        Assert.assertEquals(list, instance.getClientConfig().getFilters());
    }

    @Test
    public void testGetOrder() {
        Assert.assertEquals(Integer.MAX_VALUE, addFilterTRpcConfigManagerCustomizer.getOrder());
        addFilterTRpcConfigManagerCustomizer = new TestAddFilterTRpcConfigManagerCustomizer();
        Assert.assertEquals((long) ORDER_VALUE, addFilterTRpcConfigManagerCustomizer.getOrder());
    }

    static final class TestAddFilterTRpcConfigManagerCustomizer extends AddFilterTRpcConfigManagerCustomizer {

        @Override
        public int getOrder() {
            return ORDER_VALUE;
        }
    }

    @Test
    public void testConstructorWithNullEmpty() {
        addFilterTRpcConfigManagerCustomizer.addClientFilters(FILTER_ONE, FILTER_TWO);
        addFilterTRpcConfigManagerCustomizer = new AddFilterTRpcConfigManagerCustomizer(null, null);

        ClientConfig clientConfig = new ClientConfig();
        BackendConfig backendConfig = new BackendConfig();

        backendConfig.setFilters(Arrays.asList(FILTER_THREE, FILTER_FOUR));
        clientConfig.getBackendConfigMap().put(BACKEND_MAP_KEY, backendConfig);

        ConfigManager configManager = ConfigManager.getInstance();
        configManager.setClientConfig(clientConfig);

        addFilterTRpcConfigManagerCustomizer.customize(configManager);
        List<String> expected = Arrays.asList(FILTER_THREE, FILTER_FOUR);
        Assert.assertEquals(expected.size(), backendConfig.getFilters().size());
        Assert.assertEquals(expected, backendConfig.getFilters());
    }

    @Test
    public void testMerge() {
        // add client filter
        addFilterTRpcConfigManagerCustomizer.addClientFilters(FILTER_ONE, FILTER_TWO);

        ClientConfig clientConfig = new ClientConfig();
        BackendConfig backendConfig = new BackendConfig();

        backendConfig.setFilters(Arrays.asList(FILTER_THREE, FILTER_FOUR));
        clientConfig.getBackendConfigMap().put(BACKEND_MAP_KEY, backendConfig);

        ConfigManager configManager = ConfigManager.getInstance();
        configManager.setClientConfig(clientConfig);

        // call customize method
        addFilterTRpcConfigManagerCustomizer.customize(configManager);

        List<String> expected = Arrays.asList(FILTER_ONE, FILTER_TWO, FILTER_THREE, FILTER_FOUR);
        Assert.assertEquals(expected, backendConfig.getFilters());

        expected = Arrays.asList(FILTER_ONE, FILTER_TWO, FILTER_THREE);
        Assert.assertNotEquals(expected, backendConfig.getFilters());
    }
}
