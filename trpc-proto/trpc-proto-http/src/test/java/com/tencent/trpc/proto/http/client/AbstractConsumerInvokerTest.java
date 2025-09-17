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
 * Test the ShutdownListener functionality of AbstractConsumerInvoker
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
        // Create mock objects
        mockClient = mock(AbstractRpcClient.class);
        mockConfig = mock(ConsumerConfig.class);
        mockProtocolConfig = mock(ProtocolConfig.class);
        mockBackendConfig = mock(BackendConfig.class);
        mockWorkerPool = mock(WorkerPool.class);

        // Configure mock object behavior
        when(mockConfig.getBackendConfig()).thenReturn(mockBackendConfig);
        when(mockBackendConfig.getWorkerPoolObj()).thenReturn(mockWorkerPool);
        when(mockProtocolConfig.getIp()).thenReturn("127.0.0.1");
        when(mockProtocolConfig.getPort()).thenReturn(8080);
        when(mockProtocolConfig.getExtMap()).thenReturn(new HashMap<String, Object>());

        // Create test instance
        testInvoker = new TestConsumerInvoker(mockClient, mockConfig, mockProtocolConfig);
    }

    @After
    public void tearDown() {
        // Reset static state
        AbstractConsumerInvoker.reset();
    }

    /**
     * Test that ShutdownListener is not null
     */
    @Test
    public void testShutdownListenerNotNull() {
        ShutdownListener shutdownListener = testInvoker.getShutdownListener();
        assertNotNull("ShutdownListener should not be null", shutdownListener);
    }

    /**
     * Test the log output and execution of onShutdown method
     */
    @Test
    public void testOnShutdownExecution() {
        // Get ShutdownListener
        ShutdownListener shutdownListener = testInvoker.getShutdownListener();
        assertNotNull("ShutdownListener should not be null", shutdownListener);

        // Test that onShutdown method does not throw exceptions
        try {
            shutdownListener.onShutdown();
            // If no exception occurs, test passes
            assertTrue("onShutdown method should execute without exceptions", true);
        } catch (Exception e) {
            throw new AssertionError("onShutdown method should not throw exceptions", e);
        }
    }

    /**
     * Test the safety of calling onShutdown method multiple times
     */
    @Test
    public void testMultipleOnShutdownCalls() {
        ShutdownListener shutdownListener = testInvoker.getShutdownListener();
        assertNotNull("ShutdownListener should not be null", shutdownListener);

        // Call onShutdown method multiple times to ensure no exceptions occur
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
     * Test the type of ShutdownListener
     */
    @Test
    public void testShutdownListenerType() {
        ShutdownListener shutdownListener = testInvoker.getShutdownListener();
        assertNotNull("ShutdownListener should not be null", shutdownListener);
        
        // Verify that ShutdownListener is an instance of the inner class
        String className = shutdownListener.getClass().getSimpleName();
        assertTrue("ShutdownListener should be InternalShutdownListener", 
                className.contains("InternalShutdownListener"));
    }

    /**
     * Test the invocation of static methods stop() and reset()
     */
    @Test
    public void testStaticMethods() {
        try {
            // Test that static method calls do not throw exceptions
            AbstractConsumerInvoker.stop();
            AbstractConsumerInvoker.reset();
            assertTrue("Static methods should execute without exceptions", true);
        } catch (Exception e) {
            throw new AssertionError("Static methods should not throw exceptions", e);
        }
    }

    /**
     * Test default configuration for HTTP protocol (without keystore configuration)
     */
    @Test
    public void testHttpSchemeWithoutKeystore() throws Exception {
        // Create extMap without keystore configuration
        Map<String, Object> extMap = new HashMap<>();
        when(mockProtocolConfig.getExtMap()).thenReturn(extMap);

        // Create new test instance
        TestConsumerInvoker httpInvoker = new TestConsumerInvoker(mockClient, mockConfig, mockProtocolConfig);

        // Verify by accessing the scheme field via reflection
        Field schemeField = AbstractConsumerInvoker.class.getDeclaredField("scheme");
        schemeField.setAccessible(true);
        String scheme = (String) schemeField.get(httpInvoker);

        assertEquals("Should use HTTP scheme when keystore is not configured", HTTP_SCHEME, scheme);
    }

    /**
     * Test HTTPS protocol configuration (with keystore configuration)
     */
    @Test
    public void testHttpsSchemeWithKeystore() throws Exception {
        // Create extMap with keystore configuration
        Map<String, Object> extMap = new HashMap<>();
        extMap.put(KEYSTORE_PATH, "/path/to/keystore.jks");
        extMap.put(KEYSTORE_PASS, "password123");
        when(mockProtocolConfig.getExtMap()).thenReturn(extMap);

        // Create new test instance
        TestConsumerInvoker httpsInvoker = new TestConsumerInvoker(mockClient, mockConfig, mockProtocolConfig);

        // Verify by accessing the scheme field via reflection
        Field schemeField = AbstractConsumerInvoker.class.getDeclaredField("scheme");
        schemeField.setAccessible(true);
        String scheme = (String) schemeField.get(httpsInvoker);

        assertEquals("Should use HTTPS scheme when keystore is configured", HTTPS_SCHEME, scheme);
    }

    /**
     * Test case with only KEYSTORE_PATH but no KEYSTORE_PASS (should use HTTP)
     */
    @Test
    public void testHttpSchemeWithOnlyKeystorePath() throws Exception {
        // Create extMap with only KEYSTORE_PATH
        Map<String, Object> extMap = new HashMap<>();
        extMap.put(KEYSTORE_PATH, "/path/to/keystore.jks");
        when(mockProtocolConfig.getExtMap()).thenReturn(extMap);

        // Create new test instance
        TestConsumerInvoker httpInvoker = new TestConsumerInvoker(mockClient, mockConfig, mockProtocolConfig);

        // Verify by accessing the scheme field via reflection
        Field schemeField = AbstractConsumerInvoker.class.getDeclaredField("scheme");
        schemeField.setAccessible(true);
        String scheme = (String) schemeField.get(httpInvoker);

        assertEquals("Should use HTTP scheme when only KEYSTORE_PATH is configured", HTTP_SCHEME, scheme);
    }

    /**
     * Test case with only KEYSTORE_PASS but no KEYSTORE_PATH (should use HTTP)
     */
    @Test
    public void testHttpSchemeWithOnlyKeystorePass() throws Exception {
        // Create extMap with only KEYSTORE_PASS
        Map<String, Object> extMap = new HashMap<>();
        extMap.put(KEYSTORE_PASS, "password123");
        when(mockProtocolConfig.getExtMap()).thenReturn(extMap);

        // Create new test instance
        TestConsumerInvoker httpInvoker = new TestConsumerInvoker(mockClient, mockConfig, mockProtocolConfig);

        // Verify by accessing the scheme field via reflection
        Field schemeField = AbstractConsumerInvoker.class.getDeclaredField("scheme");
        schemeField.setAccessible(true);
        String scheme = (String) schemeField.get(httpInvoker);

        assertEquals("Should use HTTP scheme when only KEYSTORE_PASS is configured", HTTP_SCHEME, scheme);
    }

    /**
     * Test URI construction correctness under different protocols
     */
    @Test
    public void testUriConstructionWithDifferentSchemes() throws Exception {
        // Configure basic settings for mock objects
        when(mockConfig.getBackendConfig().getBasePath()).thenReturn("/api");
        
        // Create mock request and invocation
        Request mockRequest = mock(Request.class);
        com.tencent.trpc.core.rpc.RpcInvocation mockInvocation = mock(com.tencent.trpc.core.rpc.RpcInvocation.class);
        when(mockRequest.getInvocation()).thenReturn(mockInvocation);
        when(mockInvocation.getFunc()).thenReturn("/test");

        // Test URI for HTTP protocol
        Map<String, Object> httpExtMap = new HashMap<>();
        when(mockProtocolConfig.getExtMap()).thenReturn(httpExtMap);
        TestConsumerInvoker httpInvoker = new TestConsumerInvoker(mockClient, mockConfig, mockProtocolConfig);
        URI httpUri = httpInvoker.getUri(mockRequest);
        assertEquals("HTTP URI scheme should be http", HTTP_SCHEME, httpUri.getScheme());

        // Test URI for HTTPS protocol
        Map<String, Object> httpsExtMap = new HashMap<>();
        httpsExtMap.put(KEYSTORE_PATH, "/path/to/keystore.jks");
        httpsExtMap.put(KEYSTORE_PASS, "password123");
        when(mockProtocolConfig.getExtMap()).thenReturn(httpsExtMap);
        TestConsumerInvoker httpsInvoker = new TestConsumerInvoker(mockClient, mockConfig, mockProtocolConfig);
        URI httpsUri = httpsInvoker.getUri(mockRequest);
        assertEquals("HTTPS URI scheme should be https", HTTPS_SCHEME, httpsUri.getScheme());
    }

    /**
     * Test ConsumerInvoker implementation class for testing purposes
     */
    private static class TestConsumerInvoker extends AbstractConsumerInvoker<TestService> {

        public TestConsumerInvoker(AbstractRpcClient client, ConsumerConfig<TestService> config,
                ProtocolConfig protocolConfig) {
            super(client, config, protocolConfig);
        }

        @Override
        public Response send(Request request) throws Exception {
            // Simple test implementation
            return null;
        }
    }

    /**
     * Test service interface for testing purposes
     */
    private interface TestService {
        String testMethod(String input);
    }
}