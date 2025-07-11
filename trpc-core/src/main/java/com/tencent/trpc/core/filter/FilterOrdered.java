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
 * Filter Definition of plugin execution order constants.
 */
public interface FilterOrdered {

    /**
     * consumer head filter ordered
     */
    int CONSUMER_HEAD_ORDERED = Ordered.HIGHEST_PRECEDENCE;

    /**
     * consumer tail filter ordered
     */
    int CONSUMER_TAIL_ORDERED = Ordered.LOWEST_PRECEDENCE;

    /**
     * provider head filter ordered
     */
    int PROVIDER_HEAD_ORDERED = Ordered.HIGHEST_PRECEDENCE;

    /**
     * provider tail filter ordered
     */
    int PROVIDER_TAIL_ORDERED = Ordered.LOWEST_PRECEDENCE;

    /**
     * tps metrics ordered
     */
    int TPS_METRICS_ORDERED = Ordered.HIGHEST_PRECEDENCE + 10000;

    /**
     * zhiyan metrics ordered
     */
    int ZHIYAN_METRICS_ORDERED = Ordered.HIGHEST_PRECEDENCE + 10000;

    /**
     * atta metrics ordered
     */
    int ATTA_METRICS_ORDERED = Ordered.HIGHEST_PRECEDENCE + 10000;

    /**
     * tjg trace ordered
     */
    int TJG_TRACE_ORDERED = Ordered.HIGHEST_PRECEDENCE + 20000;

    /**
     * tps trace ordered
     */
    int TPS_TRACE_ORDERED = Ordered.HIGHEST_PRECEDENCE + 20000;

    /**
     * log replay trace ordered
     */
    int LOG_REPLAY_TRACE_ORDERED = Ordered.HIGHEST_PRECEDENCE + 20000;

    /**
     * atta logger ordered
     */
    int ATTA_LOGGER_ORDERED = Ordered.HIGHEST_PRECEDENCE + 30000;

    /**
     * zhiyan logger ordered
     */
    int ZHIYAN_LOGGER_ORDERED = Ordered.HIGHEST_PRECEDENCE + 30000;

    /**
     * tps logger ordered
     */
    int TPS_LOGGER_ORDERED = Ordered.HIGHEST_PRECEDENCE + 30000;

    /**
     * pgv validation ordered
     */
    int PGV_VALIDATION_ORDERED = Ordered.HIGHEST_PRECEDENCE + 40000;

    /**
     * provider knock auth ordered
     */
    int PROVIDER_KNOCK_ORDERED = Ordered.HIGHEST_PRECEDENCE + 50000;

    /**
     * consumer knock auth ordered
     */
    int CONSUMER_KNOCK_ORDERED = Ordered.HIGHEST_PRECEDENCE + 50000;

    /**
     * sentinel limiter ordered
     */
    int SENTINEL_LIMITER_ORDERED = Ordered.HIGHEST_PRECEDENCE + 60000;

    /**
     * polaris limiter ordered
     */
    int POLARIS_LIMIT_ORDERED = Ordered.HIGHEST_PRECEDENCE + 60000;

}
