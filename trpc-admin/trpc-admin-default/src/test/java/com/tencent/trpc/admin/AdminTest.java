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

package com.tencent.trpc.admin;

import com.tencent.trpc.core.admin.spi.AdminService;
import com.tencent.trpc.core.common.config.AdminConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.exception.LifecycleException;
import com.tencent.trpc.core.extension.ExtensionClass;
import com.tencent.trpc.core.extension.ExtensionLoader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminTest {

    private static final String LOCAL_HOST = "127.0.0.1";

    @After
    public void after() {
        ExtensionLoader.destroyAllPlugin();
    }

    @Test
    public void testSuccess() throws InterruptedException {
        AdminService adminService = getAdminService(LOCAL_HOST, 8080);
        adminService.stop();
    }

    @Test
    public void testStartFail() {
        AdminService adminService1 = null;
        AdminService adminService2 = null;
        Exception exception = null;
        try {
            adminService1 = getAdminService(LOCAL_HOST, 8080);
            adminService2 = getAdminService(LOCAL_HOST, 8080);
        } catch (Exception e) {
            exception = e;
        } finally {
            if (adminService1 != null) {
                adminService1.stop();
            }
            if (adminService2 != null) {
                adminService2.stop();
            }
        }
        Assert.assertTrue(exception != null && exception instanceof LifecycleException);
    }

    private AdminService getAdminService(String ip, int adminPort) {
        ServerConfig serverConfig = new ServerConfig();
        AdminConfig adminConfig = new AdminConfig();
        serverConfig.setAdminConfig(adminConfig);
        adminConfig.setAdminPort(adminPort);
        adminConfig.setAdminIp(ip);
        ExtensionLoader<AdminService> extensionLoader =
                ExtensionLoader.getExtensionLoader(AdminService.class);
        ExtensionClass<AdminService> extensionClass =
                extensionLoader.getExtensionClass("defaultAdminService");
        AdminService adminService = extensionClass.getExtInstance();
        adminService.setServerConfig(serverConfig);
        adminService.init();
        adminService.start();
        return adminService;
    }

}
