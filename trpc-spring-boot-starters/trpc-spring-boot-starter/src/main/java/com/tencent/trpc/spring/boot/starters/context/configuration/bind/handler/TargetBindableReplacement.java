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

package com.tencent.trpc.spring.boot.starters.context.configuration.bind.handler;

import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;

/**
 * Modify the target type before binding properties
 */
public interface TargetBindableReplacement {

    /**
     * Try to replace with custom rules, if unable to modify return null
     *
     * @param name Name about to be bound
     * @param target Binding target
     * @param context Context
     * @param <T> Type
     * @return bindable
     */
    <T> Bindable<T> tryReplace(ConfigurationPropertyName name, Bindable<T> target, BindContext context);
}