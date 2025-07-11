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

package com.tencent.trpc.spring.aop;

import org.aopalliance.aop.Advice;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.NonNull;

/**
 * Extended {@link AbstractTRpcServiceAdvisor} and provide {@link BeanFactoryAware} capability
 *
 * @see AbstractTRpcServiceAdvisor
 */
public abstract class AbstractAwareTRpcServiceAdvisor extends AbstractTRpcServiceAdvisor implements BeanFactoryAware {

    private static final long serialVersionUID = 453491697178338711L;

    protected BeanFactory beanFactory;

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        Advice advice = getAdvice();
        if (advice instanceof BeanFactoryAware) {
            ((BeanFactoryAware) advice).setBeanFactory(beanFactory);
        }
    }

}
