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

package com.tencent.trpc.core.metrics;

/**
 * The Gauge type represents a metric whose sample data can be changed at will, either up or down; Gauge is typically
 * used for metrics like temperature or memory usage, but can also represent a "total" that can be increased or
 * decreased at any time, e.g., the number of concurrent requests.
 */
public interface Gauge extends Metric {

    /**
     * Set current value
     *
     * @param value specific value
     */
    default void set(double value) {
        set(value, (String[]) null);
    }

    /**
     * Set the current value and also set the labels
     *
     * @param value specific value
     * @param labelValues Tag value, if the tag name is specified when getting {@code Gauge},
     *                    here it should be consistent in order and number
     */
    void set(double value, String... labelValues);

}
