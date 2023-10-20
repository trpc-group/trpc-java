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

package com.tencent.trpc.spring.context;

import com.google.common.collect.Lists;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.filter.FilterManager;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.spring.annotation.TRpcClient;
import com.tencent.trpc.spring.context.TRpcConfigAutoRegistryTest.TRpcConfigAutoRegistryTestConfiguration;
import com.tencent.trpc.spring.context.TRpcConfigAutoRegistryTest.TRpcConfigAutoRegistryTestInitializer;
import com.tencent.trpc.spring.context.configuration.TRpcConfigManagerCustomizer;
import com.tencent.trpc.spring.test.TRpcConfigManagerTestUtils;
import com.tencent.trpc.spring.test.TestSpringApplication;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestSpringApplication.class)
@ContextConfiguration(classes = {TRpcConfigAutoRegistryTestConfiguration.class,
        TRpcConfiguration.class}, initializers = TRpcConfigAutoRegistryTestInitializer.class)
@TestPropertySource(properties = {"test.my.flag=add", "test.reg.enabled=true"})
public class TRpcConfigAutoRegistryTest {

    private static final String TEST_URL = "ip://127.0.0.1:2333";

    @Autowired
    private InjectByFieldBean injectByFieldBean;
    @Autowired
    private InjectByConstructorBean injectByConstructorBean;
    @Autowired
    private InjectBySetterBean injectBySetterBean;
    @Autowired
    private MyTRpcConfigManagerCustomizer myTRpcConfigManagerCustomizer;
    @Autowired(required = false)
    @Qualifier("my.trpc.customized.client")
    private MyService customizedMyService;
    @Autowired
    private BeanFactory beanFactory;
    @Autowired
    private TestService myTestService1;
    @Autowired
    private TestService myTestService2;
    @Resource(name = "auto_inject_test_server_filter")
    private Filter autoInjectTestServerFilter;

    /**
     * Test tRPC client field-injection
     */
    @Test
    public void testTRpcClientAutoRegistryByField() {
        // test field
        Assert.assertNotNull(injectByFieldBean.getMyTestService());
        Assert.assertNotNull(injectByFieldBean.getMyTestService1());
        Assert.assertNotNull(injectByFieldBean.getMyTestService2());
        Assert.assertNotNull(injectByFieldBean.getMyTestServiceByResource());
        Assert.assertNotNull(injectByFieldBean.getTestService1());
        Assert.assertNotNull(injectByFieldBean.getTestService2());
        Assert.assertSame(injectByFieldBean.getTestService1(), injectByFieldBean.getMyTestService());
        Assert.assertSame(injectByFieldBean.getTestService1(), injectByFieldBean.getMyTestService1());
        Assert.assertSame(injectByFieldBean.getTestService2(), injectByFieldBean.getMyTestService2());
        Assert.assertSame(injectByFieldBean.getTestService2(), injectByFieldBean.getMyTestServiceByResource());
    }

    /**
     * Test tRPC client constructor-injection
     */
    @Test
    public void testTRpcClientAutoRegistry() {
        // test constructor
        Assert.assertNotNull(injectByConstructorBean.getMyTestService());
        Assert.assertNotNull(injectByConstructorBean.getMyTestService1());
        Assert.assertNotNull(injectByConstructorBean.getMyTestService2());
        Assert.assertNotNull(injectByConstructorBean.getTestService1());
        Assert.assertNotNull(injectByConstructorBean.getTestService2());
        Assert.assertSame(injectByConstructorBean.getTestService1(), injectByConstructorBean.getMyTestService());
        Assert.assertSame(injectByConstructorBean.getTestService1(), injectByConstructorBean.getMyTestService1());
        Assert.assertSame(injectByConstructorBean.getTestService2(), injectByConstructorBean.getMyTestService2());
    }

    /**
     * Test tRPC client setter-injection
     */
    @Test
    public void testTRpcClientAutoRegistryBySetter() {
        // test setter
        Assert.assertNotNull(injectBySetterBean.getMyTestService());
        Assert.assertNotNull(injectBySetterBean.getMyTestService1());
        Assert.assertNotNull(injectBySetterBean.getMyTestService2());
        Assert.assertNotNull(injectBySetterBean.getTestService1());
        Assert.assertNotNull(injectBySetterBean.getTestService2());
        Assert.assertSame(injectBySetterBean.getTestService1(), injectBySetterBean.getMyTestService());
        Assert.assertSame(injectBySetterBean.getTestService1(), injectBySetterBean.getMyTestService1());
        Assert.assertSame(injectBySetterBean.getTestService2(), injectBySetterBean.getMyTestService2());
        Assert.assertSame(injectBySetterBean.getTestService2(), injectBySetterBean.getMyTestServiceByResource());
    }

    /**
     * Custom tRPC clients should not be registered as spring beans
     */
    @Test
    public void testNotAutoRegistry() {
        Assert.assertNull(customizedMyService);
        Assert.assertTrue(beanFactory.containsBean("myTRpcConfigManagerCustomizer"));
        Assert.assertFalse(beanFactory.containsBean("my.trpc.customized.client"));
        Assert.assertEquals(1, beanFactory.getBeanProvider(MyService.class).stream().count());
        Assert.assertTrue(myTRpcConfigManagerCustomizer.isEnabled());
        Assert.assertEquals("add", myTRpcConfigManagerCustomizer.getFlag());
    }

    /**
     * Test TRpc filter with auto inject feature.
     */
    @Test
    public void testTRpcFilterAutoRegistry() {
        AutoInjectTestClientFilter clientFilter = (AutoInjectTestClientFilter) FilterManager.get(
                "auto_inject_test_client_filter");
        Assert.assertNotNull(clientFilter.getMyTestService1());
        Assert.assertNotNull(clientFilter.getMyTestService2());
        Assert.assertSame(clientFilter.getMyTestService1(), myTestService1);
        Assert.assertSame(clientFilter.getMyTestService2(), myTestService2);

        AutoInjectTestServerFilter serverFilter = (AutoInjectTestServerFilter) FilterManager.get(
                "auto_inject_test_server_filter");
        Assert.assertSame(serverFilter, autoInjectTestServerFilter);
        Assert.assertNotNull(serverFilter.getInjectByFieldBean());
        Assert.assertNotNull(serverFilter.getAutowiredByFieldBean());
        Assert.assertNotNull(serverFilter.getInjectBySetterBean());
        Assert.assertSame(serverFilter.getInjectByFieldBean(), injectByFieldBean);
        Assert.assertSame(serverFilter.getAutowiredByFieldBean(), injectByFieldBean);
        Assert.assertSame(serverFilter.getInjectBySetterBean(), injectBySetterBean);
    }

    @TRpcService(name = "test_trpc_config_registry")
    public interface TestService {

        @TRpcMethod(name = "test_method")
        Object test(RpcContext context, Object req);
    }

    @TRpcService(name = "my_trpc_config_registry")
    public interface MyService {

        @TRpcMethod(name = "call_method")
        Object call(RpcContext context, Object req);
    }

    public static class TRpcConfigAutoRegistryTestInitializer implements
            ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TRpcConfigManagerTestUtils.resetConfigManager();
            final ConfigManager configManager = ConfigManager.getInstance();
            BackendConfig backendConfig1 = new BackendConfig();
            backendConfig1.setName("myTestService1");
            backendConfig1.setNamingUrl(TEST_URL);
            backendConfig1.setServiceInterface(TestService.class);
            backendConfig1.setFilters(
                    Lists.newArrayList("additional_server_filter", "auto_inject_test_client_filter"));
            configManager.getClientConfig().addBackendConfig(backendConfig1);

            BackendConfig backendConfig2 = new BackendConfig();
            backendConfig2.setName("myTestService2");
            backendConfig2.setNamingUrl(TEST_URL);
            backendConfig2.setServiceInterface(TestService.class);
            configManager.getClientConfig().addBackendConfig(backendConfig2);

            BackendConfig backendConfig3 = new BackendConfig();
            backendConfig3.setName("my.trpc.my.client");
            backendConfig3.setNamingUrl(TEST_URL);
            backendConfig3.setServiceInterface(MyService.class);
            backendConfig3.setFilters(
                    Lists.newArrayList("auto_inject_test_server_filter", "auto_inject_test_client_filter"));
            configManager.getClientConfig().addBackendConfig(backendConfig3);

            configManager.getServerConfig().setFilters(Lists.newArrayList("auto_inject_test_server_filter"));
            configManager.getClientConfig().setFilters(Lists.newArrayList("auto_inject_test_client_filter"));
        }
    }

    public static class MyTRpcConfigManagerCustomizer implements TRpcConfigManagerCustomizer {

        private String flag;

        @Value("${test.reg.enabled:false}")
        private boolean enabled;

        public String getFlag() {
            return flag;
        }

        public void setFlag(String flag) {
            this.flag = flag;
        }

        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void customize(ConfigManager configManager) {
            BackendConfig backendConfig3 = new BackendConfig();
            backendConfig3.setName("my.trpc.customized.client");
            backendConfig3.setNamingUrl(TEST_URL);
            backendConfig3.setServiceInterface(MyService.class);
            configManager.getClientConfig().addBackendConfig(backendConfig3);
        }

    }

    @Configuration
    public static class TRpcConfigAutoRegistryTestConfiguration implements BeanDefinitionRegistryPostProcessor {

        @Bean("myTRpcConfigManagerCustomizer")
        @ConfigurationProperties(prefix = "test.my")
        public TRpcConfigManagerCustomizer myTRpcConfigManagerCustomizer() {
            return new MyTRpcConfigManagerCustomizer();
        }

        @Bean
        public InjectByFieldBean injectByFieldBean() {
            return new InjectByFieldBean();
        }

        @Bean
        public InjectBySetterBean injectBySetterBean() {
            return new InjectBySetterBean();
        }

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            registry.registerBeanDefinition("injectByConstructorBean",
                    BeanDefinitionBuilder.genericBeanDefinition(InjectByConstructorBean.class)
                            .getBeanDefinition());
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

        }

    }

    /**
     * Test bean for field injection
     */
    public static class InjectByFieldBean {

        @TRpcClient(id = "myTestService1")
        private TestService testService1;

        @TRpcClient(id = "myTestService2")
        private TestService testService2;

        @Autowired
        private TestService myTestService1;

        @Autowired
        private TestService myTestService2;

        @Autowired
        @Qualifier("myTestService1")
        private TestService myTestService;

        @Resource(name = "myTestService2")
        private TestService myTestServiceByResource;

        public TestService getTestService1() {
            return testService1;
        }

        public TestService getTestService2() {
            return testService2;
        }

        public TestService getMyTestService1() {
            return myTestService1;
        }

        public TestService getMyTestService2() {
            return myTestService2;
        }

        public TestService getMyTestService() {
            return myTestService;
        }

        public TestService getMyTestServiceByResource() {
            return myTestServiceByResource;
        }
    }

    /**
     * Test bean for constructor injection
     */
    public static class InjectByConstructorBean {

        @TRpcClient(id = "myTestService1")
        private TestService testService1;

        @TRpcClient(id = "myTestService2")
        private TestService testService2;

        private TestService myTestService1;

        private TestService myTestService2;

        private TestService myTestService;

        public InjectByConstructorBean() {
        }

        public InjectByConstructorBean(TestService testService1, TestService testService2) {
            this.testService1 = testService1;
            this.testService2 = testService2;
        }

        @Autowired
        public InjectByConstructorBean(TestService myTestService1, TestService myTestService2,
                @Qualifier("myTestService1") TestService myTestService) {
            this.myTestService1 = myTestService1;
            this.myTestService2 = myTestService2;
            this.myTestService = myTestService;
        }

        public TestService getTestService1() {
            return testService1;
        }

        public TestService getTestService2() {
            return testService2;
        }

        public TestService getMyTestService1() {
            return myTestService1;
        }

        public TestService getMyTestService2() {
            return myTestService2;
        }

        public TestService getMyTestService() {
            return myTestService;
        }
    }

    /**
     * Test bean for setter injection
     */
    public static class InjectBySetterBean {

        @TRpcClient(id = "myTestService1")
        private TestService testService1;

        @TRpcClient(id = "myTestService2")
        private TestService testService2;

        private TestService myTestService1;
        private TestService myTestService2;
        private TestService myTestService;
        private TestService myTestServiceByResource;

        public TestService getTestService1() {
            return testService1;
        }

        public void setTestService1(TestService testService1) {
            this.testService1 = testService1;
        }

        public TestService getTestService2() {
            return testService2;
        }

        public void setTestService2(TestService testService2) {
            this.testService2 = testService2;
        }

        public TestService getMyTestService1() {
            return myTestService1;
        }

        @Autowired
        public void setMyTestService1(TestService myTestService1) {
            this.myTestService1 = myTestService1;
        }

        public TestService getMyTestService2() {
            return myTestService2;
        }

        @Autowired
        public void setMyTestService2(TestService myTestService2) {
            this.myTestService2 = myTestService2;
        }

        public TestService getMyTestService() {
            return myTestService;
        }

        @Autowired
        @Qualifier("myTestService1")
        public void setMyTestService(TestService myTestService) {
            this.myTestService = myTestService;
        }

        public TestService getMyTestServiceByResource() {
            return myTestServiceByResource;
        }

        @Resource(name = "myTestService2")
        public void setMyTestServiceByResource(TestService myTestServiceByResource) {
            this.myTestServiceByResource = myTestServiceByResource;
        }
    }

}
