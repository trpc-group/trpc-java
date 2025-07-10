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

package com.tencent.trpc.support.constant;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import org.junit.Test;
import java.lang.reflect.Constructor;

public class ConsulConstantTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulConstantTest.class);

    @Test
    public void testCkvConstant() {
        Class<ConsulConstant> consulConstantClass = ConsulConstant.class;
        try {
            Constructor<ConsulConstant> constructor = consulConstantClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        } catch (Exception e) {
            LOGGER.warn("newInstance warn {}", e);
        }
    }
}