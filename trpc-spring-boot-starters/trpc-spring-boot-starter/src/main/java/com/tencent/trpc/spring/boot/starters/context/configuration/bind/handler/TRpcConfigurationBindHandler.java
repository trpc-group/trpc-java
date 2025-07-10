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

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;

/**
 * Dynamically modify {@code Bindable} by determining the binding type at {@code onStart}
 */
public class TRpcConfigurationBindHandler extends BoundPropertiesTrackingBindHandler {

    private final List<TargetBindableReplacement> bindableEditors = Lists.newArrayList(
            new ServiceProviderTargetBindableReplacement(),
            new ListDetectingTargetBindableReplacement()
    );

    public TRpcConfigurationBindHandler(Consumer<ConfigurationProperty> consumer) {
        super(consumer);
    }

    @Override
    public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
        return super.onStart(name, replaceTargetOnStart(name, target, context), context);
    }

    private <T> Bindable<T> replaceTargetOnStart(ConfigurationPropertyName name, Bindable<T> target,
            BindContext context) {
        return bindableEditors.stream()
                .map(replacement -> replacement.tryReplace(name, target, context))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(target);
    }
}
