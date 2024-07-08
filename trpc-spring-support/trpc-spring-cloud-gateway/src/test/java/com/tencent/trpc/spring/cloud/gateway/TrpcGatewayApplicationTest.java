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

package com.tencent.trpc.spring.cloud.gateway;

import com.tencent.trpc.spring.cloud.gateway.service.MyService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TrpcGatewayApplication.class)
public class TrpcGatewayApplicationTest {

    @Autowired
    private MyService myService;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testContextLoads() {
        // 测试上下文加载
        Assert.assertNotNull(applicationContext);
    }

    @Test
    public void testComponentBehavior() {
        Assert.assertNotNull(myService);
        Assert.assertTrue(myService.invoke());
    }
}
