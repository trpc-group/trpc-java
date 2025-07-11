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

/**
 * The Counter type represents a metric whose sample data is monotonically increasing, i.e., it only increases
 * and does not decrease, unless the monitoring system is reset.
 * For example, you can use the counter type of metrics to represent the number of requests for services, the number
 * of tasks completed, the number of errors that occurred, etc.
 * This allows the user to easily understand the changes in the rate of event generation
 * Do not apply the counter type to metrics where the sample data is not monotonically increasing
 */
public interface Counter extends Metric {

    /**
     * The counter self-increasing by 1
     */
    default void incr() {
        incr(1);
    }

    /**
     * The counter is self-increasing by n, and n is the input parameter
     *
     * @param value numeric value, can be negative
     */
    default void incr(double value) {
        incr(value, (String[]) null);
    }

    /**
     * The counter increases by n, which is the input parameter, and also sets the label
     *
     * @param value numeric value, can be negative
     * @param labelValues Tag value, if you specify the tag name when getting {@code Counter}, here you have to
     *                    be consistent in order and number
     */
    void incr(double value, String... labelValues);

}
