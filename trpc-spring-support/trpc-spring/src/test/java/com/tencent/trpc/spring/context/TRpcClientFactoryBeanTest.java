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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import com.tencent.trpc.core.rpc.TRpcProxy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TRpcProxy.class})
public class TRpcClientFactoryBeanTest {

    private TRpcClientFactoryBean<MyTrpcService> factoryBean;

    @Before
    public void setUp() {
        String name = "trpcService";
        MockitoAnnotations.initMocks(this);
        factoryBean = new TRpcClientFactoryBean<>(name, MyTrpcService.class);
        PowerMockito.mockStatic(TRpcProxy.class);
        when(TRpcProxy.getProxy(name, MyTrpcService.class)).thenReturn(() -> 1024);
    }

    @Test
    public void testGetObject() {
        assertNotNull(factoryBean);
        MyTrpcService object = factoryBean.getObject();
        assertNotNull(object);
        int result = object.doMethod();
        Assert.assertEquals(1024, result);
    }

    @Test
    public void testGetGetObjectType() {
        Class<?> objectType = factoryBean.getObjectType();
        Assert.assertEquals(objectType, MyTrpcService.class);
    }

    public interface MyTrpcService {

        int doMethod();
    }
}
