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

package com.tencent.trpc.opentelemetry.sdk;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Indicator template class for limiting problems with high base reporting
 */
@SuppressWarnings("rawtypes")
public final class TemporaryMetricsView {

    private static final Set<AttributeKey> DURATION_ALWAYS_INCLUDE = buildDurationAlwaysInclude();
    private static final Set<AttributeKey> DURATION_SERVER_VIEW = buildDurationServerView();
    private static final Set<AttributeKey> ACTIVE_REQUESTS_VIEW = buildActiveRequestsView();

    private static Set<AttributeKey> buildDurationAlwaysInclude() {
        Set<AttributeKey> view = new HashSet<>();
        view.add(SemanticAttributes.RPC_METHOD);
        view.add(SemanticAttributes.RPC_SYSTEM);
        view.add(SemanticAttributes.RPC_SERVICE);
        return view;
    }

    private static Set<AttributeKey> buildDurationServerView() {
        return new HashSet<>(DURATION_ALWAYS_INCLUDE);
    }

    private static Set<AttributeKey> buildActiveRequestsView() {
        return new HashSet<>();
    }

    static Attributes applyServerDurationView(Attributes startAttributes, Attributes endAttributes) {
        Set<AttributeKey> fullSet = DURATION_SERVER_VIEW;
        AttributesBuilder filtered = Attributes.builder();
        applyView(filtered, startAttributes, fullSet);
        applyView(filtered, endAttributes, fullSet);
        return filtered.build();
    }

    static Attributes applyActiveRequestsView(Attributes attributes) {
        AttributesBuilder filtered = Attributes.builder();
        applyView(filtered, attributes, ACTIVE_REQUESTS_VIEW);
        return filtered.build();
    }

    @SuppressWarnings("unchecked")
    private static void applyView(
            AttributesBuilder filtered, Attributes attributes, Set<AttributeKey> view) {
        attributes.forEach(
                (BiConsumer<AttributeKey, Object>)
                        (key, value) -> {
                            if (view.contains(key)) {
                                filtered.put(key, value);
                            }
                        });
    }

    private TemporaryMetricsView() {
    }

}
