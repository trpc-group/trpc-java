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

package com.tencent.trpc.spring.exception.support;

import com.tencent.trpc.spring.aop.AbstractAwareTRpcServiceAdvisor;
import com.tencent.trpc.spring.aop.AbstractTRpcServiceAdvisor;
import com.tencent.trpc.spring.exception.annotation.TRpcHandleException;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.Pointcuts;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.lang.NonNull;

/**
 * Extending {@link AbstractTRpcServiceAdvisor} to add extra pointcut for
 * {@link TRpcHandleException} annotated classes and methods
 *
 * @see TRpcHandleException
 * @see AbstractTRpcServiceAdvisor
 * @see AbstractAwareTRpcServiceAdvisor
 */
public class HandleExceptionAdvisor extends AbstractAwareTRpcServiceAdvisor {

    private static final long serialVersionUID = 6716531841668460358L;

    private final MethodInterceptor advice;

    public HandleExceptionAdvisor(MethodInterceptor advice) {
        this.advice = advice;
    }

    @Override
    @NonNull
    public Advice getAdvice() {
        return this.advice;
    }

    @Override
    protected Pointcut getExtraPointcut() {
        return Pointcuts.union(
                new AnnotationMatchingPointcut(null, TRpcHandleException.class, true),
                new AnnotationMatchingPointcut(TRpcHandleException.class, true));
    }

}
