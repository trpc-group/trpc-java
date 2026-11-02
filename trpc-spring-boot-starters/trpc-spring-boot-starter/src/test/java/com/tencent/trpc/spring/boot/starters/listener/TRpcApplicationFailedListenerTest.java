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

package com.tencent.trpc.spring.boot.starters.listener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.tencent.trpc.spring.context.TRpcLifecycleManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

public class TRpcApplicationFailedListenerTest {

    private final TRpcApplicationFailedListener listener = new TRpcApplicationFailedListener();

    @Test
    public void testNullContext() {
        assertDoesNotThrow(() -> listener.onApplicationEvent(newEvent(null)));
    }

    @Test
    public void testBeanFactoryNotAvailable() {
        // bean factory of a not refreshed refreshable context is not available
        ConfigurableApplicationContext context = new ClassPathXmlApplicationContext();
        assertDoesNotThrow(() -> listener.onApplicationEvent(newEvent(context)));
    }

    @Test
    public void testStopExistingLifecycleManager() {
        CountingLifecycleManager lifecycleManager = new CountingLifecycleManager();
        GenericApplicationContext context = new GenericApplicationContext();
        context.getBeanFactory().registerSingleton("tRpcLifecycleManager", lifecycleManager);

        listener.onApplicationEvent(newEvent(context));
        assertEquals(1, lifecycleManager.stopCount);
    }

    @Test
    public void testNotCreateLifecycleManager() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("tRpcLifecycleManager", TRpcLifecycleManager.class);

        listener.onApplicationEvent(newEvent(context));
        assertFalse(context.getBeanFactory().containsSingleton("tRpcLifecycleManager"));
    }

    private static ApplicationFailedEvent newEvent(ConfigurableApplicationContext context) {
        return new ApplicationFailedEvent(new SpringApplication(), new String[0], context,
                new RuntimeException("startup failed"));
    }

    private static class CountingLifecycleManager extends TRpcLifecycleManager {

        private int stopCount;

        @Override
        public void stopIfStarted() {
            stopCount++;
        }
    }
}
