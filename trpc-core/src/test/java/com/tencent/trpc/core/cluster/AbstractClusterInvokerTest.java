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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SelectorManager.class)
@PowerMockIgnore({"javax.management.*", "javax.security.*", "javax.ws.*"})
public class AbstractClusterInvokerTest {

    private static final String INVALID_SELECTOR = "invalid";

    private DefClusterInvoker<GenericClient> defClusterInvoker;

    @Mock
    private ExtensionManager<Selector> mockManager;

    /**
     * Init defClusterInvoker & mockManager
     */
    @Before
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
        // mock SelectorManager and ExtensionManager<Selector>
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(SelectorManager.class);
        PowerMockito.when(SelectorManager.getManager()).thenReturn(mockManager);
        PowerMockito.when(mockManager.get(INVALID_SELECTOR)).thenReturn(null);
        this.defClusterInvoker = new DefClusterInvoker<>(consumerConfig);
    }

    @Test
    public void testInvoke() {
        try {
            defClusterInvoker.invoke(new DefRequest()).toCompletableFuture().join();
        } catch (CompletionException exception) {
            NamingOptions namingOptions = defClusterInvoker.getConfig().getBackendConfig().getNamingOptions();
            String expect = "com.tencent.trpc.core.exception.TRpcException: "
                    + "the selector name:" + namingOptions.getSelectorId() + " not found selector";
            Assert.assertEquals(expect, exception.getMessage());
        }
    }

}
