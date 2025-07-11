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

import com.tencent.trpc.core.metrics.MetricsAttr;
import com.tencent.trpc.core.metrics.MetricsCustom;
import junit.framework.TestCase;
import org.junit.Assert;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AbstractMetricsFactoryTest extends TestCase {

    public void testCounter() {
        AtomicInteger count = new AtomicInteger();
        AtomicInteger gauge = new AtomicInteger();
        AtomicReference<List<String>> report = new AtomicReference<>();
        AtomicReference<List<MetricsCustom.StatValue>> statValues = new AtomicReference<>();
        MetricsFactory factory = new AbstractMetricsFactory() {
            @Override
            public MetricsAttr getAttr() {
                return new MetricsAttr() {
                    @Override
                    public void incr(String attrName, int value) {
                        count.addAndGet(value);
                    }

                    @Override
                    public void set(String attrName, int value) {
                        gauge.set(value);
                    }
                };
            }

            @Override
            public MetricsCustom getCustomData(String name, List<String> dimensions) {
                return values -> {
                    report.set(dimensions);
                    statValues.set(values);
                };
            }
        };

        factory.counter("a").incr();
        Assert.assertEquals(1, count.get());

        String label = "1";
        factory.counter("a", "label").incr(2);
        Assert.assertNull(report.get());
        factory.counter("a", "label").incr(2, label);
        Assert.assertEquals(label,  report.get().get(0));
        Assert.assertEquals(2, statValues.get().get(0).value, 0.0);
    }

    public void testGauge() {
        AtomicInteger count = new AtomicInteger();
        AtomicInteger gauge = new AtomicInteger();
        AtomicReference<List<String>> report = new AtomicReference<>();
        AtomicReference<List<MetricsCustom.StatValue>> statValues = new AtomicReference<>();
        MetricsFactory factory = new AbstractMetricsFactory() {
            @Override
            public MetricsAttr getAttr() {
                return new MetricsAttr() {
                    @Override
                    public void incr(String attrName, int value) {
                        count.addAndGet(value);
                    }

                    @Override
                    public void set(String attrName, int value) {
                        gauge.set(value);
                    }
                };
            }

            @Override
            public MetricsCustom getCustomData(String name, List<String> dimensions) {
                return values -> {
                    report.set(dimensions);
                    statValues.set(values);
                };
            }
        };

        factory.gauge("a").set(100);
        Assert.assertEquals(100, gauge.get());

        String label = "1";
        factory.gauge("a", "label").set(200);
        Assert.assertNull(report.get());

        factory.gauge("a", "label").set(200, label);
        Assert.assertEquals(label,  report.get().get(0));
        Assert.assertEquals(200, statValues.get().get(0).value, 0.0);
    }

    public void testHistogram() {
        AtomicInteger count = new AtomicInteger();
        AtomicInteger gauge = new AtomicInteger();
        AtomicReference<List<String>> report = new AtomicReference<>();
        AtomicReference<List<MetricsCustom.StatValue>> statValues = new AtomicReference<>();
        MetricsFactory factory = new AbstractMetricsFactory() {
            @Override
            public MetricsAttr getAttr() {
                return new MetricsAttr() {
                    @Override
                    public void incr(String attrName, int value) {
                        count.addAndGet(value);
                    }

                    @Override
                    public void set(String attrName, int value) {
                        gauge.set(value);
                    }
                };
            }

            @Override
            public MetricsCustom getCustomData(String name, List<String> dimensions) {
                return values -> {
                    report.set(dimensions);
                    statValues.set(values);
                };
            }
        };

        factory.histogram("a").record(1);
        Assert.assertEquals(1, gauge.get());

        String label = "1";
        factory.histogram("a").record(1, "");
        Assert.assertNull(report.get());

        factory.histogram("a", new double[]{0}, "label").record(1, label);
        Assert.assertEquals(label,  report.get().get(0));
        Assert.assertEquals(1, statValues.get().get(0).value, 0.0);
    }
}