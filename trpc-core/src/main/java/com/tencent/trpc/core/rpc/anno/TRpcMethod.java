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

package com.tencent.trpc.core.rpc.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TRPC method annotation.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TRpcMethod {

    /**
     * Method name.
     */
    String name() default "";

    /**
     * Method alias, can set multiple, equivalent to /trpcServiceName/trpcMethodName.
     */
    String[] alias() default {};

    /**
     * Whether it is a generic type interface.
     *
     * @return true if it is a generic type interface
     */
    boolean isGeneric() default false;

    /**
     * Whether it is a default method. If it is a default method, it will go to this method when all routes are not
     * matched.
     *
     * @return true if it is a default method
     */
    boolean isDefault() default false;

}
