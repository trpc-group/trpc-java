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

package com.tencent.trpc.core.worker.support.thread;

import static com.tencent.trpc.core.common.Constants.DEFAULT_BIZ_VIRTUAL_CORE_THREADS;
import static com.tencent.trpc.core.common.Constants.DEFAULT_BIZ_VIRTUAL_MAX_THREADS;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.utils.PreconditionUtils;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.MapUtils;

/**
 * Fork join pool configuration class.
 */
public class ThreadPoolConfig {

    /**
     * Core threads.
     */
    public static final String CORE_POOL_SIZE = "core_pool_size";
    /**
     * Maximum thread count.
     */
    public static final String MAXIMUM_POOL_SIZE = "maximum_pool_size";
    /**
     * Thread longest idle time.
     */
    public static final String KEEP_ALIVE_TIME_SECONDS = "keep_alive_time_seconds";
    /**
     * Queue size, 0 means no limit.
     */
    public static final String QUEUE_SIZE = "queue_size";
    /**
     * Whether to allow core thread timeout.
     */
    public static final String ALLOW_CORE_THREAD_TIMEOUT = "allow_core_thread_timeout";
    /**
     * Thread name prefix in thread pool.
     */
    public static final String NAME_PREFIX = "name_prefix";
    /**
     * Whether the thread is a background thread.
     */
    public static final String DAEMON = "deamon";
    /**
     * Default 10 seconds timeout.
     */
    public static final String CLOSE_TIMEOUT = "close_timeout";
    /**
     * Whether to use virtual threads for Java21.
     * See {@link Executors#newVirtualThreadPerTaskExecutor}
     * or {@link Executors#newThreadPerTaskExecutor}
     */
    public static final String USE_VIRTUAL_THREAD_PER_TASK_EXECUTOR = "use_virtual_thread_per_task_executor";
    /**
     * Whether to use coroutine.
     */
    public static final String USE_FIBER = "use_fiber";
    /**
     * Number of threads in the coroutine scheduling thread pool.
     */
    public static final String FIBER_PARALLEL = "fiber_parallel";
    /**
     * Whether the coroutine pool shares the scheduler.
     */
    public static final String SHARE_SCHEDULE = "share_schedule";
    /**
     * Default thread pool queue size.
     */
    private static final int DEFAULT_QUEUE_SIZE = 5000;
    /**
     * Default keep-alive time.
     */
    private static final long DEFAULT_KEEP_ALIVE_TIME_SECONDS = 60;
    /**
     * Default close timeout.
     */
    private static final int DEFAULT_CLOSE_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);
    /**
     * Plugin configuration.
     */
    private String id;
    private int corePoolSize;
    private int maximumPoolSize;
    private long keepAliveTimeSeconds = DEFAULT_KEEP_ALIVE_TIME_SECONDS;
    private int queueSize = DEFAULT_QUEUE_SIZE;
    private boolean allowCoreThreadTimeOut = Boolean.TRUE;
    private String namePrefix;
    private boolean daemon = Boolean.TRUE;
    private int closeTimeout = DEFAULT_CLOSE_TIMEOUT;
    /**
     * Whether to use virtual threads Executors.newThreadPerTaskExecutor
     */
    private boolean useVirtualThreadPerTaskExecutor;
    private boolean useFiber;
    private int fiberParallel;
    private boolean shareSchedule;

    /**
     * Parse thread pool configuration information and generate configuration class.
     *
     * @param id plugin name
     * @param extMap configuration
     * @return thread pool config
     */
    public static ThreadPoolConfig parse(String id, Map<String, Object> extMap) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(extMap, "extMap");
        ThreadPoolConfig config = new ThreadPoolConfig();
        config.id = id;
        config.useVirtualThreadPerTaskExecutor = MapUtils.getBooleanValue(extMap, USE_VIRTUAL_THREAD_PER_TASK_EXECUTOR,
                Boolean.FALSE);
        config.useFiber = MapUtils.getBooleanValue(extMap, USE_FIBER, Boolean.FALSE);
        if (config.useFiber()) {
            config.corePoolSize = MapUtils.getIntValue(extMap, CORE_POOL_SIZE, DEFAULT_BIZ_VIRTUAL_CORE_THREADS);
            config.maximumPoolSize = MapUtils.getIntValue(extMap, MAXIMUM_POOL_SIZE, DEFAULT_BIZ_VIRTUAL_MAX_THREADS);
            config.shareSchedule = MapUtils.getBooleanValue(extMap, SHARE_SCHEDULE, Boolean.TRUE);
            config.fiberParallel = MapUtils.getIntValue(extMap, FIBER_PARALLEL, Constants.CPUS);
        } else {
            config.corePoolSize = MapUtils.getIntValue(extMap, CORE_POOL_SIZE, 0);
            config.maximumPoolSize = MapUtils.getIntValue(extMap, MAXIMUM_POOL_SIZE, config.corePoolSize);
        }
        config.keepAliveTimeSeconds = MapUtils.getLongValue(extMap, KEEP_ALIVE_TIME_SECONDS,
                DEFAULT_KEEP_ALIVE_TIME_SECONDS);
        config.queueSize = MapUtils.getIntValue(extMap, QUEUE_SIZE, DEFAULT_QUEUE_SIZE);
        config.allowCoreThreadTimeOut = MapUtils.getBoolean(extMap, ALLOW_CORE_THREAD_TIMEOUT, Boolean.TRUE);
        config.namePrefix = MapUtils.getString(extMap, NAME_PREFIX, id);
        config.daemon = MapUtils.getBoolean(extMap, DAEMON, Boolean.TRUE);
        config.closeTimeout = MapUtils.getIntValue(extMap, CLOSE_TIMEOUT, DEFAULT_CLOSE_TIMEOUT);
        return config;
    }

    public static void validate(PluginConfig config) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(config.getProperties());
        ThreadPoolConfig.parse(config.getName(), config.getProperties()).validate();
    }

    public void validate() {
        PreconditionUtils.checkArgument(corePoolSize >= 0, "id[%s],corePoolSize[%s] should >= 0", id,
                corePoolSize);
        PreconditionUtils.checkArgument(queueSize >= 0, "id[%s],queueSize[%s] should >= 0", id,
                queueSize);
        PreconditionUtils.checkArgument(closeTimeout >= 0, "id[%s],queueSize[%s] should >= 0", id,
                closeTimeout);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = Maps.newHashMap();
        map.put(CORE_POOL_SIZE, corePoolSize);
        map.put(MAXIMUM_POOL_SIZE, maximumPoolSize <= 0 ? corePoolSize : maximumPoolSize);
        map.put(KEEP_ALIVE_TIME_SECONDS, keepAliveTimeSeconds);
        map.put(QUEUE_SIZE, queueSize);
        map.put(ALLOW_CORE_THREAD_TIMEOUT, allowCoreThreadTimeOut);
        map.put(NAME_PREFIX, namePrefix);
        map.put(DAEMON, daemon);
        map.put(CLOSE_TIMEOUT, closeTimeout);
        map.put(USE_VIRTUAL_THREAD_PER_TASK_EXECUTOR, useVirtualThreadPerTaskExecutor);
        map.put(USE_FIBER, useFiber);
        map.put(FIBER_PARALLEL, fiberParallel);
        map.put(SHARE_SCHEDULE, shareSchedule);
        return map;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public ThreadPoolConfig setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
        return this;
    }

    public String getId() {
        return id;
    }

    public ThreadPoolConfig setId(String id) {
        this.id = id;
        return this;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public ThreadPoolConfig setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
        return this;
    }

    public long getKeepAliveTimeSeconds() {
        return keepAliveTimeSeconds;
    }

    public ThreadPoolConfig setKeepAliveTimeSeconds(long keepAliveTimeSeconds) {
        this.keepAliveTimeSeconds = keepAliveTimeSeconds;
        return this;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public ThreadPoolConfig setQueueSize(int queueSize) {
        this.queueSize = queueSize;
        return this;
    }

    public boolean isAllowCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    public ThreadPoolConfig setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
        this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
        return this;
    }

    public String getNamePrefix() {
        return namePrefix;
    }

    public ThreadPoolConfig setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
        return this;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public ThreadPoolConfig setDaemon(boolean daemon) {
        this.daemon = daemon;
        return this;
    }

    public int getCloseTimeout() {
        return closeTimeout;
    }

    public ThreadPoolConfig setCloseTimeout(int closeTimeout) {
        this.closeTimeout = closeTimeout;
        return this;
    }

    public boolean useVirtualThreadPerTaskExecutor() {
        return useVirtualThreadPerTaskExecutor;
    }

    public void setUseVirtualThreadPerTaskExecutor(boolean useVirtualThreadPerTaskExecutor) {
        this.useVirtualThreadPerTaskExecutor = useVirtualThreadPerTaskExecutor;
    }

    public boolean useFiber() {
        return useFiber;
    }

    public void setUseFiber(boolean useFiber) {
        this.useFiber = useFiber;
    }

    public boolean isShareSchedule() {
        return shareSchedule;
    }

    public void setShareSchedule(boolean shareSchedule) {
        this.shareSchedule = shareSchedule;
    }

    public int getFiberParallel() {
        return fiberParallel;
    }

    public void setFiberParallel(int fiberParallel) {
        this.fiberParallel = fiberParallel;
    }

    @Override
    public String toString() {
        return "ThreadPoolConfig{"
                + "id='" + id + '\''
                + ", corePoolSize=" + corePoolSize
                + ", maximumPoolSize=" + maximumPoolSize
                + ", keepAliveTimeSeconds=" + keepAliveTimeSeconds
                + ", queueSize=" + queueSize
                + ", allowCoreThreadTimeOut=" + allowCoreThreadTimeOut
                + ", namePrefix='" + namePrefix + '\''
                + ", daemon=" + daemon
                + ", closeTimeout=" + closeTimeout
                + ", useFiber=" + useFiber
                + ", shareSchedule=" + shareSchedule
                + '}';
    }

}
