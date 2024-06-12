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

import static org.powermock.api.mockito.PowerMockito.when;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ClientConfig;
import com.tencent.trpc.core.utils.StringUtils;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.annotation.AnnotationAttributes;

@PowerMockIgnore({"javax.management.*"})
@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigManager.class})
public class TRpcClientAnnotationBeanPostProcessorTest {

    private TRpcClientAnnotationBeanPostProcessor postProcessor;

    @Mock
    private ConfigManager configManager;

    @Mock
    private ClientConfig clientConfig;

    @Mock
    private Map<String, BackendConfig> backendConfigMap;

    @Mock
    private BackendConfig backendConfig;

    @Before
    public void setUp() {
        postProcessor = new TRpcClientAnnotationBeanPostProcessor();
        PowerMockito.mockStatic(ConfigManager.class);
        backendConfig.setName("myName");
    }


    @Test
    public void testDoGetInjectedBean() {
        when(ConfigManager.getInstance()).thenReturn(configManager);
        when(configManager.getClientConfig()).thenReturn(clientConfig);
        when(clientConfig.getBackendConfigMap()).thenReturn(backendConfigMap);
        when(backendConfigMap.get(Mockito.anyString())).thenReturn(backendConfig);
        AnnotationAttributes attributes = new AnnotationAttributes();
        attributes.put("id", "MmyAnnotationService");
        Object o = postProcessor.doGetInjectedBean(attributes, null, null, MyAnnotationService.class, null);
        Assert.assertNull(o);
        when(backendConfig.getDefaultProxy()).thenReturn(new Object());
        o = postProcessor.doGetInjectedBean(attributes, null, null, MyAnnotationService.class, null);
        Assert.assertNotNull(o);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDoGetInjectedBeanWithException() {
        when(ConfigManager.getInstance()).thenReturn(configManager);
        when(configManager.getClientConfig()).thenReturn(clientConfig);
        when(clientConfig.getBackendConfigMap()).thenReturn(backendConfigMap);
        when(backendConfigMap.get(Mockito.anyString())).thenReturn(backendConfig);
        AnnotationAttributes attributes = new AnnotationAttributes();
        attributes.put("id", StringUtils.EMPTY);
        postProcessor.doGetInjectedBean(attributes, null, null, MyAnnotationService.class, null);
    }

    static class MyAnnotationService {

    }
}
