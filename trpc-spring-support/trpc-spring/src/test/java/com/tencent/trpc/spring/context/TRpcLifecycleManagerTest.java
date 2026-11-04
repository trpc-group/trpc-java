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

package com.tencent.trpc.spring.context;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.spring.test.TRpcConfigManagerTestUtils;
import java.lang.reflect.Field;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;

public class TRpcLifecycleManagerTest {

    private static final String AWARE_CONTEXT_ID = "application";

    private static final String SERVICE_NAME = "TRpcLifecycleManagerTest.service";

    private ConfigManager configManager;

    private ServerConfig serverConfig;

    private GenericApplicationContext context;

    private TRpcLifecycleManager manager;

    @Before
    public void setUp() {
        configManager = Mockito.mock(ConfigManager.class);
        serverConfig = new ServerConfig();
        Mockito.when(configManager.getServerConfig()).thenReturn(serverConfig);
        setConfigManager(configManager);

        context = new GenericApplicationContext();
        context.setId(AWARE_CONTEXT_ID);
        context.registerBean(TestServiceImpl.class);
        context.refresh();

        manager = new TRpcLifecycleManager();
        manager.setApplicationContext(context);
    }

    @After
    public void tearDown() {
        context.close();
        TRpcConfigManagerTestUtils.resetConfigManager();
    }

    @Test
    public void testGetOrder() {
        Assert.assertEquals(TRpcLifecycleManager.START_ORDER, manager.getOrder());
        Assert.assertEquals(Ordered.LOWEST_PRECEDENCE - 20, TRpcLifecycleManager.START_ORDER);
    }

    @Test
    public void testStartAndStop() {
        ProviderConfig<?> providerConfig = addProvider(TestServiceImpl.class.getName());

        manager.onApplicationEvent(new ContextRefreshedEvent(context));
        Assert.assertTrue(manager.isStarted());
        Assert.assertTrue(providerConfig.getRef() instanceof TestServiceImpl);
        Assert.assertEquals(TestService.class, providerConfig.getServiceInterface());
        Mockito.verify(configManager, Mockito.times(1)).start(false);

        // start only once
        manager.onApplicationEvent(new ContextRefreshedEvent(context));
        Mockito.verify(configManager, Mockito.times(1)).start(false);

        manager.stopIfStarted();
        Assert.assertFalse(manager.isStarted());
        Mockito.verify(configManager, Mockito.times(1)).stop();

        // stop only once
        manager.stopIfStarted();
        manager.destroy();
        Mockito.verify(configManager, Mockito.times(1)).stop();
    }

    @Test
    public void testStateIsPerInstance() {
        manager.onApplicationEvent(new ContextRefreshedEvent(context));
        Assert.assertTrue(manager.isStarted());

        TRpcLifecycleManager another = new TRpcLifecycleManager();
        Assert.assertFalse(another.isStarted());
        another.stopIfStarted();
        Mockito.verify(configManager, Mockito.never()).stop();
        Assert.assertTrue(manager.isStarted());
    }

    @Test
    public void testDestroyStopsStartedTRpc() {
        manager.onApplicationEvent(new ContextRefreshedEvent(context));
        Assert.assertTrue(manager.isStarted());

        manager.destroy();
        Assert.assertFalse(manager.isStarted());
        Mockito.verify(configManager, Mockito.times(1)).stop();
    }

    @Test
    public void testDestroyWithoutStart() {
        manager.destroy();
        Mockito.verify(configManager, Mockito.never()).stop();
    }

    @Test
    public void testIgnoreChildContextEvent() {
        GenericApplicationContext child = new GenericApplicationContext(context);
        child.setId(AWARE_CONTEXT_ID + ":management");

        Assert.assertTrue(manager.isManagedContext(context));
        Assert.assertFalse(manager.isManagedContext(child));
        Assert.assertFalse(manager.isManagedContext(null));

        manager.onApplicationEvent(new ContextRefreshedEvent(child));
        Assert.assertFalse(manager.isStarted());
        Mockito.verify(configManager, Mockito.never()).start(false);
    }

    @Test
    public void testIgnoreNotAwareContextEvent() {
        context.setId("not-aware");
        Assert.assertFalse(manager.isManagedContext(context));

        manager.onApplicationEvent(new ContextRefreshedEvent(context));
        Assert.assertFalse(manager.isStarted());
        Mockito.verify(configManager, Mockito.never()).start(false);
    }

    @Test
    public void testWithoutApplicationContextAware() {
        TRpcLifecycleManager notAwareManager = new TRpcLifecycleManager();
        Assert.assertTrue(notAwareManager.isManagedContext(context));

        context.setId("not-aware");
        Assert.assertFalse(notAwareManager.isManagedContext(context));
    }

    @Test
    public void testStartFailedWithRuntimeException() {
        IllegalStateException startException = new IllegalStateException("start failed");
        Mockito.doThrow(startException).when(configManager).start(false);
        try {
            manager.onApplicationEvent(new ContextRefreshedEvent(context));
            Assert.fail("expected exception");
        } catch (IllegalStateException e) {
            Assert.assertSame(startException, e);
        }
        Assert.assertFalse(manager.isStarted());

        // stop is not required since tRPC lifecycle has stopped itself on start failure
        manager.stopIfStarted();
        Mockito.verify(configManager, Mockito.never()).stop();

        // could start again
        Mockito.doNothing().when(configManager).start(false);
        manager.onApplicationEvent(new ContextRefreshedEvent(context));
        Assert.assertTrue(manager.isStarted());
    }

    @Test
    public void testStartFailedWithError() {
        NoClassDefFoundError startError = new NoClassDefFoundError("start failed");
        Mockito.doThrow(startError).when(configManager).start(false);
        try {
            manager.onApplicationEvent(new ContextRefreshedEvent(context));
            Assert.fail("expected error");
        } catch (NoClassDefFoundError e) {
            Assert.assertSame(startError, e);
        }
        Assert.assertFalse(manager.isStarted());

        manager.destroy();
        Mockito.verify(configManager, Mockito.never()).stop();
    }

    @Test
    public void testReplaceProviderRefFailed() {
        addProvider("com.tencent.trpc.spring.context.NotExistServiceImpl");
        try {
            manager.onApplicationEvent(new ContextRefreshedEvent(context));
            Assert.fail("expected exception");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getCause() instanceof ClassNotFoundException);
        }
        Assert.assertFalse(manager.isStarted());
        Mockito.verify(configManager, Mockito.never()).start(false);
    }

    @Test
    public void testStartAndStopTRpcContainer() {
        TRpcConfigManagerTestUtils.resetConfigManager();
        ConfigManager.getInstance().getServerConfig().setWaitTimeout(1);
        ConfigManager.getInstance().getServerConfig().setCloseTimeout(1);
        TRpcLifecycleManager realManager = new TRpcLifecycleManager();
        realManager.setApplicationContext(context);
        realManager.onApplicationEvent(new ContextRefreshedEvent(context));
        Assert.assertTrue(realManager.isStarted());

        realManager.destroy();
        Assert.assertFalse(realManager.isStarted());
    }

    private ProviderConfig<?> addProvider(String refClazz) {
        ProviderConfig<?> providerConfig = new ProviderConfig<>();
        providerConfig.setRefClazz(refClazz);
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.addProviderConfig(providerConfig);
        serverConfig.getServiceMap().put(SERVICE_NAME, serviceConfig);
        return providerConfig;
    }

    private static void setConfigManager(ConfigManager configManager) {
        try {
            Field instanceField = ConfigManager.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, configManager);
        } catch (Exception e) {
            throw new IllegalStateException("set ConfigManager instance error", e);
        }
    }

    @TRpcService(name = SERVICE_NAME)
    public interface TestService {

        @TRpcMethod(name = "call")
        Object call(RpcContext context, Object req);
    }

    public static class TestServiceImpl implements TestService {

        @Override
        public Object call(RpcContext context, Object req) {
            return null;
        }
    }
}
