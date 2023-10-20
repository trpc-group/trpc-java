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

package com.tencent.trpc.spring.boot.starters.context.configuration.bind.handler;

import java.util.function.Consumer;
import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;

/**
 * Tracks successfully bound properties
 */
public class BoundPropertiesTrackingBindHandler extends AbstractBindHandler {

    private final Consumer<ConfigurationProperty> consumer;

    public BoundPropertiesTrackingBindHandler(
            Consumer<ConfigurationProperty> consumer) {
        this.consumer = consumer;
    }

    @Override
    public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
        if (context.getConfigurationProperty() != null && name.equals(context.getConfigurationProperty().getName())) {
            this.consumer.accept(context.getConfigurationProperty());
        }
        return super.onSuccess(name, target, context, result);
    }
}