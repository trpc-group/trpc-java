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

package com.tencent.trpc.registry.common;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;


/**
 * Test class for constants.
 */
public class ConstantsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConstantsTest.class);

    @Test
    public void testCkvConstant() {
        Class<Constants> constantsClass = Constants.class;
        try {
            Constructor<Constants> constructor = constantsClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        } catch (Exception e) {
            LOGGER.warn("newInstance warn {}", e);
        }

        Class<ConfigConstants> configConstantsClass = ConfigConstants.class;
        try {
            Constructor<ConfigConstants> constructor = configConstantsClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        } catch (Exception e) {
            LOGGER.warn("newInstance warn {}", e);
        }
    }
}