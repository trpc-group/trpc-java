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

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.handler.IgnoreErrorsBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.ResolvableType;

/**
 * Prevents Spring Boot from incorrectly binding a List as a Map.
 *
 * Background:
 * When the initial target is a map and the value type is not explicit (e.g., {@code Map<String, Object>}),
 * Spring Boot's bind process defaults the value type to a Map. This results in a List[Object] being bound as a
 * Map[0->Object, 1->Object].
 *
 * @see org.springframework.boot.context.properties.bind.MapBinder Spring Boot MapBinder
 * @see org.springframework.boot.context.properties.bind.MapBinder#isValueTreatedAsNestedMap
 *         isValueTreatedAsNestedMap method
 */
public class ListDetectingTargetBindableReplacement implements TargetBindableReplacement {

    private static final Bindable<?> NESTED_MAP_LIST_TYPE = Bindable.of(
            ResolvableType.forClassWithGenerics(List.class, Map.class));

    private static final Bindable<?> NESTED_OBJECT_LIST_TYPE = Bindable.of(
            ResolvableType.forClassWithGenerics(List.class, Object.class));

    private static final BindHandler IGNORE_ERRORS_BIND_HANDLER = new IgnoreErrorsBindHandler();

    private static final int MAP_VALUE_GENERIC_INDEX = 1;

    @SuppressWarnings("unchecked")
    @Override
    public <T> Bindable<T> tryReplace(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
        if (isAmbiguousMap(target.getType())) {
            return (Bindable<T>) tryDetectBindableList(name, context);
        }
        return null;
    }

    private boolean isAmbiguousMap(ResolvableType type) {
        boolean isMap = Map.class.isAssignableFrom(type.resolve(Object.class));
        if (!isMap) {
            return false;
        }
        ResolvableType valueType = type.getGeneric(MAP_VALUE_GENERIC_INDEX);
        return valueType.resolve() != null && Object.class.equals(valueType.resolve());
    }

    private Bindable<?> tryDetectBindableList(ConfigurationPropertyName name, BindContext context) {
        Binder binder = context.getBinder();
        if (tryBind(binder, name, NESTED_MAP_LIST_TYPE)) {
            return NESTED_MAP_LIST_TYPE;
        }
        if (tryBind(binder, name, NESTED_OBJECT_LIST_TYPE)) {
            return NESTED_OBJECT_LIST_TYPE;
        }
        return null;
    }

    private boolean tryBind(Binder binder, ConfigurationPropertyName name, Bindable<?> bindable) {
        return binder.bind(name, bindable, IGNORE_ERRORS_BIND_HANDLER).isBound();
    }
}
