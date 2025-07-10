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

package com.tencent.trpc.core.metrics;

import java.util.Arrays;
import java.util.Objects;

/**
 * Metric name define to be used in lambda expressions
 */
public class MetricName {

    private static final String[] EMPTY_LABELS = new String[0];

    private final String name;
    private final String[] labels;

    private int hashCode = 0;
    private boolean hashCodeCached = false;

    private MetricName(String name, String[] labels) {
        this.name = Objects.requireNonNull(name, "param name must not be null");
        this.labels = labels == null ? EMPTY_LABELS : labels;   // Replace labels with empty labels when they are null
    }

    /**
     * Build a metric name to be used in lambda expressions
     *
     * @param name Name
     * @param labels list of label names
     * @return indicator signature
     */
    public static MetricName build(String name, String... labels) {
        return new MetricName(name, labels);
    }

    public String getName() {
        return name;
    }

    public String[] getLabels() {
        return labels;
    }

    @Override
    public String toString() {
        return "Metrics{name='" + name + "', labels=" + Arrays.toString(labels) + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetricName that = (MetricName) o;
        return name.equals(that.name) && Arrays.equals(labels, that.labels);
    }

    @Override
    public int hashCode() {
        if (!hashCodeCached) {
            int result = Objects.hash(name);
            result = 31 * result + Arrays.hashCode(labels);

            hashCode = result;
            hashCodeCached = true;
        }
        return hashCode;
    }

}