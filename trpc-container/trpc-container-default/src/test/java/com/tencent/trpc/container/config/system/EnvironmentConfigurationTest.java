package com.tencent.trpc.container.config.system;

import com.tencent.trpc.container.config.ApplicationConfigParser;
import com.tencent.trpc.core.extension.ExtensionLoader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EnvironmentConfigurationTest {

    private Environment environment;

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
    public void testGetInternalProperty() {
        Object internalProperty = environment.getInternalProperty("server.app");
        Assert.assertEquals(internalProperty, "wechat");
    }
}