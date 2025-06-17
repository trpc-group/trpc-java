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

package com.tencent.trpc.spring.boot.starters.annotation;

import com.tencent.trpc.core.filter.FilterManager;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.spring.boot.starters.annotation.EnableAnnotationTest.EnableAnnotationTestConfiguration;
import com.tencent.trpc.spring.boot.starters.test.AutoInjectTestFilter;
import com.tencent.trpc.spring.boot.starters.test.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.spring.boot.starters.test.HelloRequestProtocol.HelloResponse;
import com.tencent.trpc.spring.boot.starters.test.SpringBootTestApplication;
import jakarta.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringBootTestApplication.class, webEnvironment = WebEnvironment.NONE)
@ContextConfiguration(classes = EnableAnnotationTestConfiguration.class)
public class EnableAnnotationTest {

    @Autowired
    private DemoServiceImpl demoService;
    @Resource(name = "auto_inject_test_filter")
    private Filter autoInjectTestFilter;

    @Test
    public void contextLoad() {

        HelloResponse hello = demoService.sayHello(HelloRequest.newBuilder().setMessage("hello").build());

        Assert.assertEquals("hello", hello.getMessage());
        Assert.assertNotNull(demoService.getGreeterService1Bean());
        Assert.assertNotNull(demoService.getGreeterService2Bean());
        Assert.assertNotNull(demoService.getMyTestServer());
        Assert.assertSame(demoService.getGreeterService(), demoService.getGreeterService1Bean());
        Assert.assertSame(demoService.getGreeterService2(), demoService.getGreeterService2Bean());
        Assert.assertSame(demoService.getMyTestServerClient(), demoService.getMyTestServer());
    }

    @Test
    public void testFilterInject() {
        AutoInjectTestFilter filter = (AutoInjectTestFilter) FilterManager.get("auto_inject_test_filter");
        Assert.assertSame(filter, autoInjectTestFilter);
        Assert.assertNotNull(filter.getGreeterService());
        Assert.assertNotNull(filter.getGreeterService2());
        Assert.assertNotNull(filter.getMyTestServer());
        Assert.assertSame(filter.getGreeterService(), demoService.getGreeterService1Bean());
        Assert.assertSame(filter.getGreeterService2(), demoService.getGreeterService2Bean());
        Assert.assertSame(filter.getMyTestServer(), demoService.getMyTestServer());
    }

    @Configuration
    @ComponentScan
    public static class EnableAnnotationTestConfiguration {

    }
}
