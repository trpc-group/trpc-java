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

package com.tencent.trpc.core.common;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;

import com.tencent.trpc.core.common.Lifecycle.LifecycleListener;
import com.tencent.trpc.core.common.Lifecycle.LifecycleState;
import com.tencent.trpc.core.exception.LifecycleException;
import com.tencent.trpc.core.selector.ReflectionUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

public class LifeCycleTest {

    @Test
    public void test() throws Exception {
        LifeCycleObj obj = new LifeCycleObj();
        LifecycleListener listenerMock = PowerMockito.mock(LifecycleListener.class);
        obj.addListener(listenerMock);
        assertSame(obj.getState(), LifecycleState.NEW);
        // init exception
        obj.TEST_FAIL.set(true);
        try {
            obj.init();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        assertSame(obj.getState(), LifecycleState.FAILED);
        Mockito.verify(listenerMock, times(1)).onInitializing(obj);
        Mockito.verify(listenerMock, times(1)).onStopping(obj);
        Mockito.reset(listenerMock);
        // init normal
        obj.TEST_FAIL.set(false);
        ReflectionUtils.setField(obj, LifecycleBase.class.getDeclaredField("state"),
                LifecycleState.NEW);
        obj.init();
        assertTrue(obj.getState() == LifecycleState.INITIALIZED);
        Mockito.verify(listenerMock, times(1)).onInitializing(obj);
        Mockito.verify(listenerMock, times(0)).onStopping(obj);
        Mockito.reset(listenerMock);
        // start exception
        obj.TEST_FAIL.set(true);
        ReflectionUtils.setField(obj, LifecycleBase.class.getDeclaredField("state"),
                LifecycleState.INITIALIZED);
        try {
            obj.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        assertTrue(obj.getState() == LifecycleState.FAILED);
        Mockito.verify(listenerMock, times(1)).onStarting(obj);
        Mockito.verify(listenerMock, times(1)).onStopping(obj);
        Mockito.reset(listenerMock);
        // start noraml
        obj.TEST_FAIL.set(false);
        ReflectionUtils.setField(obj, LifecycleBase.class.getDeclaredField("state"),
                LifecycleState.INITIALIZED);
        obj.start();
        assertTrue(obj.getState() == LifecycleState.STARTED);
        assertTrue(obj.isStarted());
        Mockito.verify(listenerMock, times(1)).onStarting(obj);
        Mockito.verify(listenerMock, times(0)).onStopping(obj);
    }

    public static final class LifeCycleObj extends LifecycleBase {

        public static final AtomicBoolean TEST_FAIL = new AtomicBoolean(false);

        @Override
        protected void startInternal() throws Exception {
            super.startInternal();
            if (TEST_FAIL.get()) {
                throw new Exception("");
            }
        }

        @Override
        protected void stopInternal() throws Exception {
            super.stopInternal();
            if (TEST_FAIL.get()) {
                throw new Exception("");
            }
        }

        @Override
        protected void initInternal() throws Exception {
            super.initInternal();
            if (TEST_FAIL.get()) {
                throw new LifecycleException("");
            }
        }
    }
}