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

package com.tencent.trpc.registry.factory;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.registry.center.AbstractRegistryCenter;
import com.tencent.trpc.registry.center.NotifyListener;
import com.tencent.trpc.registry.center.RegistryCenter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Test class for registry.
 */
public class AbstractRegistryFactoryTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRegistryFactoryTest.class);

    private final RegistryFactory registryFactory = new AbstractRegistryFactory() {
        @Override
        protected AbstractRegistryCenter createRegistry(ProtocolConfig protocolConfig) {
            return new AbstractRegistryCenter() {

                @Override
                public void init() throws TRpcExtensionException {

                }

                @Override
                public void subscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {

                }

                @Override
                public void unsubscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {

                }

                @Override
                public void destroy() throws TRpcExtensionException {

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
    };

    private final RegistryFactory registryFactory01 = new AbstractRegistryFactory() {
        @Override
        protected AbstractRegistryCenter createRegistry(ProtocolConfig protocolConfig) {
            return null;
        }
    };

    private final RegistryFactory registryFactory02 = new AbstractRegistryFactory() {
        @Override
        protected AbstractRegistryCenter createRegistry(ProtocolConfig protocolConfig) {
            return new AbstractRegistryCenter() {
                @Override
                public void init() throws TRpcExtensionException {

                }

                @Override
                public void subscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {

                }

                @Override
                public void unsubscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {

                }

                @Override
                public void destroy() throws TRpcExtensionException {

                }

                @Override
                public void register(RegisterInfo registerInfo) {

                }

                @Override
                public void unregister(RegisterInfo registerInfo) {

                }

                @Override
                public boolean isAvailable() {
                    return true;
                }
            };
        }
    };

    AbstractRegistryCenter registryCenter = new AbstractRegistryCenter() {
        @Override
        public void init() throws TRpcExtensionException {

        }

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
            return true;
        }
    };


    AbstractRegistryCenter registryCenter01 = new AbstractRegistryCenter() {
        @Override
        public void init() throws TRpcExtensionException {

        }

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

    @Test
    public void getRegistries() {
        Map<String, AbstractRegistryCenter> registriesMap = AbstractRegistryFactory.REGISTRIES;
        registriesMap.clear();
        Collection<AbstractRegistryCenter> registries = AbstractRegistryFactory.getRegistries();
        Assertions.assertEquals(0, registries.size());
    }

    @Test
    public void getRegistry() {
        Map<String, AbstractRegistryCenter> registries = AbstractRegistryFactory.REGISTRIES;
        registries.put("test", null);
        AbstractRegistryCenter registryCenter = AbstractRegistryFactory.getRegistry("test");
        Assertions.assertNull(registryCenter);
    }

    @Test
    public void connect04() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("addresses", "127.0.0.1:8500");
        protocolConfig.setExtMap(extMap);
        RegistryCenter registryCenter = registryFactory.connect(protocolConfig);
        Assertions.assertNotNull(registryCenter);
        AbstractRegistryFactory.clearRegistryNotDestroy();
    }

    @Test
    public void connect() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("addresses", "127.0.0.1:8500");
        protocolConfig.setExtMap(extMap);
        RegistryCenter registryCenter = registryFactory.connect(protocolConfig);
        Assertions.assertNotNull(registryCenter);

        AbstractRegistryFactory.destroyAll();
        AbstractRegistryCenter registryCenterT = registryFactory.connect(protocolConfig);
        Assertions.assertNotNull(registryCenterT);
        registryCenterT.unsubscribe(null, null);
        registryCenterT.subscribe(null, null);
        registryCenterT.register(null);
        registryCenterT.unregister(null);
        registryCenterT.destroy();
        boolean available = registryCenterT.isAvailable();
        Assertions.assertFalse(available);
    }

    @Test
    public void connect01() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("addresses", "127.0.0.1:8500");
        protocolConfig.setExtMap(extMap);
        try {
            registryFactory01.connect(protocolConfig);
        } catch (Exception e) {
            LOGGER.warn("connect warn{}", e);
        }
    }

    @Test
    public void connect02() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("addresses", "127.0.0.1:8500");
        protocolConfig.setExtMap(extMap);

        Map<String, AbstractRegistryCenter> registries = AbstractRegistryFactory.REGISTRIES;
        registries.put("127.0.0.1:8500", registryCenter);

        RegistryCenter registryCenter = registryFactory02.connect(protocolConfig);
        Assertions.assertNotNull(registryCenter);
    }


    @Test
    public void connect03() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("addresses", "127.0.0.1:8500");
        protocolConfig.setExtMap(extMap);

        try {
            registryFactory02.connect(protocolConfig);
        } catch (Exception e) {
            LOGGER.warn("connect warn{}", e);
        }
    }

    @Test
    public void destroyAll() {
        Map<String, AbstractRegistryCenter> registries = AbstractRegistryFactory.REGISTRIES;
        registries.put("test", registryCenter);
        AbstractRegistryFactory.clearRegistryNotDestroy();
    }

    @Test
    public void destroyAll01() {
        Map<String, AbstractRegistryCenter> registries = AbstractRegistryFactory.REGISTRIES;
        registries.put("test", registryCenter01);
        AbstractRegistryFactory.destroyAll();
        AbstractRegistryFactory.destroyAll();
        AbstractRegistryFactory.clearRegistryNotDestroy();
    }

    @Test
    public void createRegistry() {
    }

    @Test
    public void removeDestroyedRegistry() {
    }

}
