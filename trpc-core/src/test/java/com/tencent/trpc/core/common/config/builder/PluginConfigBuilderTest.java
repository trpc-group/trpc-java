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

package com.tencent.trpc.core.common.config.builder;

import static org.junit.Assert.assertEquals;

import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.rpc.GenericClient;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import com.tencent.trpc.core.worker.support.thread.ForkJoinWorkerPool;
import com.tencent.trpc.core.worker.support.thread.ThreadWorkerPool;
import java.util.HashMap;
import org.junit.Assert;
import org.junit.Test;

public class PluginConfigBuilderTest {

    @Test
    public void test() {
        PluginConfigBuilder builder =
                PluginConfigBuilder.newBuilder().setName("id")
                        .setPluginClass(ThreadWorkerPool.class);
        assertEquals("id", builder.getName());
        assertEquals(ThreadWorkerPool.class, builder.getPluginClass());
        assertEquals(null, builder.getPluginInterface());
        PluginConfig config = builder.build();
        assertEquals("id", config.getName());
        assertEquals(ThreadWorkerPool.class, config.getPluginClass());
        assertEquals(WorkerPool.class, config.getPluginInterface());
        builder.setPluginInterface(GenericClient.class);
        assertEquals(GenericClient.class, builder.getPluginInterface());
        builder.setPluginClass(ForkJoinWorkerPool.class);
        assertEquals(ForkJoinWorkerPool.class, builder.getPluginClass());
        builder.addPropertie("a", "a");
        Assert.assertEquals("a", builder.getProperties().get("a"));
        builder.setProperties(new HashMap<>());
        Assert.assertEquals(0, builder.getProperties().size());
    }
}
