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

package com.tencent.trpc.spring.util;

import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.spring.test.TestSpringApplication;
import com.tencent.trpc.spring.util.TRpcSpringUtilsTest.BeanConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestSpringApplication.class)
@ContextConfiguration(classes = BeanConfiguration.class)
public class TRpcSpringUtilsTest {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Test
    public void testSetRef() throws Exception {
        ApplicationContext context = new SpringApplicationBuilder()
                .parent(applicationContext)
                .sources(ChildContextConfiguration.class)
                .build()
                .run();

        ProviderConfig<TestService> config = new ProviderConfig<>();
        config.setRefClazz(TestServiceImpl.class.getName());

        TRpcSpringUtils.setRef(context, config);

        Assert.assertEquals(TestService.class, config.getServiceInterface());
        Assert.assertEquals(context.getBean(TestService.class), config.getRef());
    }

    @TRpcService(name = "testService")
    private interface TestService {

        @TRpcMethod(name = "call")
        Object call(RpcContext context, Object req);
    }

    static class TestServiceImpl implements TestService {

        @Override
        public Object call(RpcContext context, Object req) {
            return null;
        }
    }

    @TestConfiguration
    static class BeanConfiguration {

        @Bean
        public TestService testService() {
            return new TestServiceImpl();
        }
    }

    @TestConfiguration
    static class ChildContextConfiguration {

    }
}
