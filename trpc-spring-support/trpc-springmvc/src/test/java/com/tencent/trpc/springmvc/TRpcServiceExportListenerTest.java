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

package com.tencent.trpc.springmvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tencent.trpc.spring.context.TRpcLifecycleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;

public class TRpcServiceExportListenerTest {

    private StubHandlerMapping handlerMapping;

    private StubLifecycleManager lifecycleManager;

    private GenericApplicationContext context;

    @BeforeEach
    public void setUp() {
        handlerMapping = new StubHandlerMapping();
        lifecycleManager = new StubLifecycleManager();
        context = new GenericApplicationContext();
    }

    @Test
    public void testNullHandlerMapping() {
        assertThrows(NullPointerException.class, () -> new TRpcServiceExportListener(null, lifecycleManager));
    }

    @Test
    public void testGetOrder() {
        TRpcServiceExportListener listener = new TRpcServiceExportListener(handlerMapping, lifecycleManager);
        assertEquals(TRpcServiceExportListener.EXPORT_ORDER, listener.getOrder());
        assertTrue(TRpcServiceExportListener.EXPORT_ORDER > TRpcLifecycleManager.START_ORDER,
                "must export after tRPC started");
    }

    @Test
    public void testExportAfterStarted() {
        lifecycleManager.managed = true;
        lifecycleManager.started = true;
        new TRpcServiceExportListener(handlerMapping, lifecycleManager)
                .onApplicationEvent(new ContextRefreshedEvent(context));
        assertEquals(1, handlerMapping.exportCount);
    }

    @Test
    public void testSkipWithoutLifecycleManager() {
        new TRpcServiceExportListener(handlerMapping, null)
                .onApplicationEvent(new ContextRefreshedEvent(context));
        assertEquals(0, handlerMapping.exportCount);
    }

    @Test
    public void testSkipNotManagedContext() {
        lifecycleManager.managed = false;
        lifecycleManager.started = true;
        new TRpcServiceExportListener(handlerMapping, lifecycleManager)
                .onApplicationEvent(new ContextRefreshedEvent(context));
        assertEquals(0, handlerMapping.exportCount);
    }

    @Test
    public void testSkipNotStarted() {
        lifecycleManager.managed = true;
        lifecycleManager.started = false;
        new TRpcServiceExportListener(handlerMapping, lifecycleManager)
                .onApplicationEvent(new ContextRefreshedEvent(context));
        assertEquals(0, handlerMapping.exportCount);
    }

    @Test
    public void testAutoConfiguration() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("tRpcLifecycleManager", lifecycleManager);
        TRpcServiceExportListener listener = new TRpcSpringMvcAutoConfiguration()
                .tRpcServiceExportListener(handlerMapping, beanFactory.getBeanProvider(TRpcLifecycleManager.class));
        assertNotNull(listener);

        lifecycleManager.managed = true;
        lifecycleManager.started = true;
        listener.onApplicationEvent(new ContextRefreshedEvent(context));
        assertEquals(1, handlerMapping.exportCount);
    }

    @Test
    public void testAutoConfigurationWithoutLifecycleManager() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        TRpcServiceExportListener listener = new TRpcSpringMvcAutoConfiguration()
                .tRpcServiceExportListener(handlerMapping, beanFactory.getBeanProvider(TRpcLifecycleManager.class));
        listener.onApplicationEvent(new ContextRefreshedEvent(context));
        assertEquals(0, handlerMapping.exportCount);
    }

    private static class StubHandlerMapping extends TRpcServiceHandlerMapping {

        private int exportCount;

        @Override
        public void exportTRpcServices() {
            exportCount++;
        }
    }

    private static class StubLifecycleManager extends TRpcLifecycleManager {

        private boolean managed;

        private boolean started;

        @Override
        public boolean isManagedContext(ApplicationContext context) {
            return managed;
        }

        @Override
        public boolean isStarted() {
            return started;
        }
    }
}
