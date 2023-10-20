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

package com.tencent.trpc.registry.common;

import static com.tencent.trpc.registry.common.ConfigConstants.DEFAULT_REGISTRY_CENTER_CACHE_ALIVE_TIME_SECS;
import static com.tencent.trpc.registry.common.ConfigConstants.DEFAULT_REGISTRY_CENTER_CONN_TIMEOUT_MS;
import static com.tencent.trpc.registry.common.ConfigConstants.DEFAULT_REGISTRY_CENTER_RETRY_PERIOD_MS;
import static com.tencent.trpc.registry.common.ConfigConstants.DEFAULT_REGISTRY_CENTER_RETRY_TIMES;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_ADDRESSED_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_CACHE_ALIVE_TIME_SECS_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_CACHE_FILE_PATH_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_CONN_TIMEOUT_MS_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_OPEN_FAILED_RETRY_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_PASSWORD_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_REGISTER_CONSUMER_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_RETRY_PERIOD_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_RETRY_TIMES_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_SAVE_CACHE_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_SYNCED_SAVE_CACHE_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_USERNAME_KEY;

import com.tencent.trpc.core.common.config.PluginConfig;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;


/**
 * Registry configuration item.
 */
public class RegistryCenterConfig {

    /**
     * Registry name.
     */
    protected String name;

    /**
     * Registry address. Multiple addresses are separated by commas.
     */
    protected String addresses;

    /**
     * Username for the registry.
     */
    protected String username;

    /**
     * Password for the registry.
     */
    protected String password;

    /**
     * Connection timeout for the registry.
     */
    protected int connTimeoutMs;

    /**
     * Enable retry logic for registry operations. Enabled by default.
     */
    protected boolean openFailedRetry;

    /**
     * Time interval for retrying failed registry operations.
     */
    protected int retryPeriod;

    /**
     * Number of times to retry failed registry operations.
     */
    protected int retryTimes;

    /**
     * Enable persistent local caching. Disabled by default.
     */
    protected boolean persistedSaveCache;

    /**
     * Synchronization mode for persistent local caching. Asynchronous by default.
     */
    protected boolean syncedSaveCache;

    /**
     * Expiration time for persistent caching.
     */
    protected int cacheAliveTimeSecs;

    /**
     * The path to the cache file.
     */
    protected String cacheFilePath;
    /**
     * Whether to register a consumer.
     */
    private boolean registerConsumer;

    public RegistryCenterConfig() {
    }

    /**
     * Build the registry configuration item, converted from pluginConfig passed by the framework.
     *
     * @param pluginConfig The registry configuration passed by the framework.
     */
    public RegistryCenterConfig(PluginConfig pluginConfig) {
        Map<String, Object> properties = pluginConfig.getProperties();
        this.name = pluginConfig.getName();
        this.addresses = MapUtils.getString(properties, REGISTRY_CENTER_ADDRESSED_KEY);
        this.username = MapUtils.getString(properties, REGISTRY_CENTER_USERNAME_KEY, null);
        this.password = MapUtils.getString(properties, REGISTRY_CENTER_PASSWORD_KEY, null);
        this.registerConsumer = MapUtils.getBoolean(properties, REGISTRY_CENTER_REGISTER_CONSUMER_KEY, false);
        this.connTimeoutMs = MapUtils
                .getInteger(properties, REGISTRY_CENTER_CONN_TIMEOUT_MS_KEY, DEFAULT_REGISTRY_CENTER_CONN_TIMEOUT_MS);
        this.openFailedRetry = MapUtils.getBoolean(properties, REGISTRY_CENTER_OPEN_FAILED_RETRY_KEY, true);
        this.retryPeriod = MapUtils
                .getInteger(properties, REGISTRY_CENTER_RETRY_PERIOD_KEY, DEFAULT_REGISTRY_CENTER_RETRY_PERIOD_MS);
        this.retryTimes = MapUtils
                .getInteger(properties, REGISTRY_CENTER_RETRY_TIMES_KEY, DEFAULT_REGISTRY_CENTER_RETRY_TIMES);
        this.persistedSaveCache = MapUtils.getBoolean(properties, REGISTRY_CENTER_SAVE_CACHE_KEY, false);
        this.syncedSaveCache = MapUtils
                .getBoolean(properties, REGISTRY_CENTER_SYNCED_SAVE_CACHE_KEY, false);
        this.cacheAliveTimeSecs = MapUtils.getInteger(properties, REGISTRY_CENTER_CACHE_ALIVE_TIME_SECS_KEY,
                DEFAULT_REGISTRY_CENTER_CACHE_ALIVE_TIME_SECS);

        String defaultSyncFilePath = String.format("%s/.trpc/trpc-registry/%s.cache",
                System.getProperty("user.home"), this.name);
        this.cacheFilePath = MapUtils.getString(properties, REGISTRY_CENTER_CACHE_FILE_PATH_KEY, defaultSyncFilePath);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddresses() {
        return addresses;
    }

    public void setAddresses(String addresses) {
        this.addresses = addresses;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isRegisterConsumer() {
        return registerConsumer;
    }

    public void setRegisterConsumer(boolean registerConsumer) {
        this.registerConsumer = registerConsumer;
    }

    public int getConnTimeoutMs() {
        return connTimeoutMs;
    }

    public void setConnTimeoutMs(int connTimeoutMs) {
        this.connTimeoutMs = connTimeoutMs;
    }

    public boolean isOpenFailedRetry() {
        return openFailedRetry;
    }

    public void setOpenFailedRetry(boolean openFailedRetry) {
        this.openFailedRetry = openFailedRetry;
    }

    public int getRetryPeriod() {
        return retryPeriod;
    }

    public void setRetryPeriod(int retryPeriod) {
        this.retryPeriod = retryPeriod;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public boolean isPersistedSaveCache() {
        return persistedSaveCache;
    }

    public void setPersistedSaveCache(boolean persistedSaveCache) {
        this.persistedSaveCache = persistedSaveCache;
    }

    public boolean isSyncedSaveCache() {
        return syncedSaveCache;
    }

    public void setSyncedSaveCache(boolean syncedSaveCache) {
        this.syncedSaveCache = syncedSaveCache;
    }

    public int getCacheAliveTimeSecs() {
        return cacheAliveTimeSecs;
    }

    public void setCacheAliveTimeSecs(int cacheAliveTimeSecs) {
        this.cacheAliveTimeSecs = cacheAliveTimeSecs;
    }

    public String getCacheFilePath() {
        return cacheFilePath;
    }

    public void setCacheFilePath(String cacheFilePath) {
        this.cacheFilePath = cacheFilePath;
    }
}
