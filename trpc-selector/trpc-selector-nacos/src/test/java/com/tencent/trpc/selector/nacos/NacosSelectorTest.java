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

package com.tencent.trpc.selector.nacos;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.core.rpc.AbstractRequest;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.loadbalance.support.RandomLoadBalance;
import com.tencent.trpc.registry.center.NotifyListener;
import com.tencent.trpc.registry.center.RegistryCenter;
import org.junit.jupiter.api.Test;

/**
 * Nacos selector test class
 */
public class NacosSelectorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(NacosSelectorTest.class);

    private final NacosSelector selector = new NacosSelector();

    @Test
    public void init() {
        try {
            selector.init();
        } catch (Exception e) {
            LOGGER.warn("selector init {}", e);
        }
    }

    @Test
    public void asyncSelectOne() {
        ServiceId serviceId = new ServiceId();
        serviceId.setServiceName("test");
        selector.setRegistryCenter(registryCenter);
        selector.setLoadBalance(new RandomLoadBalance());
        selector.asyncSelectOne(serviceId, new MockRequest());
    }

    @Test
    public void asyncSelectOne01() {
        ServiceId serviceId = new ServiceId();
        serviceId.setServiceName("test");
        selector.setRegistryCenter(registryCenter);
        try {
            selector.asyncSelectOne(serviceId, new MockRequest());
        } catch (Exception e) {
            LOGGER.warn("asyncSelectOne {}", e);
        }
    }

    @Test
    public void asyncSelectOne02() {
        try {
            selector.asyncSelectOne(null, null);
        } catch (Exception e) {
            LOGGER.warn("asyncSelectOne {}", e);
        }
    }

    @Test
    public void asyncSelectAll() {
        ServiceId serviceId = new ServiceId();
        serviceId.setServiceName("test");
        selector.setRegistryCenter(registryCenter);
        selector.asyncSelectAll(serviceId, new MockRequest());
    }

    @Test
    public void report() {
        selector.report(null, 0, 0);
    }

    @Test
    public void destroy() {
        selector.destroy();
    }

    private static class MockRequest extends AbstractRequest {

    }

    private static RegistryCenter registryCenter = new RegistryCenter() {
        @Override
        public void subscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {

        }

        @Override
        public void unsubscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {

        }

        @Override
        public void register(RegisterInfo registerInfo) {

        }

        @Override
        public void unregister(RegisterInfo registerInfo) {

        }

        @Override
        public boolean isAvailable() {
            return false;
        }
    };
}
