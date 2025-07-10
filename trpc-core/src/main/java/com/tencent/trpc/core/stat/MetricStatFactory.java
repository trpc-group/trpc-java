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

package com.tencent.trpc.core.stat;

import com.google.common.collect.Sets;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.stat.spi.Stat;
import com.tencent.trpc.core.utils.CollectionUtils;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MetricStatFactory {

    private static final Logger logger = LoggerFactory.getLogger(MetricStatFactory.class);
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final Set<Stat> METRIC_STAT_SET = Sets.newConcurrentHashSet();
    private static final int DELAY_MS = 60 * 1000;
    private static final int PERIOD_MS = 30 * 1000;
    private static volatile ScheduledFuture<?> schedule;

    public static void startStat(List<String> metricStats) {
        if (!CollectionUtils.isEmpty(metricStats)) {
            ExtensionLoader<Stat> extensionLoader = ExtensionLoader.getExtensionLoader(Stat.class);
            METRIC_STAT_SET.addAll(
                    metricStats.stream().map(extensionLoader::getExtension).collect(Collectors.toList()));
        }
        if (schedule == null) {
            synchronized (MetricStatFactory.class) {
                if (schedule == null) {
                    schedule = SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                            MetricStatFactory::stat, DELAY_MS, PERIOD_MS, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    public static void stat() {
        METRIC_STAT_SET.forEach(s -> {
            try {
                s.stat();
            } catch (Exception ex) {
                logger.error("startStat error {}", ex.getMessage(), ex);
            }
        });
    }

    public static void closeStat() {
        try {
            METRIC_STAT_SET.clear();
            if (schedule != null) {
                synchronized (MetricStatFactory.class) {
                    if (schedule != null) {
                        schedule.cancel(true);
                        schedule = null;
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("closeStat error {}", ex.getMessage(), ex);
        }
    }

}