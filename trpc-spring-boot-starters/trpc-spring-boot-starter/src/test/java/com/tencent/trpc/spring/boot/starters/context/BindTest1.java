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

package com.tencent.trpc.spring.boot.starters.context;

import com.tencent.trpc.spring.boot.starters.context.configuration.TRpcConfigurationProperties;
import com.tencent.trpc.spring.boot.starters.test.SpringBootTestApplication;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootTestApplication.class, webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("bind-test1")
public class BindTest1 {


    @Autowired
    private TRpcConfigurationProperties properties;

    @Test
    @SuppressWarnings("unchecked")
    public void test() {
        Assert.assertEquals(properties.getServer().getApp(), "TestApp");
        List<Object> serviceList = (List<Object>) properties.getPlugins().getRegistry().get("polaris").get("service");
        Assert.assertNotNull(serviceList);
        Assert.assertEquals(serviceList.size(), 1);
    }
}
