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

import com.tencent.trpc.core.metrics.MetricsAttr;
import com.tencent.trpc.core.metrics.MetricsCustom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AbstractMetricsFactoryTest {

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
        Assertions.assertEquals(1, count.get());

        String label = "1";
        factory.counter("a", "label").incr(2);
        Assertions.assertNull(report.get());
        factory.counter("a", "label").incr(2, label);
        Assertions.assertEquals(label, report.get().get(0));
        Assertions.assertEquals(2, statValues.get().get(0).value, 0.0);
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
        Assertions.assertEquals(100, gauge.get());

        String label = "1";
        factory.gauge("a", "label").set(200);
        Assertions.assertNull(report.get());

        factory.gauge("a", "label").set(200, label);
        Assertions.assertEquals(label, report.get().get(0));
        Assertions.assertEquals(200, statValues.get().get(0).value, 0.0);
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
        Assertions.assertEquals(1, gauge.get());

        String label = "1";
        factory.histogram("a").record(1, "");
        Assertions.assertNull(report.get());

        factory.histogram("a", new double[]{0}, "label").record(1, label);
        Assertions.assertEquals(label, report.get().get(0));
        Assertions.assertEquals(1, statValues.get().get(0).value, 0.0);
    }
}
