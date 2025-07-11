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

package com.tencent.trpc.core.management;

import java.lang.management.PlatformManagedObject;
import java.util.Arrays;
import java.util.Objects;

public interface PoolMXBean extends PlatformManagedObject {

    String BAR = "-";

    String WORKER_POOL_MXBEAN_DOMAIN_TYPE = "com.tencent.trpc.core:type=WorkerPool";

    String getType();

    int getPoolSize();

    int getActiveThreadCount();

    enum WorkerPoolType {

        THREAD("thread"),

        FORK_JOIN("forkJoin");

        private final String name;

        WorkerPoolType(String name) {
            this.name = name;
        }

        public static WorkerPoolType ofName(String name) {
            return Arrays.stream(WorkerPoolType.values()).filter(wp -> Objects.equals(wp.getName(), name))
                    .findFirst().orElse(null);
        }

        public String getName() {
            return name;
        }
    }

}