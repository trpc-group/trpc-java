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

package com.tencent.trpc.spring.context.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.tencent.trpc.spring.test.TestSpringApplication;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestSpringApplication.class)
public class BeanFactoryAwareSupportTest {

    private BeanFactoryAwareSupport support;

    @Autowired
    private BeanFactory beanFactory;

    @Before
    public void setUp() {
        support = new BeanFactoryAwareSupport();
        support.setBeanFactory(beanFactory);
    }

    @Test
    public void testSetBeanFactory() {
        assertNotNull(beanFactory);
        assertEquals(beanFactory, support.beanFactory);
    }

    @Test
    public void testGetBean() {
        TaskExecutionAutoConfiguration bean = support.getBean(TaskExecutionAutoConfiguration.class);
        assertNotNull(bean);
        support.setBeanFactory(null);
        bean = support.getBean(TaskExecutionAutoConfiguration.class);
        assertNull(bean);
    }

    @Test
    public void testGetQualifierBean() {
        String qualifier = "applicationTaskExecutor";
        ThreadPoolTaskExecutor bean = support.getQualifierBean(qualifier, ThreadPoolTaskExecutor.class);
        assertNotNull(bean);
        support.setBeanFactory(null);
        try {
            support.getQualifierBean(qualifier, ThreadPoolTaskExecutor.class);
        } catch (Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
            assertTrue(e.getMessage()
                    .contains("BeanFactory must be provided to access qualified bean 'applicationTaskExecutor'"));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetQualifierBeanWithBeanFactoryIsNull() {
        support.setBeanFactory(null);
        String qualifier = "applicationTaskExecutor";
        ThreadPoolTaskExecutor bean = support.getQualifierBean(qualifier, ThreadPoolTaskExecutor.class);
        assertNull(bean);
    }

    @Test
    public void testGetQualifierBeanWithNullQualifier() {
        Object qualifierBean = support.getQualifierBean("", Object.class);
        Assert.assertNull(qualifierBean);
    }

}
