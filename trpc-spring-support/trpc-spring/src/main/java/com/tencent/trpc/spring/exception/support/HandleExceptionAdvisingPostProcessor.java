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

package com.tencent.trpc.spring.exception.support;

import com.tencent.trpc.spring.exception.annotation.TRpcHandleException;
import com.tencent.trpc.spring.exception.api.ExceptionHandler;
import com.tencent.trpc.spring.exception.api.ExceptionHandlerResolver;
import com.tencent.trpc.spring.exception.api.ExceptionResultTransformer;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodIntrospector.MetadataLookup;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Implements {@link BeanPostProcessor} to wire tRPC {@link HandleExceptionAdvisor}.
 *
 * @see AbstractBeanFactoryAwareAdvisingPostProcessor
 * @see HandleExceptionAdvisor
 */
public class HandleExceptionAdvisingPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {

    private static final long serialVersionUID = -364503933578465142L;

    private static final MetadataLookup<TRpcHandleException> HANDLER_EXCEPTION_METHOD_FILTER = method ->
            AnnotatedElementUtils.findMergedAnnotation(method, TRpcHandleException.class);

    private final HandleExceptionInterceptor advice;

    /**
     * Construct {@link HandleExceptionAdvisingPostProcessor}
     * @param exceptionHandlerResolverSupplier {@link Supplier} for {@link ExceptionHandlerResolver}
     * @param exceptionResultTransformSupplier {@link Supplier} for {@link ExceptionResultTransformer}
     */
    public HandleExceptionAdvisingPostProcessor(
            @Nullable Supplier<ExceptionHandlerResolver> exceptionHandlerResolverSupplier,
            @Nullable Supplier<ExceptionResultTransformer> exceptionResultTransformSupplier) {
        setBeforeExistingAdvisors(true);
        this.advice = new HandleExceptionInterceptor(exceptionHandlerResolverSupplier,
                exceptionResultTransformSupplier);
        this.advisor = new HandleExceptionAdvisor(this.advice);
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) {
        super.setBeanFactory(beanFactory);
        if (advisor instanceof BeanFactoryAware) {
            ((BeanFactoryAware) advisor).setBeanFactory(beanFactory);
        }
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) {
        try {
            if (isEligible(bean, beanName)) {
                MethodIntrospector.selectMethods(ClassUtils.getUserClass(bean), HANDLER_EXCEPTION_METHOD_FILTER)
                        .forEach(this::validateQualifierBean);
            }
        } catch (Exception ex) {
            throw new BeanCreationException(beanName,
                    "Post process TRpcService exception handler encountered an error", ex);
        }
        return super.postProcessAfterInitialization(bean, beanName);
    }

    /**
     * Check if bean name specified by @TRpcHandleException exists.
     * If not, throws {@link IllegalStateException}.
     */
    private void validateQualifierBean(Method method, TRpcHandleException handleException) {
        if (StringUtils.hasText(handleException.transform())) {
            advice.getQualifierBean(handleException.transform(), ExceptionResultTransformer.class);
        }
        if (StringUtils.hasText(handleException.handler())) {
            advice.getQualifierBean(handleException.handler(), ExceptionHandler.class);
        }
    }

}
