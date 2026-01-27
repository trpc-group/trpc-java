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

package com.tencent.trpc.registry.scheduler;

import com.ecwid.consul.v1.ConsulClient;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.support.ConsulInstanceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.HashMap;
import java.util.Map;

public class TtlSchedulerTest {


    private TtlScheduler ttlScheduler;
    private String serviceId = "trpc.test.testName";

    @BeforeEach
    public void setUp() {

        Map<String, Object> extMap = new HashMap<>();
        extMap.put("addresses", "127.0.0.1:8088");
        extMap.put("ttl_enabled", false);
        extMap.put("tag", "dev");

        ProtocolConfig protocolConfig = new ProtocolConfig();

        protocolConfig.setExtMap(extMap);

        ConsulInstanceManager consulInstanceManager = new ConsulInstanceManager(protocolConfig);
        ConsulClient consulClient = Mockito.mock(ConsulClient.class);
        consulInstanceManager.resetClient(consulClient);
        this.ttlScheduler = new TtlScheduler(consulInstanceManager);
    }

    @Test
    public void add() {
        ttlScheduler.add(serviceId);
        ttlScheduler.add(serviceId);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ttlScheduler.remove(serviceId);
        ttlScheduler.stop();
    }

}
