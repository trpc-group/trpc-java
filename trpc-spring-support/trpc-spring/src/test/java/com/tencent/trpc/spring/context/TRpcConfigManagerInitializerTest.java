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

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.spring.annotation.TRpcClient;
import com.tencent.trpc.spring.context.TRpcConfigManagerInitializerTest.BeanConfiguration;
import com.tencent.trpc.spring.context.TRpcConfigManagerInitializerTest.InitConfigManagerApplicationContextInitializer;
import com.tencent.trpc.spring.context.configuration.AddFilterTRpcConfigManagerCustomizer;
import com.tencent.trpc.spring.context.configuration.TRpcConfigManagerCustomizer;
import com.tencent.trpc.spring.test.TRpcConfigManagerTestUtils;
import com.tencent.trpc.spring.test.TestSpringApplication;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestSpringApplication.class)
@ContextConfiguration(classes = {BeanConfiguration.class, TRpcConfiguration.class},
        initializers = InitConfigManagerApplicationContextInitializer.class)
public class TRpcConfigManagerInitializerTest {

    private static final String SERVICE_ID = "test";

    private static final String CLIENT_ID = "client";

    private static final String CUSTOMIZED_NAMING_URL = "ip://127.0.0.1:2333";

    private static final String ADDITIONAL_CLIENT_FILTER = "additional_client_filter";

    private static final String ADDITIONAL_SERVER_FILTER = "additional_server_filter";

    @Autowired
    private ServiceHolder serviceHolder;

    @Test
    public void test() {
        BackendConfig backendConfig = ConfigManager.getInstance().getClientConfig().getBackendConfigMap()
                .get(CLIENT_ID);
        Assert.assertNotNull(backendConfig);
        Assert.assertEquals(backendConfig.getNamingUrl(), CUSTOMIZED_NAMING_URL);
        Assert.assertFalse(backendConfig.getBatchDecoder());
        Assert.assertTrue(backendConfig.getFilters().contains(ADDITIONAL_CLIENT_FILTER));

        ServiceConfig serviceConfig = ConfigManager.getInstance().getServerConfig().getServiceMap().get(SERVICE_ID);
        Assert.assertNotNull(serviceConfig);
        Assert.assertTrue(serviceConfig.getFilters().contains(ADDITIONAL_SERVER_FILTER));
    }

    @TRpcService(name = SERVICE_ID)
    interface TestService {

        @TRpcMethod(name = "test-method")
        Object call(RpcContext context, Object req);
    }

    @Service
    static class TestServiceImpl implements TestService {

        @Override
        public Object call(RpcContext context, Object req) {
            return null;
        }
    }

    public static class InitConfigManagerApplicationContextInitializer implements
            ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TRpcConfigManagerTestUtils.resetConfigManager();
            final BackendConfig backendConfig = new BackendConfig();
            backendConfig.setName(CLIENT_ID);
            // set to false, default is true
            backendConfig.setBatchDecoder(false);

            ServiceConfig serviceConfig = new ServiceConfig();
            serviceConfig.setIp("127.0.0.1");
            serviceConfig.setPort(12344);
            serviceConfig.setName(SERVICE_ID);

            ConfigManager.getInstance().getClientConfig().getBackendConfigMap().putIfAbsent(CLIENT_ID, backendConfig);
            ConfigManager.getInstance().getServerConfig().getServiceMap().putIfAbsent(SERVICE_ID, serviceConfig);
        }
    }

    @TestConfiguration
    public static class BeanConfiguration {

        @Bean
        public TRpcConfigManagerCustomizer customizeAtLastCustomizer() {
            return configManager -> configManager.getClientConfig()
                    .getBackendConfigMap()
                    .get(CLIENT_ID)
                    .setNamingUrl(CUSTOMIZED_NAMING_URL);
        }

        @Bean
        public AddFilterTRpcConfigManagerCustomizer addFilterTRpcConfigManager() {
            return new AddFilterTRpcConfigManagerCustomizer()
                    .addClientFilters(ADDITIONAL_CLIENT_FILTER)
                    .addServerFilters(ADDITIONAL_SERVER_FILTER);
        }

        @Bean
        public ServiceHolder serviceHolder() {
            return new ServiceHolder();
        }
    }

    public static class ServiceHolder {

        @TRpcClient(id = CLIENT_ID)
        private TestService compatibleTestService;
    }
}
