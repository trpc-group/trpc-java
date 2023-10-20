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

import javax.annotation.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

/**
 * Base implementation of {@link BeanFactoryAware}
 */
public class BeanFactoryAwareSupport implements BeanFactoryAware {

    protected BeanFactory beanFactory;

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    /**
     * Get bean from underlying beanFactory
     *
     * @param type the type of bean to retrieve
     * @return bean, null if not exists
     */
    @Nullable
    public <T> T getBean(Class<T> type) {
        if (beanFactory == null) {
            return null;
        }
        return beanFactory.getBeanProvider(type).getIfAvailable();
    }

    /**
     * Get bean from underlying beanFactory with qualifier
     *
     * @param qualifier the qualifier for selecting between multiple bean matches
     * @param type the type of bean to retrieve
     * @return the matching bean of type T
     */
    @Nullable
    public <T> T getQualifierBean(String qualifier, Class<T> type) {
        if (StringUtils.hasText(qualifier)) {
            if (beanFactory == null) {
                throw new IllegalStateException(
                        "BeanFactory must be provided to access qualified bean '" + qualifier + "' of type '" + type
                                .getSimpleName() + "'");
            }
            return BeanFactoryAnnotationUtils.qualifiedBeanOfType(beanFactory, type, qualifier);
        }
        return null;
    }

}
