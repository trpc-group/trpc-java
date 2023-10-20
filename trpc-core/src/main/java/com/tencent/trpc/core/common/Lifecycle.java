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

package com.tencent.trpc.core.common;

import com.tencent.trpc.core.exception.LifecycleException;

/**
 * Lifecycle management. All plugins have a lifecycle: init - start -stop.
 * Also provides listener capability LifecycleListener
 */
public interface Lifecycle {

    /**
     * Initialize, If initialization fails, it will trigger stop
     */
    void init() throws LifecycleException;

    /**
     * <pre>
     * Start
     * ) If the state is new, trigger init,
     * ) If the state is failed, trigger stop and then start
     * If startup fails, it will trigger stop
     * </pre>
     */
    void start() throws LifecycleException;

    void stop() throws LifecycleException;

    /**
     * Stop without throwing an exception, just log the exception scenario
     */
    void stopQuietly();

    boolean isStarted();

    boolean isStarting();

    boolean isStopping();

    boolean isStopped();

    boolean isFailed();

    LifecycleState getState();

    /**
     * Add an event listener
     */
    void addListener(LifecycleListener listener);

    /**
     * Remove an event listener
     */
    void removeListener(LifecycleListener listener);

    enum LifecycleState {
        NEW, INITIALIZING, INITIALIZED, STARTING, STARTED, STOPPING, STOPPED, FAILED;
    }

    enum LifecycleEvent {
        BEFORE_INIT_EVENT, AFTER_INIT_EVENT, //
        START_EVENT, BEFORE_START_EVENT, AFTER_START_EVENT, //
        STOP_EVENT, BEFORE_STOP_EVENT, AFTER_STOP_EVENT //
    }

    /**
     * Lifecycle event listener
     */
    interface LifecycleListener {

        void onInitializing(Lifecycle obj);

        void onInitialized(Lifecycle obj);

        void onStarting(Lifecycle obj);

        void onStarted(Lifecycle obj);

        void onStopping(Lifecycle obj);

        void onStopped(Lifecycle obj);

        void onFailed(Lifecycle obj, Throwable cause);
    }

}
