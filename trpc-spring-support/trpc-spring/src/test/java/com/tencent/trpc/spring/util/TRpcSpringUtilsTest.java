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

package com.tencent.trpc.spring.util;

import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.spring.test.TestSpringApplication;
import com.tencent.trpc.spring.util.TRpcSpringUtilsTest.BeanConfiguration;
import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestSpringApplication.class)
@ContextConfiguration(classes = BeanConfiguration.class)
public class TRpcSpringUtilsTest {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    private static final String NOT_APPLICATION_ID = "notApplication";

    private static final String APPLICATION_ID = "application";

    private static final String SPRING_APPLICATION_NAME_KEY = "spring.application.name";

    private static final String SPRING_APPLICATION_NAME_VALUE = "notApplicationName";

    private static final String CUSTOM_PROPERTY_SOURCE = "customPropertySource";

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

    @Test
    public void testIsAwareContext() {
        Assert.assertTrue(TRpcSpringUtils.isAwareContext(applicationContext));

        // test id is not application
        applicationContext.setId(NOT_APPLICATION_ID);
        Assert.assertFalse(TRpcSpringUtils.isAwareContext(applicationContext));

        // test id is application
        applicationContext.setId(APPLICATION_ID);
        Assert.assertTrue(TRpcSpringUtils.isAwareContext(applicationContext));

        // test id is application and spring.application.name is not null
        applicationContext.setId(SPRING_APPLICATION_NAME_VALUE);
        setApplicationName();
        String name = applicationContext.getEnvironment().getProperty(SPRING_APPLICATION_NAME_KEY);
        Assert.assertEquals(SPRING_APPLICATION_NAME_VALUE, name);
        Assert.assertTrue(TRpcSpringUtils.isAwareContext(applicationContext));

    }

    private void setApplicationName() {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        MapPropertySource propertySource = new MapPropertySource(CUSTOM_PROPERTY_SOURCE,
                Collections.singletonMap(SPRING_APPLICATION_NAME_KEY, SPRING_APPLICATION_NAME_VALUE));
        environment.getPropertySources().addFirst(propertySource);
    }

    @Test
    public void testIsAwareContextWithEmpty() {
        // test id is null
        applicationContext.setId(StringUtils.EMPTY);
        boolean context = TRpcSpringUtils.isAwareContext(applicationContext);
        Assert.assertFalse(context);
        applicationContext.setId(null);
        context = TRpcSpringUtils.isAwareContext(applicationContext);
        Assert.assertFalse(context);
    }
}
