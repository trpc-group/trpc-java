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

package com.tencent.trpc.core.common;

import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ClientConfig;
import com.tencent.trpc.core.common.config.GlobalConfig;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.rpc.AppInitializer;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import com.tencent.trpc.core.worker.support.thread.ThreadPoolConfig;
import com.tencent.trpc.core.worker.support.thread.ThreadWorkerPool;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfigManagerTest {

    private static final int TCP_PORT = 8090;
    private static final long WAIT_TIME = 2000;

    /**
     * ConfigManager start
     */
    @Before
    public void setUp() {
        ConfigManager.stopTest();
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setContainerName("cn");
        globalConfig.setEnableSet(true);
        globalConfig.setEnvName("en");
        globalConfig.setNamespace("prod");
        globalConfig.setFullSetName("fsn");
        ConfigManager.getInstance().addTRPCRunListener(new TRPCRunListener() {
            @Override
            public void starting() {
                TRPCRunListener.super.starting();
            }
        });
        ConfigManager.getInstance().setGlobalConfig(globalConfig);
        ConfigManager.getInstance().getServerConfig().setRunListeners(Lists.newArrayList());
        ConfigManager.getInstance().getServerConfig()
                .getRunListeners().add("listener1");
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:" + TCP_PORT);
        BackendConfig backendConfig2 = new BackendConfig();
        backendConfig2.setNamingUrl("ip://127.0.0.1:" + TCP_PORT + 1);
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.addBackendConfig(backendConfig);
        clientConfig.addBackendConfig(backendConfig2);
        ConfigManager.getInstance().setClientConfig(clientConfig);

        ConfigManager.startTest();
    }

    @After
    public void teardown() {
        ConfigManager.stopTest();
    }

@Test
    public void testStart() {
        // Reset ConfigManager state to avoid influence from listener1 configuration in setUp()
        ConfigManager.stopTest();
        ConfigManager.startTest();
        
        ConfigManager configManager = ConfigManager.getInstance();
        
        // Set minimal configuration, do not use listener1
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setRunListeners(Lists.newArrayList()); // Empty runListeners list
        configManager.setServerConfig(serverConfig);
        
        configManager.start();
        configManager.stop();
    }

    @Test
    public void testGetInstance() {
        ConfigManager instance = ConfigManager.getInstance();
        Assert.assertNotNull(instance);
    }

    @Test
    public void testSetDefault() {
        ConfigManager.getInstance().setDefault();
    }

    @Test
    public void testRegisterPlugin() {
        ThreadPoolConfig poolConfig = new ThreadPoolConfig();
        poolConfig.setCorePoolSize(1);
        PluginConfig pluginConfig = new PluginConfig("workerPool", WorkerPool.class,
                ThreadWorkerPool.class, poolConfig.toMap());
        ConfigManager.getInstance().registerPlugin(pluginConfig);
    }

    @Test
    public void testGetGlobalConfig() {
        GlobalConfig globalConfig = ConfigManager.getInstance().getGlobalConfig();
        Assert.assertNotNull(globalConfig);
        Assert.assertEquals("cn", globalConfig.getContainerName());
        Assert.assertEquals("en", globalConfig.getEnvName());
        Assert.assertEquals("prod", globalConfig.getNamespace());
        Assert.assertEquals("fsn", globalConfig.getFullSetName());
        Assert.assertTrue(globalConfig.isEnableSet());
    }

    @Test
    public void testGetServerConfig() {
        Assert.assertNotNull(ConfigManager.getInstance().getServerConfig());
    }

    @Test
    public void testSetServerConfig() {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setServer("aaa");
        ConfigManager.getInstance().setServerConfig(serverConfig);
        Assert.assertEquals("aaa", ConfigManager.getInstance().getServerConfig().getServer());
    }

    @Test
    public void testGetClientConfig() {
        Assert.assertNotNull(ConfigManager.getInstance().getClientConfig());
    }

    @Test
    public void testSetClientConfig() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setCharset("aaa");
        ConfigManager.getInstance().setClientConfig(clientConfig);
        Assert.assertEquals("aaa", ConfigManager.getInstance().getClientConfig().getCharset());
    }

    @Test
    public void testGetPluginConfigMap() {
        Assert.assertNotNull(ConfigManager.getInstance().getPluginConfigMap());
    }

    @Test
    public void testSetPluginConfigMap() {
        ConfigManager.getInstance().setPluginConfigMap(new HashMap<>());
        Assert.assertNotNull(ConfigManager.getInstance().getPluginConfigMap());
    }

    @Test
    public void testGetAppInitializer() {
        Assert.assertNull(ConfigManager.getInstance().getAppInitializer());
    }

    @Test
    public void testSetAppInitializer() {
        ConfigManager.getInstance().setAppInitializer(new AppInitializer() {
            @Override
            public void init() {

            }

            @Override
            public void stop() {

            }
        });
    }

@Test
    public void testGracefulRestart() throws InterruptedException {
        // Reset ConfigManager state to avoid influence from listener1 configuration in setUp()
        ConfigManager.stopTest();
        ConfigManager.startTest();
        

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setWaitTimeout(WAIT_TIME);
        serverConfig.setCloseTimeout(WAIT_TIME);
        serverConfig.setRunListeners(Lists.newArrayList()); // Empty runListeners list
        serverConfig.setDefault();

        final ConfigManager configManager = ConfigManager.getInstance();
        configManager.setServerConfig(serverConfig);
        
        configManager.start();
        Thread.sleep(WAIT_TIME);
        configManager.stop();
    }

    @Test
    public void testRegisterShutdownListener() {
        // Reset ConfigManager state to avoid influence from listener1 configuration in setUp()
        ConfigManager.stopTest();
        ConfigManager.startTest();
        
        ConfigManager configManager = ConfigManager.getInstance();
        
        // Set minimal configuration, do not use listener1
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setRunListeners(Lists.newArrayList()); // Empty runListeners list
        configManager.setServerConfig(serverConfig);
        
        TestShutdownListener listener = new TestShutdownListener();
        
        configManager.registerShutdownListener(listener);
        
        // Verify listener is registered by starting and stopping the container
        configManager.start(false); // Do not register shutdown hook
        configManager.stop();
        
        assertTrue("Shutdown listener should be called", listener.isShutdownCalled());
    }

    @Test
    public void testUnregisterShutdownListener() {
        // Reset ConfigManager state to avoid influence from listener1 configuration in setUp()
        ConfigManager.stopTest();
        ConfigManager.startTest();
        
        ConfigManager configManager = ConfigManager.getInstance();
        
        // Set minimal configuration, do not use listener1
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setRunListeners(Lists.newArrayList()); // Empty runListeners list
        configManager.setServerConfig(serverConfig);
        
        TestShutdownListener listener = new TestShutdownListener();
        
        configManager.registerShutdownListener(listener);
        configManager.unregisterShutdownListener(listener);
        
        // Verify listener is not called after being unregistered
        configManager.start(false);
        configManager.stop();
        
        assertFalse("Shutdown listener should not be called after unregister", listener.isShutdownCalled());
    }

    @Test
    public void testMultipleShutdownListeners() {
        // Reset ConfigManager state to avoid influence from listener1 configuration in setUp()
        ConfigManager.stopTest();
        ConfigManager.startTest();
        
        ConfigManager configManager = ConfigManager.getInstance();
        
        // Set minimal configuration, do not use listener1
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setRunListeners(Lists.newArrayList()); // Empty runListeners list
        configManager.setServerConfig(serverConfig);
        
        TestShutdownListener listener1 = new TestShutdownListener("listener1");
        TestShutdownListener listener2 = new TestShutdownListener("listener2");
        TestShutdownListener listener3 = new TestShutdownListener("listener3");
        
        configManager.registerShutdownListener(listener1);
        configManager.registerShutdownListener(listener2);
        configManager.registerShutdownListener(listener3);
        
        configManager.start(false);
        configManager.stop();
        
        assertTrue("Listener1 should be called", listener1.isShutdownCalled());
        assertTrue("Listener2 should be called", listener2.isShutdownCalled());
        assertTrue("Listener3 should be called", listener3.isShutdownCalled());
    }

    @Test
    public void testNullShutdownListenerHandling() {
        // Reset ConfigManager state to avoid influence from listener1 configuration in setUp()
        ConfigManager.stopTest();
        ConfigManager.startTest();
        
        ConfigManager configManager = ConfigManager.getInstance();
        
        // Set minimal configuration, do not use listener1
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setRunListeners(Lists.newArrayList()); // Empty runListeners list
        configManager.setServerConfig(serverConfig);
        
        // Verify null listener does not cause exceptions
        configManager.registerShutdownListener(null);
        configManager.unregisterShutdownListener(null);
        
        configManager.start(false);
        configManager.stop();
    }

    @Test
    public void testShutdownListenerExceptionHandling() {
        // Reset ConfigManager state to avoid influence from listener1 configuration in setUp()
        ConfigManager.stopTest();
        ConfigManager.startTest();
        
        ConfigManager configManager = ConfigManager.getInstance();
        
        // Set minimal configuration, do not use listener1
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setRunListeners(Lists.newArrayList()); // Empty runListeners list
        configManager.setServerConfig(serverConfig);
        
        TestShutdownListener goodListener = new TestShutdownListener("good");
        TestShutdownListener badListener = new TestShutdownListener("bad", true);
        
        configManager.registerShutdownListener(goodListener);
        configManager.registerShutdownListener(badListener);
        
        configManager.start(false);
        configManager.stop();
        
        // Verify other listeners are called even if one listener throws an exception
        assertTrue("Good listener should be called despite bad listener exception", goodListener.isShutdownCalled());
        assertTrue("Bad listener should be called even with exception", badListener.isShutdownCalled());
    }

    @Test
    public void testConfigManagerShutdownListenerOnly() {
        // Create an isolated test specifically for ShutdownListener functionality, not dependent on listener1
        ConfigManager.stopTest();
        
        // Reinitialize to ensure extensions are properly loaded
        ConfigManager.startTest();
        ConfigManager configManager = ConfigManager.getInstance();
        
        // Set minimal configuration, do not use listener1
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setRunListeners(Lists.newArrayList()); // Empty runListeners list
        configManager.setServerConfig(serverConfig);
        
        TestShutdownListener testListener = new TestShutdownListener("isolated-test");
        configManager.registerShutdownListener(testListener);
        
        try {
            configManager.start(false);
            configManager.stop();
            
            assertTrue("Shutdown listener should be called", testListener.isShutdownCalled());
        } catch (Exception e) {
            // Shutdown listener should be called even if startup fails
            assertTrue("Shutdown listener should be called even on startup failure", 
                      testListener.isShutdownCalled());
        }
    }

    @Test
    public void testShutdownListenerWithStartupFailure() {
        // Specifically test if ShutdownListener is still called when startup fails
        ConfigManager.stopTest();
        ConfigManager.startTest();
        ConfigManager configManager = ConfigManager.getInstance();
        
        TestShutdownListener testListener = new TestShutdownListener("startup-failure-test");
        configManager.registerShutdownListener(testListener);
        
        // Deliberately set a nonexistent listener to trigger startup failure
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setRunListeners(Lists.newArrayList("nonexistent-listener"));
        configManager.setServerConfig(serverConfig);
        
        try {
            configManager.start(false);
            // If no exception, stop normally
            configManager.stop();
        } catch (Exception e) {
            // Exception is expected, but ShutdownListener should be called during exception handling
        }
        
        assertTrue("Shutdown listener should be called even when startup fails",
                  testListener.isShutdownCalled());
    }

    /**
     * Test ShutdownListener implementation
     */
    private static class TestShutdownListener implements ShutdownListener {
        private final String name;
        private final boolean throwException;
        private final AtomicBoolean shutdownCalled = new AtomicBoolean(false);
        
        public TestShutdownListener() {
            this("test-listener", false);
        }
        
        public TestShutdownListener(String name) {
            this(name, false);
        }
        
        public TestShutdownListener(String name, boolean throwException) {
            this.name = name;
            this.throwException = throwException;
        }
        
        @Override
        public void onShutdown() {
            shutdownCalled.set(true);
            if (throwException) {
                throw new RuntimeException("Simulated exception in " + name);
            }
        }
        
        public boolean isShutdownCalled() {
            return shutdownCalled.get();
        }
    }

@Test
    public void testShutdownHook() {
        // Reset ConfigManager state to avoid influence from listener1 configuration in setUp()
        ConfigManager.stopTest();
        ConfigManager.startTest();

        ConfigManager configManager = ConfigManager.getInstance();
        configManager.start();
    }
}