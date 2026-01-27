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

package com.tencent.trpc.support;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.factory.api.APIFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.registry.polaris.PolarisRegistry;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HeatBeatManagerTest {
    private MockedStatic<APIFactory> mockedAPIFactory;

    @BeforeEach
    public void setUp() {
        try {
        mockedAPIFactory = Mockito.mockStatic(APIFactory.class);
            HeartBeatManager.init(200);
        } catch (Throwable e) {
            //To prevent exceptions from occurring when tasks are submitted after the UT is completed
            // and the shutdown method is called for online invocation.
            e.printStackTrace();
        }
    }

    @Test
    public void startHeartBeatTest() throws PolarisException {
        PolarisRegistry registry = Mockito.mock(PolarisRegistry.class);
        Map<String, Object> params = new HashMap<>();
        RegisterInfo registerInfo =
                new RegisterInfo("http", "127.0.0.1", 8080, "test", "normal", "v1.0.0", params);
        HeartBeatManager.startHeartBeat(registerInfo, registry);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Mockito.verify(registry, Mockito.atLeast(1)).heartbeat(registerInfo);
    }

    @AfterEach
    public void destroy() throws PolarisException {
        if (mockedAPIFactory != null) {
            mockedAPIFactory.close();
        }

        HeartBeatManager.destroy();
    }
}
