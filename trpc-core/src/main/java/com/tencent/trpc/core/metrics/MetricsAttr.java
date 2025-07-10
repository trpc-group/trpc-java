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
 * Attribute Statistics
 */
@Deprecated
public interface MetricsAttr {

    /**
     * Self-incrementing properties
     *
     * @param attrName attribute name
     * @param value Self-incrementing range
     */
    default void incr(String attrName, double value) {
        incr(attrName, (int) value);
    }

    /**
     * Self-incrementing properties
     *
     * @param attrName attribute name
     * @param value Self-incrementing range
     */
    void incr(String attrName, int value);

    /**
     * Property settings
     *
     * @param attrName Property name
     * @param value The specific value to be set
     */
    default void set(String attrName, double value) {
        // compatible with old interfaces and data accuracy
        set(attrName, (int) value);
    }

    /**
     * Property settings
     *
     * @param attrName Property name
     * @param value The specific value to be set
     */
    void set(String attrName, int value);

}
