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

package com.tencent.trpc.spring.aop;

import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import javax.annotation.Nonnull;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.util.function.SingletonSupplier;

/**
 * Abstract base class for tRPC service advisors.
 * <p>Provides built-in pointcut to methods annotated with {@link TRpcMethod}
 * in classes annotated with {@link TRpcService}.</p>
 * Subclasses can provide additional pointcut by implementing {@link #getExtraPointcut}
 *
 * @see TRpcService
 * @see TRpcMethod
 */
public abstract class AbstractTRpcServiceAdvisor extends AbstractPointcutAdvisor {

    private static final long serialVersionUID = 6432211741530512650L;

    private final SingletonSupplier<Pointcut> pointcutSupplier = SingletonSupplier.of(this::buildPointcut);

    @Nonnull
    @Override
    public final Pointcut getPointcut() {
        return pointcutSupplier.obtain();
    }

    /**
     * Subclasses can provide additional pointcut by implementing this
     *
     * @return Pointcut
     */
    protected Pointcut getExtraPointcut() {
        return null;
    }

    private Pointcut buildPointcut() {
        ComposablePointcut composablePointcut = new ComposablePointcut(
                new AnnotationMatchingPointcut(TRpcService.class, TRpcMethod.class, true));
        Pointcut extraPointcut = getExtraPointcut();
        if (extraPointcut != null) {
            composablePointcut.intersection(extraPointcut);
        }
        return composablePointcut;
    }

}
