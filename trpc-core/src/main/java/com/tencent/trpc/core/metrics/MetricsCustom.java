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

import java.util.List;
import java.util.Objects;

@Deprecated
public interface MetricsCustom {

    void report(List<StatValue> values);

    enum StatPolicy {
        SUM, AVG, MAX, MIN, SET
    }

    class StatValue {

        public final double value;
        public final int count;
        public final StatPolicy policy;

        private StatValue(double value, int count, StatPolicy policy) {
            this.value = value;
            this.count = count;
            this.policy = Objects.requireNonNull(policy, "param policy must not be null");
        }

        public static StatValue of(double value) {
            return of(value, 1, StatPolicy.SUM);
        }

        public static StatValue of(double value, int count, StatPolicy policy) {
            return new StatValue(value, count, policy);
        }
    }

}