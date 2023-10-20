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

package com.tencent.trpc.core.metrics.spi;

import com.google.common.collect.Lists;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.metrics.Counter;
import com.tencent.trpc.core.metrics.Gauge;
import com.tencent.trpc.core.metrics.Histogram;
import com.tencent.trpc.core.metrics.MetricsCustom.StatPolicy;
import com.tencent.trpc.core.metrics.MetricsCustom.StatValue;

/**
 * Abstract metrics object creation factory, used to create the actual processing of the generated metrics object;
 * where the underlying layer can be freely adapted according to needs, the current mainstream is
 * prometheus or open-telemetry oltp protocol, but also access to some private platform packaging, etc.
 */
public abstract class AbstractMetricsFactory implements MetricsFactory {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMetricsFactory.class);

    /**
     * Counter is a cumulative type of data indicator that represents a monotonically increasing counter.
     * this method get counters for numerical statistics, such as number of requests, errors, executions, etc.
     *
     * @param name counter name
     * @param labelNames counter labels
     * @return instantiated counter
     */
    @Override
    public Counter counter(String name, String... labelNames) {
        return (value, labelValues) -> {
            if (labelNames == null || labelNames.length == 0) {
                getAttr().incr(name, value);
            } else {
                if (labelValues == null || labelValues.length == 0 || labelNames.length != labelValues.length) {
                    logger.error("labelNames length must be the same as labelValues length");
                    return;
                }
                getCustomData(name, Lists.newArrayList(labelValues))
                        .report(Lists.newArrayList(StatValue.of(value, 1, StatPolicy.SUM)));
            }
        };
    }

    /**
     * Gauge is a type of indicator that can fluctuate up or down in value at will.
     * That is, the value of Gauge can be increased or decreased, or increased or decreased.
     * The current method will return an instantiated object that can be used to record instantaneous values
     *
     * @param name gauge name
     * @param labelNames gauge labels
     * @return instantiated gauge object
     */
    @Override
    public Gauge gauge(String name, String... labelNames) {
        return (value, labelValues) -> measure(name, labelNames, value, labelValues);
    }

    /**
     * The current method will return a histogram statistician object
     *
     * @param name statistician name
     * @param buckets The histogram distribution is divided into bucket boundaries, left open and right closed,
     *                e.g.: time-consuming ms interval [10,20,50,100]
     * @return histogram statistician object
     */
    @Override
    public Histogram histogram(String name, double... buckets) {
        return (value, labelValues) -> {
            if (labelValues == null) {
                getAttr().set(name, value);
            } else {
                logger.error("labelValues should be empty");
            }
        };
    }

    /**
     * The current method will return a histogram statistician object and also support to set the tag names
     *
     * @param name statistician name
     * @param buckets The histogram distribution is divided into bucket boundaries, left open and right closed,
     *                such as: time-consuming ms interval [10,20,50,100]
     * @param labelNames Histogram statistician tag names
     * @return histogram statistician object
     */
    @Override
    public Histogram histogram(String name, double[] buckets, String... labelNames) {
        return (value, labelValues) -> measure(name, labelNames, value, labelValues);
    }

    /**
     * Statistician internal execution logic
     *
     * @param name The name of the statistician
     * @param labelNames The name of the statistician label
     * @param value specific value
     * @param labelValues label data
     */
    private void measure(String name, String[] labelNames, double value, String[] labelValues) {
        if (labelNames == null || labelNames.length == 0) {
            getAttr().set(name, value);
        } else {
            if (labelValues == null || labelValues.length == 0 || labelNames.length != labelValues.length) {
                logger.error("labelNames length must be the same as labelValues length");
                return;
            }
            getCustomData(name, Lists.newArrayList(labelValues))
                    .report(Lists.newArrayList(StatValue.of(value, 1, StatPolicy.SET)));
        }
    }
}
