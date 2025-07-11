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

package com.tencent.trpc.core.metrics.spi;

import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.metrics.ActiveInvocation;
import com.tencent.trpc.core.metrics.Counter;
import com.tencent.trpc.core.metrics.Gauge;
import com.tencent.trpc.core.metrics.Histogram;
import com.tencent.trpc.core.metrics.MetricsAttr;
import com.tencent.trpc.core.metrics.MetricsCustom;
import com.tencent.trpc.core.metrics.MetricsStat;
import com.tencent.trpc.core.metrics.PassiveInvocation;
import java.util.List;

/**
 * Statistical tool factory, realized by specific plug-ins; each component party can realize its own underlying
 * implementation according to the monitoring requirements; currently supports several monitoring types more
 * commonly used in the industry, mainly Counter, Gauge, Histogram
 */
@Extensible
public interface MetricsFactory {

    /**
     * This method returns a Counter object, which can be used to implement counting statistical capabilities
     * What is Counter?
     * Counter is a cumulative type of data indicator that represents a monotonically increasing counter.
     * this method get counters for numerical statistics, such as number of requests, errors, executions, etc.
     * How the application layer uses Counter?
     * @see com.tencent.trpc.core.metrics.Metrics#counter(String, String...)
     * Note: This method relies on the implementation of the underlying monitoring system and does not necessarily
     * follow exactly the configured segment boundaries to count
     *
     * @param name counter name
     * @param labelNames counter labels
     * @return counter object
     */
    default Counter counter(String name, String... labelNames) {
        return (value, labelValues) -> getAttr().incr(name, value);
    }

    /**
     * This method returns a Gauge object, which can be used to implement instantaneous value recording capabilities
     * What is Gauge?
     * Gauge is a type of indicator that can fluctuate up or down in value at will.
     * That is, the value of Gauge can be increased or decreased, or increased or decreased.
     * For example, the CPU usage of a machine can be large or small.
     * How the application layer Gauge?
     * @see com.tencent.trpc.core.metrics.Metrics#gauge(String, String...)
     * Note: This method relies on the implementation of the underlying monitoring system and does not necessarily
     * follow exactly the configured segment boundaries to count
     *
     * @param name gauge name
     * @param labelNames gauge labels
     * @return gauge object
     */
    default Gauge gauge(String name, String... labelNames) {
        return (value, labelValues) -> getAttr().set(name, value);
    }

    /**
     * This method returns a Histogram object, which can be used to implement histogram statistical capabilities
     * What is Histogram?
     * The Histogram samples data over a period of time (typically request duration or response size, etc.) and
     * counts it in a configurable bucket, which can then be filtered by specified intervals or counted in total,
     * and then generally displayed as a histogram.
     * How the application layer Histogram?
     * @see com.tencent.trpc.core.metrics.Metrics#histogram(String, double...)
     * @see com.tencent.trpc.core.metrics.Metrics#histogram(String, double[], String...)
     * Note: This method relies on the implementation of the underlying monitoring system and does not necessarily
     * follow exactly the configured segment boundaries to count
     *
     * @param name histogram name
     * @param buckets Straight-square distribution with bucket boundaries, left open and right closed,
     *                e.g., time consuming ms interval [10,20,50,100]
     * @return histogram object
     */
    default Histogram histogram(String name, double... buckets) {
        return histogram(name, buckets, (String[]) null);
    }

    /**
     * This method returns a Histogram object, which can be used to implement histogram statistical capabilities
     * What is Histogram?
     * The Histogram samples data over a period of time (typically request duration or response size, etc.) and
     * counts it in a configurable bucket, which can then be filtered by specified intervals or counted in total,
     * and then generally displayed as a histogram.
     * How the application layer Histogram?
     * @see com.tencent.trpc.core.metrics.Metrics#histogram(String, double...)
     * @see com.tencent.trpc.core.metrics.Metrics#histogram(String, double[], String...)
     * Note: This method relies on the implementation of the underlying monitoring system and does not necessarily
     * follow exactly the configured segment boundaries to count
     *
     * @param name histogram name
     * @param buckets buckets histogram split bucket boundaries, left open right closed,
     *                e.g., time consuming ms interval [10,20,50,100]
     * @param labelNames The label name of histogram
     * @return histogram object
     */
    default Histogram histogram(String name, double[] buckets, String... labelNames) {
        // default implementation is count only, bucketing logic is implemented by the specific monitoring system
        return (value, labelValues) -> getAttr().incr(name, 1);
    }

    /**
     * Get the active invocation statistic class
     *
     * @param activeInvocation main call information
     * @return active invocation statistics class
     * @deprecated active invocation monitoring should only be of interest in the client's {@code Filter}
     */
    @Deprecated
    default MetricsStat getActiveStat(ActiveInvocation activeInvocation) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * Get the passive invocation statistic class
     *
     * @param passiveInvocation called information
     * @return passive invocation statistics class
     * @deprecated passive invocation monitoring should only be of concern in the {@code Filter} on the server side
     */
    @Deprecated
    default MetricsStat getPassiveStat(PassiveInvocation passiveInvocation) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * Get property statistics class
     *
     * @return attribute statistics class
     * @deprecated Use more specific {@code Counter}, {@code Gauge}, {@code Histogram} to monitor statistic classes
     */
    @Deprecated
    default MetricsAttr getAttr() {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * Get custom statistics class
     *
     * @param name Custom attribute name
     * @param dimensions Custom dimension (meta) information
     * @return custom statistics class
     * @deprecated is more coupled with 007, should define more abstract interface
     */
    @Deprecated
    default MetricsCustom getCustomData(String name, List<String> dimensions) {
        throw new UnsupportedOperationException("not supported");
    }

}
