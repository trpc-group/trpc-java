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

package com.tencent.trpc.core.management.support;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class MBeanRegistryHelper {

    private static final Logger logger = LoggerFactory.getLogger(MBeanRegistryHelper.class);

    /**
     * Register mbean
     *
     * @param object mbean
     * @param objectName mbean  object name {@link ObjectName}
     */
    public static void registerMBean(Object object, ObjectName objectName) {
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            mBeanServer.registerMBean(object, objectName);
        } catch (Exception e) {
            logger.warn("the instance already exists exception: ", e);
        }
    }

}