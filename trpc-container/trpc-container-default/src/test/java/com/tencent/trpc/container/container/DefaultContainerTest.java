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

package com.tencent.trpc.container.container;

import com.tencent.trpc.core.container.spi.Container;
import com.tencent.trpc.core.extension.ExtensionLoader;
import org.junit.Before;
import org.junit.Test;

public class DefaultContainerTest {

    Container container;

    @Before
    public void start() {
        container =
                ExtensionLoader.getExtensionLoader(Container.class).getExtension("default");

    }

    @Test
    public void testStart() {
        container.start();
    }

    @Test
    public void testStop() throws InterruptedException {
        Thread.sleep(1000);
        container.stop();
    }

}
