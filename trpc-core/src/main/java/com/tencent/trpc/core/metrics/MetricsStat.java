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

/**
 * Call statistics
 */
@Deprecated
public interface MetricsStat {

    /**
     * Call start time, default system current time
     */
    default void start() {
        start(System.currentTimeMillis());
    }

    /**
     * The start time of the call, determined by the specific time passed in
     *
     * @param startTime The start time of the call statistics
     */
    void start(long startTime);

    /**
     * End of call
     */
    void stop();

    /**
     * The call was successful.
     */
    void success();

    /**
     * Call failed (business failure)
     *
     * @param ret business failure error code
     */
    void fail(int ret);

    /**
     * Call timeout
     */
    void timeout();

    /**
     * Call exception
     *
     * @param t Exception type
     */
    void error(Throwable t);

}
