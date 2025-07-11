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

package com.tencent.trpc.core.filter;

/**
 * Filter Plugin sorting order interface, the smaller the value, the higher the priority.
 */
public interface Ordered {

    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

    int DEFAULT_PRECEDENCE = 0;

    /**
     * Filter Execution order, the smaller the value, the higher the priority. The default value is 0.
     *
     * @return int Execution order of filter plugins.
     */
    default int getOrder() {
        return DEFAULT_PRECEDENCE;
    }

}