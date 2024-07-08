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

package com.tencent.trpc.spring.context;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.spring.util.TRpcSpringUtils;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;

@PowerMockIgnore({"javax.management.*"})
@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigManager.class, TRpcSpringUtils.class})
public class TRpcLifecycleManagerTest {

    @Mock
    private ConfigManager mockConfigManager;

    @Mock
    private ConfigurableApplicationContext context;

    TRpcLifecycleManager tRpcLifecycleManager;

    private static final String SERVICE_FILTER_THREE = "additional_server_filter";

    private static final String SERVICE_FILTER_FOUR = "auto_inject_test_server_filter";

    private static final String SERVICE_BACKEND_MAP_KEY = "service-backend";

    private static final String SERVICE_CONFIG_NAME = "service_config_name";

    @Before
    public void setUp() {
        tRpcLifecycleManager = new TRpcLifecycleManager();
        PowerMockito.mockStatic(ConfigManager.class);
        when(ConfigManager.getInstance()).thenReturn(mockConfigManager);
        context = mock(ConfigurableApplicationContext.class);
        PowerMockito.mockStatic(TRpcSpringUtils.class);
    }

    @Test
    public void testSupportsEventType() {
        Assert.assertTrue(tRpcLifecycleManager.supportsEventType(ContextRefreshedEvent.class));
        Assert.assertTrue(tRpcLifecycleManager.supportsEventType(ContextClosedEvent.class));
        Assert.assertFalse(tRpcLifecycleManager.supportsEventType(ApplicationContextEvent.class));
    }

    @Test
    public void testOnContextRefreshedEvent() {
        ContextRefreshedEvent contextRefreshedEvent = new ContextRefreshedEvent(context);

        // test ContextRefreshedEvent logic
        when(TRpcSpringUtils.isAwareContext(context)).thenReturn(false);
        tRpcLifecycleManager.onApplicationEvent(contextRefreshedEvent);
        verify(mockConfigManager, times(0)).start(false);
        verify(mockConfigManager, times(0)).start(true);

        when(TRpcSpringUtils.isAwareContext(context)).thenReturn(true);
        ServerConfig serverConfig = new ServerConfig();
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setName(SERVICE_CONFIG_NAME);
        serviceConfig.setFilters(Arrays.asList(SERVICE_FILTER_THREE, SERVICE_FILTER_FOUR));
        serverConfig.getServiceMap().put(SERVICE_BACKEND_MAP_KEY, serviceConfig);
        when(mockConfigManager.getServerConfig()).thenReturn(serverConfig);
        tRpcLifecycleManager.onApplicationEvent(contextRefreshedEvent);
        verify(mockConfigManager, times(1)).start(false);

        // test ContextClosedEvent logic
        ContextClosedEvent contextClosedEvent = new ContextClosedEvent(context);
        verify(mockConfigManager, times(0)).stop();

        when(TRpcSpringUtils.isAwareContext(context)).thenReturn(true);
        tRpcLifecycleManager.onApplicationEvent(contextClosedEvent);
        verify(mockConfigManager, times(1)).stop();

    }

    @Test
    public void testGetOrder() {
        Assert.assertEquals(Ordered.HIGHEST_PRECEDENCE + 20000, tRpcLifecycleManager.getOrder());
    }

}
