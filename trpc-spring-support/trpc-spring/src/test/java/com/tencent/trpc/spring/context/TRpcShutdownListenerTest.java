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

package com.tencent.trpc.spring.context;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;

public class TRpcShutdownListenerTest {

    private StubLifecycleManager lifecycleManager;

    private TRpcShutdownListener listener;

    private GenericApplicationContext context;

    @Before
    public void setUp() {
        lifecycleManager = new StubLifecycleManager();
        listener = new TRpcShutdownListener(lifecycleManager);
        context = new GenericApplicationContext();
    }

    @Test(expected = NullPointerException.class)
    public void testNullLifecycleManager() {
        new TRpcShutdownListener(null);
    }

    @Test
    public void testGetOrder() {
        Assert.assertEquals(TRpcShutdownListener.STOP_ORDER, listener.getOrder());
        Assert.assertEquals(Ordered.HIGHEST_PRECEDENCE + 20000, TRpcShutdownListener.STOP_ORDER);
    }

    @Test
    public void testStopOnManagedContextClosed() {
        lifecycleManager.managed = true;
        listener.onApplicationEvent(new ContextClosedEvent(context));
        Assert.assertEquals(1, lifecycleManager.stopCount);
    }

    @Test
    public void testIgnoreNotManagedContextClosed() {
        lifecycleManager.managed = false;
        listener.onApplicationEvent(new ContextClosedEvent(context));
        Assert.assertEquals(0, lifecycleManager.stopCount);
    }

    private static class StubLifecycleManager extends TRpcLifecycleManager {

        private boolean managed;

        private int stopCount;

        @Override
        public boolean isManagedContext(ApplicationContext context) {
            return managed;
        }

        @Override
        public void stopIfStarted() {
            stopCount++;
        }
    }
}
