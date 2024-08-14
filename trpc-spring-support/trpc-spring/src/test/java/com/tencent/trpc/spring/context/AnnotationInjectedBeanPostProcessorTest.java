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

package com.tencent.trpc.spring.context;

import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.annotation.InjectionMetadata.InjectedElement;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;

public class AnnotationInjectedBeanPostProcessorTest {

    @Mock
    private ConfigurableListableBeanFactory mockBeanFactory;
    @Mock
    private Environment mockEnvironment;

    private ConcreteAnnotationInjectedBeanPostProcessor processor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        processor = new ConcreteAnnotationInjectedBeanPostProcessor();
        processor.setBeanFactory(mockBeanFactory);
        processor.setEnvironment(mockEnvironment);
        processor.setBeanClassLoader(new DefaultResourceLoader().getClassLoader());
    }

    @Test
    public void testPostProcessProperties() {
        Object bean = new Object();
        PropertyValues pvs = new PropertyValues() {
            @Override
            public PropertyValue[] getPropertyValues() {
                return new PropertyValue[0];
            }

            @Override
            public PropertyValue getPropertyValue(String s) {
                return new PropertyValue(s, "testValue");
            }

            @Override
            public PropertyValues changesSince(PropertyValues propertyValues) {
                return propertyValues;
            }

            @Override
            public boolean contains(String s) {
                return false;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        };
        String beanName = "testBean";
        PropertyValues processedPvs = processor.postProcessProperties(pvs, bean, beanName);
        Assert.assertNotNull(processedPvs);
    }

    @Test
    public void testPostProcessMergedBeanDefinition() {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        Class<?> beanType = Object.class;
        String beanName = "testBean";
        processor.postProcessMergedBeanDefinition(beanDefinition, beanType, beanName);
        Assert.assertNotNull(beanDefinition);
    }

    @Test
    public void testDestroy() {
        try {
            processor.destroy();
        } catch (Exception e) {
            fail("Destroy method should not throw an exception");
        }
    }

    static class ConcreteAnnotationInjectedBeanPostProcessor extends AnnotationInjectedBeanPostProcessor {

        public ConcreteAnnotationInjectedBeanPostProcessor() {
            super(MyAnnotation.class);
        }

        @Override
        protected String buildInjectedObjectCacheKey(AnnotationAttributes attributes, Object bean, String beanName,
                Class<?> injectedType, InjectedElement injectedElement) {
            return attributes.toString(); // 示例：使用注解属性的字符串表示作为缓存键
        }

        @Override
        protected Object doGetInjectedBean(AnnotationAttributes attributes, Object bean, String beanName,
                Class<?> injectedType, InjectedElement injectedElement) throws Exception {
            return Mockito.mock(injectedType);
        }
    }

    public @interface MyAnnotation {

    }
}