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

package com.tencent.trpc.core.common.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Inherited
@Target(value = ElementType.FIELD)
public @interface ConfigProperty {

    /**
     * The corresponding configuration name, default is "",
     * then convert the corresponding field from camel case to underscore lowercase.
     *
     * @return name String default ""
     */
    String name() default "";

    /**
     * Property Value
     *
     * @return value String default ""
     */
    String value() default "";

    /**
     * ConfigProperty type
     *
     * @return type Class default <code> String.class </code>
     */
    Class<?> type() default String.class;

    /**
     * Is override
     *
     * @return boolean default false
     */
    boolean override() default false;

    /**
     * Is moreZero
     *
     * @return boolean default true
     */
    boolean moreZero() default true;

    /**
     * Is needMerged. only for set
     *
     * @return boolean default false
     */
    boolean needMerged() default false;

}