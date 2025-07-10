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

package com.tencent.trpc.container.config.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.tencent.trpc.container.config.ApplicationConfigParser;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.extension.ExtensionLoader;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Environment test class
 */
public class EnvironmentTest {

    private Environment environment;
    private ConfigManager configManager;
    private ConfigManager otherConfigManager;

    @Before
    public void init() {
        System.setProperty("global.namespace", "${env_type_enhancer}");
        System.setProperty("server.app", "wechat");
        System.setProperty("server.local_ip", "0.0.0.0");
        System.setProperty("server.service[0].name", "trpc.TestApp.TestServer.Greeter2");
        System.setProperty("server.service[0].impls[0]", "com.tencent.trpc.container.demo.GreeterServiceImp2");
        System.setProperty("server.service[0].impls[1]", "com.tencent.trpc.container.demo.GreeterServiceImp3");
        System.setProperty("server.service[0].protocol", "fbp");
        System.setProperty("client.protocol", "fbp");
        System.setProperty("client.service[0].name", "trpc.TestApp.TestServer.Greeter3");
        System.setProperty("client.service[0].naming_url", "ip://127.0.0.1:77777");
        System.setProperty("worker.pool", "30");
        System.setProperty("enable.distribution.transaction", "true");
        System.setProperty("short.test", "1");
        System.setProperty("byte.test", "1");
        System.setProperty("float.test", "1");
        System.setProperty("double.test", "1");
        ApplicationConfigParser parser = ExtensionLoader.getExtensionLoader(ApplicationConfigParser.class)
                .getExtension("yaml");
        environment = new Environment(parser);
        configManager = environment.parseFromClassPath("trpc_java.yaml");
        otherConfigManager = environment.parse();
    }

    @After
    public void teardown() {
        System.clearProperty("global.namespace");
        System.clearProperty("server.app");
        System.clearProperty("server.local_ip");
        System.clearProperty("server.service[0].name");
        System.clearProperty("server.service[0].impls[0]");
        System.clearProperty("server.service[0].impls[1]");
        System.clearProperty("server.service[0].protocol");
        System.clearProperty("client.protocol");
        System.clearProperty("client.service[0].name");
        System.clearProperty("client.service[0].naming_url");
        System.clearProperty("worker.pool");
        System.clearProperty("enable.distribution.transaction");
        System.clearProperty("short.test");
        System.clearProperty("byte.test");
        System.clearProperty("float.test");
        System.clearProperty("double.test");
    }

    @Test
    public void testParse() {
        assertTrue(null != configManager);
        assertTrue(null != otherConfigManager);
        assertConfiguration();
    }

    @Test
    public void assertConfiguration() {
        assertEquals("${env_type_enhancer}", environment.getString("global.namespace"));
        assertEquals("OK", environment.getString("global.namespace2", "OK"));
        assertEquals(30, environment.getInt("worker.pool"));
        assertEquals(-2, environment.getInt("worker.pool2", -2));
        assertEquals(-2, environment.getInt("worker.pool2", -2));
        assertEquals(3, environment.getInteger("worker.pool3", 3).intValue());
        assertTrue(environment.getBoolean("enable.distribution.transaction"));
        assertTrue(environment.getBoolean("enable.distribution.transaction2", true));
        assertNull(environment.getString("not_exist"));
        assertEquals("${env_type_enhancer}", environment.getProperty("global.namespace"));
        assertEquals("BAD", environment.getProperty("global.namespace222", "BAD"));
        assertEquals("OK", environment.getString("global.namespace2", "OK"));

        assertTrue(1 == environment.getShort("short.test"));
        assertTrue(2 == environment.getShort("short.test2", (short) 2));

        assertTrue(1 == environment.getByte("byte.test"));
        assertTrue(2 == environment.getByte("byte.test2", (byte) 2));

        assertTrue(environment.getFloat("float.test") > 0);
        assertTrue(environment.getFloat("float.test", -1F) > 0);

        assertTrue(environment.getDouble("double.test") > 0);
        assertTrue(environment.getDouble("double.test", -1F) > 0);
    }

    @Test
    public void testOverrideConfig() {
        String namespace = environment.getString("global.namespace");
        assertEquals(namespace, configManager.getGlobalConfig().getNamespace());

        String protocol = environment.getString("server.service[0].protocol");
        assertEquals(protocol, configManager.getClientConfig().getProtocol());
    }

    @Test(expected = IllegalStateException.class)
    public void testInteger() {
        environment.getInteger("global.namespace", 2);
    }

    @Test
    public void testBoolean() {
        environment.getBoolean("global.namespace", true);
    }

    @Test(expected = IllegalStateException.class)
    public void testShort() {
        environment.getShort("global.namespace", (short) 2);
    }

    @Test(expected = IllegalStateException.class)
    public void testByte() {
        environment.getByte("global.namespace", (byte) 2);
    }

    @Test(expected = IllegalStateException.class)
    public void testFloat() {
        environment.getFloat("global.namespace", 2f);
    }

    @Test(expected = IllegalStateException.class)
    public void testDouble() {
        environment.getDouble("global.namespace", 2d);
    }

    @Test(expected = NoSuchElementException.class)
    public void testIntNoElement() {
        environment.getInt("global.namespace.not.exist");
    }

    @Test(expected = NoSuchElementException.class)
    public void testBooleanNoElement() {
        environment.getBoolean("global.namespace.not.exist");
    }

    @Test(expected = NoSuchElementException.class)
    public void testShortNoElement() {
        environment.getShort("global.namespace.not.exist");
    }

    @Test(expected = NoSuchElementException.class)
    public void testByteNoElement() {
        environment.getByte("global.namespace.not.exist");
    }

    @Test(expected = NoSuchElementException.class)
    public void testFloatNoElement() {
        environment.getFloat("global.namespace.not.exist");
    }

    @Test(expected = NoSuchElementException.class)
    public void testDoubleNoElement() {
        environment.getDouble("global.namespace.not.exist");
    }

    @Test
    public void testParseMap() {
        Map<String, Object> stringObjectMap = environment.parseMap("");
        assertEquals(3, stringObjectMap.size());
    }

    @Test
    public void testParseMapFromClassPath() {
        ConfigManager configManager = environment.parseFromClassPath("trpc_java.yaml");
        assertEquals("wechat", configManager.getServerConfig().getApp());
    }

    @Test
    public void testGetInternalProperty() {
        Object internalProperty = environment.getInternalProperty("server.app");
        assertEquals("wechat", internalProperty);
    }
}
