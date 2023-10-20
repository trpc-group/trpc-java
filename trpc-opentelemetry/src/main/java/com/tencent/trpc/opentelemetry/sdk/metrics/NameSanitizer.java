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

package com.tencent.trpc.opentelemetry.sdk.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Sanitizes a metric or label name.
 */
public class NameSanitizer implements Function<String, String> {

    static final NameSanitizer INSTANCE = new NameSanitizer();

    private static final Pattern SANITIZE_PREFIX_PATTERN = Pattern.compile("^[^a-zA-Z_:]");
    private static final Pattern SANITIZE_BODY_PATTERN = Pattern.compile("[^a-zA-Z0-9_:]");

    private final Function<String, String> delegate;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    NameSanitizer() {
        this(NameSanitizer::sanitizeMetricName);
    }

    NameSanitizer(Function<String, String> delegate) {
        this.delegate = delegate;
    }

    private static String sanitizeMetricName(String metricName) {
        return SANITIZE_BODY_PATTERN
                .matcher(SANITIZE_PREFIX_PATTERN.matcher(metricName).replaceFirst("_"))
                .replaceAll("_");
    }

    @Override
    public String apply(String labelName) {
        return cache.computeIfAbsent(labelName, delegate);
    }

}
