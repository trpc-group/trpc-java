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

package com.tencent.trpc.proto.http.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.tencent.trpc.transport.http.common.Constants.KEYSTORE_PATH;
import static com.tencent.trpc.transport.http.common.Constants.KEYSTORE_PASS;
import static com.tencent.trpc.transport.http.common.Constants.HTTPS_SCHEME;
import static com.tencent.trpc.transport.http.common.Constants.HTTP_SCHEME;

import com.tencent.trpc.core.common.ShutdownListener;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.AbstractRpcClient;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 测试 AbstractConsumerInvoker 的 ShutdownListener 功能
 */
public class AbstractConsumerInvokerTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractConsumerInvokerTest.class);

    private AbstractRpcClient mockClient;
    private ConsumerConfig<TestService> mockConfig;
    private ProtocolConfig mockProtocolConfig;
    private BackendConfig mockBackendConfig;
    private WorkerPool mockWorkerPool;

    private TestConsumerInvoker testInvoker;

    @Before
    public void setUp() {
        // 创建 mock 对象
        mockClient = mock(AbstractRpcClient.class);
        mockConfig = mock(ConsumerConfig.class);
        mockProtocolConfig = mock(ProtocolConfig.class);
        mockBackendConfig = mock(BackendConfig.class);
        mockWorkerPool = mock(WorkerPool.class);

        // 设置 mock 对象的行为
        when(mockConfig.getBackendConfig()).thenReturn(mockBackendConfig);
        when(mockBackendConfig.getWorkerPoolObj()).thenReturn(mockWorkerPool);
        when(mockProtocolConfig.getIp()).thenReturn("127.0.0.1");
        when(mockProtocolConfig.getPort()).thenReturn(8080);
        when(mockProtocolConfig.getExtMap()).thenReturn(new HashMap<String, Object>());

        // 创建测试实例
        testInvoker = new TestConsumerInvoker(mockClient, mockConfig, mockProtocolConfig);
    }

    @After
    public void tearDown() {
        // 重置静态状态
        AbstractConsumerInvoker.reset();
    }

    /**
     * 测试 ShutdownListener 不为空
     */
    @Test
    public void testShutdownListenerNotNull() {
        ShutdownListener shutdownListener = testInvoker.getShutdownListener();
        assertNotNull("ShutdownListener should not be null", shutdownListener);
    }

    /**
     * 测试 onShutdown 方法的日志输出和执行
     */
    @Test
    public void testOnShutdownExecution() {
        // 获取 ShutdownListener
        ShutdownListener shutdownListener = testInvoker.getShutdownListener();
        assertNotNull("ShutdownListener should not be null", shutdownListener);

        // 测试 onShutdown 方法不会抛出异常
        try {
            shutdownListener.onShutdown();
            // 如果没有异常，测试通过
            assertTrue("onShutdown method should execute without exceptions", true);
        } catch (Exception e) {
            throw new AssertionError("onShutdown method should not throw exceptions", e);
        }
    }

    /**
     * 测试多次调用 onShutdown 方法的安全性
     */
    @Test
    public void testMultipleOnShutdownCalls() {
        ShutdownListener shutdownListener = testInvoker.getShutdownListener();
        assertNotNull("ShutdownListener should not be null", shutdownListener);

        // 多次调用 onShutdown 方法，确保不会出现异常
        try {
            shutdownListener.onShutdown();
            shutdownListener.onShutdown();
            shutdownListener.onShutdown();
            assertTrue("Multiple onShutdown calls should be safe", true);
        } catch (Exception e) {
            throw new AssertionError("Multiple onShutdown calls should not throw exceptions", e);
        }
    }

    /**
     * 测试 ShutdownListener 的类型
     */
    @Test
    public void testShutdownListenerType() {
        ShutdownListener shutdownListener = testInvoker.getShutdownListener();
        assertNotNull("ShutdownListener should not be null", shutdownListener);
        
        // 验证 ShutdownListener 是内部类的实例
        String className = shutdownListener.getClass().getSimpleName();
        assertTrue("ShutdownListener should be InternalShutdownListener", 
                className.contains("InternalShutdownListener"));
    }

    /**
     * 测试静态方法 stop() 和 reset() 的调用
     */
    @Test
    public void testStaticMethods() {
        try {
            // 测试静态方法调用不会抛出异常
            AbstractConsumerInvoker.stop();
            AbstractConsumerInvoker.reset();
            assertTrue("Static methods should execute without exceptions", true);
        } catch (Exception e) {
            throw new AssertionError("Static methods should not throw exceptions", e);
        }
    }

    /**
     * 测试 HTTP 协议的默认配置（不包含 keystore 配置）
     */
    @Test
    public void testHttpSchemeWithoutKeystore() throws Exception {
        // 创建不包含 keystore 配置的 extMap
        Map<String, Object> extMap = new HashMap<>();
        when(mockProtocolConfig.getExtMap()).thenReturn(extMap);

        // 创建新的测试实例
        TestConsumerInvoker httpInvoker = new TestConsumerInvoker(mockClient, mockConfig, mockProtocolConfig);

        // 通过反射获取 scheme 字段来验证
        Field schemeField = AbstractConsumerInvoker.class.getDeclaredField("scheme");
        schemeField.setAccessible(true);
        String scheme = (String) schemeField.get(httpInvoker);

        assertEquals("Should use HTTP scheme when keystore is not configured", HTTP_SCHEME, scheme);
    }

    /**
     * 测试 HTTPS 协议的配置（包含 keystore 配置）
     */
    @Test
    public void testHttpsSchemeWithKeystore() throws Exception {
        // 创建包含 keystore 配置的 extMap
        Map<String, Object> extMap = new HashMap<>();
        extMap.put(KEYSTORE_PATH, "/path/to/keystore.jks");
        extMap.put(KEYSTORE_PASS, "password123");
        when(mockProtocolConfig.getExtMap()).thenReturn(extMap);

        // 创建新的测试实例
        TestConsumerInvoker httpsInvoker = new TestConsumerInvoker(mockClient, mockConfig, mockProtocolConfig);

        // 通过反射获取 scheme 字段来验证
        Field schemeField = AbstractConsumerInvoker.class.getDeclaredField("scheme");
        schemeField.setAccessible(true);
        String scheme = (String) schemeField.get(httpsInvoker);

        assertEquals("Should use HTTPS scheme when keystore is configured", HTTPS_SCHEME, scheme);
    }

    /**
     * 测试只有 KEYSTORE_PATH 但没有 KEYSTORE_PASS 的情况（应该使用 HTTP）
     */
    @Test
    public void testHttpSchemeWithOnlyKeystorePath() throws Exception {
        // 创建只包含 KEYSTORE_PATH 的 extMap
        Map<String, Object> extMap = new HashMap<>();
        extMap.put(KEYSTORE_PATH, "/path/to/keystore.jks");
        when(mockProtocolConfig.getExtMap()).thenReturn(extMap);

        // 创建新的测试实例
        TestConsumerInvoker httpInvoker = new TestConsumerInvoker(mockClient, mockConfig, mockProtocolConfig);

        // 通过反射获取 scheme 字段来验证
        Field schemeField = AbstractConsumerInvoker.class.getDeclaredField("scheme");
        schemeField.setAccessible(true);
        String scheme = (String) schemeField.get(httpInvoker);

        assertEquals("Should use HTTP scheme when only KEYSTORE_PATH is configured", HTTP_SCHEME, scheme);
    }

    /**
     * 测试只有 KEYSTORE_PASS 但没有 KEYSTORE_PATH 的情况（应该使用 HTTP）
     */
    @Test
    public void testHttpSchemeWithOnlyKeystorePass() throws Exception {
        // 创建只包含 KEYSTORE_PASS 的 extMap
        Map<String, Object> extMap = new HashMap<>();
        extMap.put(KEYSTORE_PASS, "password123");
        when(mockProtocolConfig.getExtMap()).thenReturn(extMap);

        // 创建新的测试实例
        TestConsumerInvoker httpInvoker = new TestConsumerInvoker(mockClient, mockConfig, mockProtocolConfig);

        // 通过反射获取 scheme 字段来验证
        Field schemeField = AbstractConsumerInvoker.class.getDeclaredField("scheme");
        schemeField.setAccessible(true);
        String scheme = (String) schemeField.get(httpInvoker);

        assertEquals("Should use HTTP scheme when only KEYSTORE_PASS is configured", HTTP_SCHEME, scheme);
    }

    /**
     * 测试 URI 构建在不同协议下的正确性
     */
    @Test
    public void testUriConstructionWithDifferentSchemes() throws Exception {
        // 设置 mock 对象的基本配置
        when(mockConfig.getBackendConfig().getBasePath()).thenReturn("/api");
        
        // 创建 mock request 和 invocation
        Request mockRequest = mock(Request.class);
        com.tencent.trpc.core.rpc.RpcInvocation mockInvocation = mock(com.tencent.trpc.core.rpc.RpcInvocation.class);
        when(mockRequest.getInvocation()).thenReturn(mockInvocation);
        when(mockInvocation.getFunc()).thenReturn("/test");

        // 测试 HTTP 协议的 URI
        Map<String, Object> httpExtMap = new HashMap<>();
        when(mockProtocolConfig.getExtMap()).thenReturn(httpExtMap);
        TestConsumerInvoker httpInvoker = new TestConsumerInvoker(mockClient, mockConfig, mockProtocolConfig);
        URI httpUri = httpInvoker.getUri(mockRequest);
        assertEquals("HTTP URI scheme should be http", HTTP_SCHEME, httpUri.getScheme());

        // 测试 HTTPS 协议的 URI
        Map<String, Object> httpsExtMap = new HashMap<>();
        httpsExtMap.put(KEYSTORE_PATH, "/path/to/keystore.jks");
        httpsExtMap.put(KEYSTORE_PASS, "password123");
        when(mockProtocolConfig.getExtMap()).thenReturn(httpsExtMap);
        TestConsumerInvoker httpsInvoker = new TestConsumerInvoker(mockClient, mockConfig, mockProtocolConfig);
        URI httpsUri = httpsInvoker.getUri(mockRequest);
        assertEquals("HTTPS URI scheme should be https", HTTPS_SCHEME, httpsUri.getScheme());
    }

    /**
     * 测试用的 ConsumerInvoker 实现类
     */
    private static class TestConsumerInvoker extends AbstractConsumerInvoker<TestService> {

        public TestConsumerInvoker(AbstractRpcClient client, ConsumerConfig<TestService> config,
                ProtocolConfig protocolConfig) {
            super(client, config, protocolConfig);
        }

        @Override
        public Response send(Request request) throws Exception {
            // 简单的测试实现
            return null;
        }
    }

    /**
     * 测试用的服务接口
     */
    private interface TestService {
        String testMethod(String input);
    }
}