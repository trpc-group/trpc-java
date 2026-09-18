package com.tencent.trpc.core.common;

import org.junit.Test;

public class LifecycleBaseTest {

    @Test
    public void testDebug() {
        TestLifecycle testLifecycle = new TestLifecycle();
        testLifecycle.init();
    }

    public class TestLifecycle extends LifecycleBase {

    }
}