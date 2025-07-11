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

package com.tencent.trpc.registry.transporter.common;

/**
 * Constants class, which holds global constant information
 */
public class Constants {

    /**
     * Number of retries
     */
    public static final int RETRY_TIMES = 3;

    /**
     * Retry interval
     */
    public static final int SLEEP_MS_BETWEEN_RETRIES = 1000;

    /**
     * Client connection timeout
     */
    public static final int CLIENT_CONN_TIMEOUT_MS = 5000;

}
