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

package com.tencent.trpc.core.stat.metrics;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.AdminConfig;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.metrics.Metrics;
import com.tencent.trpc.core.metrics.MetricsAttr;
import com.tencent.trpc.core.metrics.MetricsCustom;
import com.tencent.trpc.core.metrics.spi.AbstractMetricsFactory;
import com.tencent.trpc.core.metrics.spi.MetricsFactory;
import com.tencent.trpc.core.stat.MetricStatFactory;
import com.tencent.trpc.core.stat.spi.Stat;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class MetricStatFactoryTest {

    @Test
    public void testStartStat() {
        Map<String, PluginConfig> pluginMap = ConfigManager.getInstance().getPluginConfigMap()
                .computeIfAbsent(MetricsFactory.class, clz -> Maps.newConcurrentMap());
        PluginConfig plugin = new PluginConfig("testMetrics", TestMetricsFactory.class);
        pluginMap.put("testMetrics", plugin);
        Metrics.init();
        ExtensionLoader.getExtensionLoader(Stat.class).getExtension("test").stat();
        AdminConfig adminConfig = new AdminConfig();
        adminConfig.setMetricStats(Lists.newArrayList("test"));
        MetricStatFactory.startStat(adminConfig.getMetricStats());
        MetricStatFactory.startStat(adminConfig.getMetricStats());
        MetricStatFactory.stat();
        MetricStatFactory.closeStat();
    }

    public static class TestMetricsFactory extends AbstractMetricsFactory {

        private static final Logger logger = LoggerFactory.getLogger(TestMetricsFactory.class);

        @Override
        public MetricsAttr getAttr() {
            return new MetricsAttr() {
                @Override
                public void incr(String attrName, int value) {
                    logger.info(String.format("MetricsAttr incr %s %s", attrName, value));
                }

                @Override
                public void set(String attrName, int value) {
                    logger.info(String.format("MetricsAttr set %s %s", attrName, value));
                }
            };
        }

        @Override
        public MetricsCustom getCustomData(String name, List<String> dimensions) {
            return values -> {
                logger.info(String.format("getCustomData set %s", name));
            };
        }
    }

    public static class TestMetricStat implements Stat {

        private static final Logger logger = LoggerFactory.getLogger(TestMetricsFactory.class);

        @Override
        public void stat() {
            logger.info("testMetricStat");
            Metrics.gauge("testgauge1").set(1);
            Metrics.gauge("testgauge1").set(1);
            Metrics.gauge("testgauge2", "testlabelname2").set(1, "testlabelval2");
            Metrics.gauge("testgauge2", "testlabelname2").set(1, "testlabelval2");
            Metrics.gauge("testgauge3", "testlabelname3").set(1);
            Metrics.counter("testcounter1").incr(1);
            Metrics.counter("testcounter2", "testlabelname2").incr(1, "testlabelval2");
            Metrics.counter("testcounter3", "testlabelname3").incr(1);
            Metrics.histogram("testhistogram1").record(1);
            Metrics.histogram("testhistogram2",
                    new double[]{1.0}, "testlabelname2").record(1, "testlabelval2");
            Metrics.histogram("testhistogram3", new double[]{1.0}).record(1);
            Metrics.histogram("testhistogram4",
                    new double[]{1.0}, "testlabelname4").record(1, "testlabelval4");
        }
    }
}