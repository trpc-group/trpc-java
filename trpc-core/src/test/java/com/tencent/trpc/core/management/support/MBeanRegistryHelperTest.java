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

package com.tencent.trpc.core.management.support;

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for MBeanRegistryHelper to cover exception handling branches
 */
public class MBeanRegistryHelperTest {

    /**
     * Test registerMBean method with normal operation - should not throw exception
     */
    @Test
    public void testRegisterMBeanNormal() throws Exception {
        // Create test objects
        Object testObject = new TestMBeanImpl();
        ObjectName objectName = new ObjectName("test:type=TestMBean");

        // This should not throw exception
        MBeanRegistryHelper.registerMBean(testObject, objectName);
        
        // Clean up - unregister the MBean
        MBeanRegistryHelper.unregisterMBean(objectName);
    }

    /**
     * Test registerMBean method with invalid ObjectName - should trigger exception handling
     */
    @Test
    public void testRegisterMBeanWithInvalidObjectName() throws Exception {
        // Create test objects
        Object testObject = new Object(); // Not a valid MBean
        ObjectName objectName = new ObjectName("test:type=TestMBean");

        // This should trigger exception handling (NotCompliantMBeanException) but not throw
        MBeanRegistryHelper.registerMBean(testObject, objectName);
    }

    /**
     * Test registerMBean method with duplicate registration - should trigger exception handling
     */
    @Test
    public void testRegisterMBeanDuplicate() throws Exception {
        // Create test objects
        Object testObject = new TestMBeanImpl();
        ObjectName objectName = new ObjectName("test:type=DuplicateTestMBean");

        try {
            // Register first time - should succeed
            MBeanRegistryHelper.registerMBean(testObject, objectName);
            
            // Register second time - should trigger exception handling (InstanceAlreadyExistsException) but not throw
            MBeanRegistryHelper.registerMBean(testObject, objectName);
        } finally {
            // Clean up
            MBeanRegistryHelper.unregisterMBean(objectName);
        }
    }

    /**
     * Test unregisterMBean method with valid ObjectName - should work normally
     */
    @Test
    public void testUnregisterMBeanNormal() throws Exception {
        // Create ObjectName for a simple test
        ObjectName objectName = new ObjectName("test:type=UnregisterTestMBean");
        
        // This should not throw exception even if MBean doesn't exist
        MBeanRegistryHelper.unregisterMBean(objectName);
    }

    /**
     * Test unregisterMBean method with non-existent ObjectName - should not throw exception
     */
    @Test
    public void testUnregisterMBeanNonExistent() throws Exception {
        // Create ObjectName for non-existent MBean
        ObjectName objectName = new ObjectName("test:type=NonExistentMBean");
        
        // This should not throw exception even though MBean doesn't exist
        MBeanRegistryHelper.unregisterMBean(objectName);
    }

    /**
     * Test unregisterMBean method with invalid ObjectName pattern - should trigger exception handling
     */
    @Test
    public void testUnregisterMBeanWithInvalidPattern() throws Exception {
        // Create ObjectName with pattern (which cannot be used for unregistering)
        ObjectName objectName = new ObjectName("test:type=*");
        
        // This should trigger exception handling but not throw
        MBeanRegistryHelper.unregisterMBean(objectName);
    }

    /**
     * Test unregisterMBean method exception handling by creating a scenario that triggers exception
     * This test covers the exception handling branch in unregisterMBean method
     */
    @Test
    public void testUnregisterMBeanException() throws Exception {
        // Test with null ObjectName which should cause RuntimeOperationsException
        // in the isRegistered(null) call, which will trigger the exception handling branch
        
        // This should not throw any exception - the exception should be caught and logged
        try {
            MBeanRegistryHelper.unregisterMBean(null);
            // If we reach here, it means the exception was properly caught and handled
        } catch (Exception e) {
            // If any exception escapes, the test should fail
            Assert.fail("Exception should have been caught and logged, but was thrown: " + e.getMessage());
        }
        
        // The test passes if no exception is thrown (exception is caught and logged)
        // The logger.warn("unregister mbean exception: ", e) line should be executed
        // This covers the exception handling branch that was previously untested
    }

    /**
     * Test MBean interface for testing purposes
     */
    public interface TestMBean {
        String getName();
    }

    /**
     * Test MBean implementation for testing purposes
     */
    public static class TestMBeanImpl implements TestMBean {
        @Override
        public String getName() {
            return "TestMBean";
        }
    }
}