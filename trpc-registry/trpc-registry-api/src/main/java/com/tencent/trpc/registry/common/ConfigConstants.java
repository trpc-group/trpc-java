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

package com.tencent.trpc.registry.common;

/**
 * Constants for registry configuration items.
 */
public class ConfigConstants {

    /**
     * Key for registry addresses. Separated by commas. For example, the address for ZooKeeper:
     * 192.0.0.1:2181,192.0.0.2:2181
     */
    public static final String REGISTRY_CENTER_ADDRESSED_KEY = "addresses";

    /**
     * Key for registry username.
     */
    public static final String REGISTRY_CENTER_USERNAME_KEY = "username";

    /**
     * Key for registry password.
     */
    public static final String REGISTRY_CENTER_PASSWORD_KEY = "password";

    /**
     * Key for whether to register a consumer in the registry.
     */
    public static final String REGISTRY_CENTER_REGISTER_CONSUMER_KEY = "register_consumer";

    /**
     * Key for registry connection timeout.
     */
    public static final String REGISTRY_CENTER_CONN_TIMEOUT_MS_KEY = "conn_timeout";

    /**
     * Default registry connection timeout.
     */
    public static final int DEFAULT_REGISTRY_CENTER_CONN_TIMEOUT_MS = 5 * 1000;

    /**
     * Key for whether to enable persistent caching.
     */
    public static final String REGISTRY_CENTER_SAVE_CACHE_KEY = "persisted_save_cache";

    /**
     * Key for whether to enable synchronous caching. By default, caching is asynchronous.
     */
    public static final String REGISTRY_CENTER_SYNCED_SAVE_CACHE_KEY = "synced_save_cache";

    /**
     * Key for the file path for persistent caching.
     */
    public static final String REGISTRY_CENTER_CACHE_FILE_PATH_KEY = "cache_file_path";

    /**
     * Key for the expiration time of the cache.
     */
    public static final String REGISTRY_CENTER_CACHE_ALIVE_TIME_SECS_KEY = "cache_expire_time";

    /**
     * Default expiration time for the cache: 3600 seconds.
     */
    public static final int DEFAULT_REGISTRY_CENTER_CACHE_ALIVE_TIME_SECS = 60 * 60;

    /**
     * Key for whether to enable retry logic for failed registry operations.
     */
    public static final String REGISTRY_CENTER_OPEN_FAILED_RETRY_KEY = "open_failed_retry";

    /**
     * Key for the retry interval for failed registry operations.
     */
    public static final String REGISTRY_CENTER_RETRY_PERIOD_KEY = "retry_period";

    /**
     * Default retry interval for failed registry operations: 5 seconds.
     */
    public static final int DEFAULT_REGISTRY_CENTER_RETRY_PERIOD_MS = 5 * 1000;

    /**
     * Key for the number of times to retry failed registry operations.
     */
    public static final String REGISTRY_CENTER_RETRY_TIMES_KEY = "retry_times";

    /**
     * Default number of times to retry failed registry operations.
     */
    public static final int DEFAULT_REGISTRY_CENTER_RETRY_TIMES = 3;

    private ConfigConstants() {
        throw new IllegalStateException("not support invoke");
    }
}
