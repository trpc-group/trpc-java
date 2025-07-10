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

import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.metrics.spi.MetricsFactory;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.Test;

public class MetricsTest {

    public static final AtomicInteger COUNTER = new AtomicInteger();
    public static final AtomicInteger GAUGE = new AtomicInteger();
    public static final AtomicReference<List<MetricsCustom.StatValue>> STAT_VALUES = new AtomicReference<>();

    public static class TestMetricsFactory implements MetricsFactory {
        @Override
        public MetricsAttr getAttr() {
            return new MetricsAttr() {
                @Override
                public void incr(String attrName, int value) {
                    COUNTER.addAndGet(value);
                }

                @Override
                public void set(String attrName, int value) {
                    GAUGE.set(value);
                }
            };
        }

        @Override
        public MetricsCustom getCustomData(String name, List<String> dimensions) {
            return STAT_VALUES::set;
        }
    }

    @Test
    public void testLoad() {
        Map<String, PluginConfig> pluginConfigMap = ConfigManager.getInstance().getPluginConfigMap()
                .computeIfAbsent(MetricsFactory.class, clz -> Maps.newConcurrentMap());
        PluginConfig plugin = new PluginConfig("testCoreMetrics", TestMetricsFactory.class);
        pluginConfigMap.put("testCoreMetrics", plugin);
        ConfigManager.getInstance().getPluginConfigMap().put(MetricsFactory.class, pluginConfigMap);
        Metrics.init();

        COUNTER.set(0);
        Metrics.counter("a").incr();
        Assert.assertEquals(1, COUNTER.get());

        Metrics.gauge("b").set(100);
        Assert.assertEquals(100, GAUGE.get());

        COUNTER.set(0);
        Metrics.histogram("c").record(100);
        Assert.assertEquals(1, COUNTER.get());

        COUNTER.set(0);
        Metrics.histogram("c", new double[]{0}, "1").record(100, "1");
        Assert.assertEquals(1, COUNTER.get());
    }

    @Test
    public void testMetrics() {
        final AtomicInteger count = new AtomicInteger();
        final AtomicInteger gauge = new AtomicInteger();

        MetricsFactory metricsFactory = new MetricsFactory() {
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
        };

        int count1 = 100;
        // test counter and gauge
        for (int i = 0; i < count1; i++) {
            int value = i + 1;
            metricsFactory.counter("counter").incr(value);
            metricsFactory.gauge("gauge").set(value);
        }
        Assert.assertEquals(5050, count.get());
        Assert.assertEquals(count1, gauge.get());

        // test histogram
        count.set(0);
        for (int i = 0; i < count1; i++) {
            int value = i + 1;
            metricsFactory.histogram("counter").record(value);
        }
        Assert.assertEquals(count1, count.get());

        // test normal counter
        count.set(0);
        for (int i = 0; i < count1; i++) {
            metricsFactory.counter("counter").incr();
        }
        Assert.assertEquals(count1, count.get());

        try {
            metricsFactory.getActiveStat(null);
            Assert.fail("not supported by default");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof UnsupportedOperationException);
        }

        try {
            metricsFactory.getCustomData(null, null);
            Assert.fail("not supported by default");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof UnsupportedOperationException);
        }

        try {
            metricsFactory.getPassiveStat(null);
            Assert.fail("not supported by default");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof UnsupportedOperationException);
        }
    }
}
