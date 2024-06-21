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

package com.tencent.trpc.registry.polaris;

import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.PRIORITY_PARAM_KEY;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.WEIGHT_PARAM_KEY;
import static org.mockito.Matchers.anyObject;

import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.server.ReportServiceContractRequest;
import com.tencent.polaris.api.plugin.server.ReportServiceContractResponse;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceHeartbeatRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.factory.api.APIFactory;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.core.registry.spi.Registry;
import com.tencent.trpc.polaris.common.PolarisRegistryConstant;
import com.tencent.trpc.support.HeartBeatManager;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({APIFactory.class, HeartBeatManager.class})
@PowerMockIgnore({"javax.management.*"})
public class PolarisRegistryTest extends TestCase {

    @Captor
    private ArgumentCaptor<Integer> intervalCaptor;

    protected void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(APIFactory.class);
        PowerMockito.mockStatic(HeartBeatManager.class);
    }

    @Test
    public void testRegistry() throws PolarisException {
        PowerMockito.when(APIFactory.createProviderAPIByConfig(anyObject()))
                .thenReturn(new ProviderAPI() {
                    @Override
                    public InstanceRegisterResponse registerInstance(InstanceRegisterRequest instanceRegisterRequest)
                            throws PolarisException {
                        return null;
                    }

                    @Override
                    public InstanceRegisterResponse register(
                            InstanceRegisterRequest instanceRegisterRequest) {
                        Assert.assertEquals(2000, instanceRegisterRequest.getTtl().intValue());
                        return new InstanceRegisterResponse("101", true);
                    }

                    @Override
                    public void deRegister(InstanceDeregisterRequest instanceDeRegisterRequest)
                            throws PolarisException {
                        Assert.assertEquals("101", instanceDeRegisterRequest.getInstanceID());
                    }

                    @Override
                    public void heartbeat(InstanceHeartbeatRequest instanceHeartbeatRequest)
                            throws PolarisException {
                        Assert.assertEquals("101", instanceHeartbeatRequest.getInstanceID());
                    }

                    @Override
                    public ReportServiceContractResponse reportServiceContract(
                            ReportServiceContractRequest reportServiceContractRequest) throws PolarisException {
                        return null;
                    }

                    @Override
                    public void destroy() {

                    }

                    @Override
                    public void close() {
                        ProviderAPI.super.close();
                    }
                });
        Map<String, Object> extMap = new HashMap<>();
        extMap.put(PolarisRegistryConstant.POLARIS_ADDRESSES_KEY, "10.0.0.1");
        extMap.put(PolarisRegistryConstant.TOKEN_PARAM_KEY, "test");
        extMap.put(PolarisRegistryConstant.HEARTBEAT_THREAD_CORE_SIZE, 10);
        extMap.put(PolarisRegistryConstant.HEARTBEAT_THREAD_MAXSIZE, 20);
        extMap.put(PolarisRegistryConstant.HEARTBEAT_THREAD_QUEUE_SIZE, 1000);
        extMap.put(PolarisRegistryConstant.HEARTBEAT_THREAD_KEEP_ALIVE_SECONDS, 120L);
        extMap.put(PolarisRegistryConstant.TTL_KEY, 2000);
        extMap.put(PRIORITY_PARAM_KEY, 1);
        extMap.put(WEIGHT_PARAM_KEY, 100);
        extMap.put(PolarisRegistryConstant.REGISTER_SELF, true);
        ConfigManager.getInstance().getGlobalConfig().setEnableSet(true);
        ConfigManager.getInstance().getGlobalConfig().setFullSetName("test.sz.1");
        ConfigManager.getInstance().registerPlugin(new PluginConfig("polaris", PolarisRegistry.class, extMap));

        Map<String, Object> params = new HashMap<>();
        params.put(PolarisRegistryConstant.TOKEN_PARAM_KEY, "test");
        params.put(PolarisRegistryConstant.NAMESPACE_KEY, "test");
        params.put(PolarisRegistryConstant.TIMEOUT_PARAM_KEY, 60000);
        params.put(PRIORITY_PARAM_KEY, 1);
        params.put(WEIGHT_PARAM_KEY, 100);

        RegisterInfo registerInfo =
                new RegisterInfo("http", "127.0.0.1", 8080, "test", "normal", "v1.0.0", params);
        PolarisRegistry registry = (PolarisRegistry) ExtensionLoader
                .getExtensionLoader(Registry.class)
                .getExtension("polaris");
        registry.register(registerInfo);
        PowerMockito.verifyStatic();
        HeartBeatManager.init(intervalCaptor.capture());

        Assert.assertEquals(3000, intervalCaptor.getValue().longValue());

        registry.heartbeat(registerInfo);
        registry.heartbeat(registerInfo);
        registry.heartbeat(registerInfo);
        registry.heartbeat(registerInfo);
        registry.unregister(registerInfo);
        registry.destroy();
    }

}
