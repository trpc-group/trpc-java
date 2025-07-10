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

package com.tencent.trpc.support.util;

import com.ecwid.consul.v1.health.model.HealthService;
import com.google.common.collect.Lists;
import com.tencent.trpc.core.registry.RegisterInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Consul build service test class
 */
public class ConsulNewServiceUtilsTest {

    @Test
    public void convert() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("addresses", "127.0.0.1:8088");

        RegisterInfo registerInfo = new RegisterInfo("tcp", "127.0.0.1", 8080, "test", extMap);
        HealthService healthService = new HealthService();
        HealthService.Service service = new HealthService.Service();
        Map<String, String> metaMap = new HashMap<>();
        String encode = RegisterInfo.encode(registerInfo);
        metaMap.put("url", encode);
        service.setMeta(metaMap);
        healthService.setService(service);

        List<RegisterInfo> convertList = ConsulServiceUtils.convert(Lists.newArrayList(healthService), registerInfo);
        Assert.assertEquals(1, convertList.size());
    }
}