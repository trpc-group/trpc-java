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

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.metrics.spi.MetricsFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Metrics Classes that specifically handle monitoring metrics
 */
public class Metrics {

    private static final Map<MetricName, Counter> COUNTERS = new ConcurrentHashMap<>();
    private static final Map<MetricName, Gauge> GAUGES = new ConcurrentHashMap<>();
    private static final Map<MetricName, Histogram> HISTOGRAMS = new ConcurrentHashMap<>();
    private static final Map<String, MetricsFactory> METRICS_FACTORS = new ConcurrentHashMap<>();

    public static void init() {
        Map<String, PluginConfig> pluginConfigMap =
                ConfigManager.getInstance().getPluginConfigMap().get(MetricsFactory.class);
        if (pluginConfigMap == null) {
            return;
        }
        for (Map.Entry<String, PluginConfig> entry : pluginConfigMap.entrySet()) {
            METRICS_FACTORS.computeIfAbsent(entry.getKey(),
                    n -> ExtensionLoader.getExtensionLoader(MetricsFactory.class).getExtension(entry.getKey()));
        }
    }


    /**
     * The counter type represents a metric whose sample data is monotonically increasing, i.e., it only increases
     * and does not decrease, unless the monitoring system is reset. For example, you can use the counter type metric
     * to represent the number of requests for services, the number of tasks completed, the number of errors
     * that occurred, etc.
     * How to use counter?
     * The way it is used most of the time:
     * --> Metrics.counter("example_counter").incr();
     * You can also specify a self-incrementing value to achieve multiple accumulation:
     * --> Metrics.counter("example_counter").incr(10);
     * If you want to use labels, use as the following show:
     * --> Metrics.counter("example_counter", "a").incr(10, "1");
     *
     * @param name the name of the counter
     * @param labelNames the label names of the counter
     * @return the counter
     */
    public static Counter counter(String name, String... labelNames) {
        MetricName mName = MetricName.build(name, labelNames);
        return COUNTERS.computeIfAbsent(mName, n -> (value, labelValues) ->
                METRICS_FACTORS.values().forEach(x -> x.counter(name, labelNames).incr(value, labelValues)));
    }

    /**
     * The gauge type represents a metric whose sample data can be varied arbitrarily, i.e., increased or decreased.
     * gauge is typically used for metrics such as temperature or memory usage, but can also represent "totals" that
     * can be increased or decreased at any time, e.g., the number of current concurrent requests.
     * How to use gauge?
     * The way it is used most of the time:
     * --> Metrics.gauge("example_gauge").set(100);
     * If you want to use labels, use as the following show:
     * --> Metrics.gauge("example_gauge", "a").set(98, "1");
     *
     * @param name the name of the gauge
     * @param labelNames the label names of the gauge
     * @return the gauge
     */
    public static Gauge gauge(String name, String... labelNames) {
        MetricName mName = MetricName.build(name, labelNames);
        return GAUGES.computeIfAbsent(mName, n -> (value, labelValues) ->
                METRICS_FACTORS.values().forEach(x -> x.gauge(name, labelNames).set(value, labelValues)));
    }

    /**
     * Histogram samples the data over a period of time (usually request duration or response size, etc.) and counts
     * it in a configurable storage bucket. Subsequently, samples can be filtered by specified intervals, or the
     * total number of samples can be counted, and finally the data is typically displayed as a histogram.
     *
     * @param name the name of the histogram
     * @param buckets the histogram distribution bucket boundaries, left-open right-closed, e.g., time ms
     *         interval [10,20,50,100]
     * @param labelNames the label names of the histogram
     * @return the histogram
     */
    public static Histogram histogram(String name, double[] buckets, String... labelNames) {
        MetricName mName = MetricName.build(name, labelNames);
        return HISTOGRAMS.computeIfAbsent(mName, n -> (value, labelValues) ->
                METRICS_FACTORS.values().forEach(x -> {
                    Histogram histogram =
                            labelNames == null ? x.histogram(name, buckets) : x.histogram(name, buckets, labelNames);
                    histogram.record(value, labelValues);
                }));
    }

    /**
     * Histogram samples the data over a period of time (usually request duration or response size, etc.) and counts
     * it in a configurable storage bucket. Subsequently, samples can be filtered by specified intervals, or the
     * total number of samples can be counted, and finally the data is typically displayed as a histogram.
     *
     * @param name the name of the histogram
     * @param buckets the histogram distribution bucket boundaries, left-open right-closed, e.g., time ms
     *         interval [10,20,50,100]
     * @return the histogram
     */
    public static Histogram histogram(String name, double... buckets) {
        MetricName mName = MetricName.build(name);
        return HISTOGRAMS.computeIfAbsent(mName, n -> (value, labelValues) ->
                METRICS_FACTORS.values().forEach(x -> x.histogram(name).record(value, labelValues)));
    }

}