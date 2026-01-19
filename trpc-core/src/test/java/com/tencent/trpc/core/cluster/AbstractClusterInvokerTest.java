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

package com.tencent.trpc.core.cluster;

import com.tencent.trpc.core.cluster.def.DefClusterInvoker;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.NamingOptions;
import com.tencent.trpc.core.extension.ExtensionManager;
import com.tencent.trpc.core.rpc.GenericClient;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.selector.SelectorManager;
import com.tencent.trpc.core.selector.spi.Selector;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AbstractClusterInvokerTest {

    private static final String INVALID_SELECTOR = "invalid";

    private DefClusterInvoker<GenericClient> defClusterInvoker;

    @Mock
    private ExtensionManager<Selector> mockManager;

    @BeforeEach
    public void setUp() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setServiceInterface(GenericClient.class);
        backendConfig.setProxyType("bytebuddy");
        backendConfig.setNamingUrl("ip://127.0.0.1:12345");
        backendConfig.setName("GenericClient");
        backendConfig.setNamespace("development");
        backendConfig.setWorkerPool(WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_NAME);
        backendConfig.setDefault();
        NamingOptions options = new NamingOptions();
        options.setSelectorId(INVALID_SELECTOR);
        backendConfig.setNamingOptions(options);
        ConsumerConfig<GenericClient> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setBackendConfig(backendConfig);
        consumerConfig.setServiceInterface(GenericClient.class);
        
        try (MockedStatic<SelectorManager> mockedStatic = Mockito.mockStatic(SelectorManager.class)) {
            mockedStatic.when(SelectorManager::getManager).thenReturn(mockManager);
            Mockito.when(mockManager.get(INVALID_SELECTOR)).thenReturn(null);
            this.defClusterInvoker = new DefClusterInvoker<>(consumerConfig);
        }
    }

    @Test
    public void testInvoke() {
        try (MockedStatic<SelectorManager> mockedStatic = Mockito.mockStatic(SelectorManager.class)) {
            mockedStatic.when(SelectorManager::getManager).thenReturn(mockManager);
            Mockito.when(mockManager.get(INVALID_SELECTOR)).thenReturn(null);
            
            try {
                defClusterInvoker.invoke(new DefRequest()).toCompletableFuture().join();
            } catch (CompletionException exception) {
                NamingOptions namingOptions = defClusterInvoker.getConfig().getBackendConfig().getNamingOptions();
                String expect = "com.tencent.trpc.core.exception.TRpcException: "
                        + "the selector name:" + namingOptions.getSelectorId() + " not found selector";
                Assertions.assertEquals(expect, exception.getMessage());
            }
        }
    }

}
