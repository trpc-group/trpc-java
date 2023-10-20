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
 * Histogram distribution metric, suitable for statistics such as interface call time consumption distribution
 * and other types of data. Subsequently, samples can be filtered by specified intervals, and the total number
 * of samples can also be counted, and finally the data is generally displayed as a histogram.
 *
 * Note: The histogram distribution depends on the implementation of the underlying monitoring system and does
 * not necessarily follow the configured segment boundaries exactly
 */
public interface Histogram extends Metric {

    /**
     * Record a monitoring data
     *
     * @param value specific values
     */
    default void record(double value) {
        record(value, (String[]) null);
    }

    /**
     * Record a monitoring data and set the tag at the same time
     *
     * @param value specific values
     * @param labelValues Tag data, if the tag name is specified when getting {@code Histogram},
     *                    here it should be consistent in order and number
     */
    void record(double value, String... labelValues);

}
