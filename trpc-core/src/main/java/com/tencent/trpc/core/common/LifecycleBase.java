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
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lifecycle abstract base class. All plugins have a lifecycle: init - start - stop.
 * Also provides listener capability LifecycleListener.
 */
public abstract class LifecycleBase implements Lifecycle {

    private static final Logger logger = LoggerFactory.getLogger(LifecycleBase.class);

    protected volatile LifecycleState state = LifecycleState.NEW;
    protected final CopyOnWriteArrayList<LifecycleListener> listeners =
            new CopyOnWriteArrayList<>();

    protected void initInternal() throws Exception {
    }

    protected void startInternal() throws Exception {
    }

    protected void stopInternal() throws Exception {
    }

    /**
     * Impl of {@link Lifecycle#init()}
     *
     * @see com.tencent.trpc.core.common.Lifecycle#init()
     */
    @Override
    public final synchronized void init() throws LifecycleException {
        if (!isNew()) {
            invalidTransition(LifecycleEvent.BEFORE_INIT_EVENT);
        }
        try {
            setStateInternal(LifecycleState.INITIALIZING);
            initInternal();
            setStateInternal(LifecycleState.INITIALIZED);
        } catch (Throwable t) {
            setStateInternal(LifecycleState.FAILED, t);
            Exception stopException = null;
            try {
                // In case of exception, trigger stop directly
                stop();
            } catch (Exception ex) {
                stopException = ex;
            }
            LifecycleException targetException =
                    (t instanceof LifecycleException) ? (LifecycleException) t
                            : new LifecycleException(
                                    "Lifecycle init fail ,obj={" + this.toString() + "}", t);
            if (stopException != null) {
                targetException.addSuppressed(stopException);
            }
            throw targetException;
        }
    }

    /**
     * impl of {@link Lifecycle#start()}
     *
     * @see com.tencent.trpc.core.common.Lifecycle#start()
     */
    @Override
    public final synchronized void start() throws LifecycleException {
        if (isStart()) {
            return;
        }
        preStart();
        try {
            setStateInternal(LifecycleState.STARTING);
            startInternal();
            setStateInternal(LifecycleState.STARTED);
        } catch (Throwable t) {
            logger.error("obj {} lifecycle start failed.", this, t);
            setStateInternal(LifecycleState.FAILED, t);
            Exception stopException = null;
            try {
                // In case of exception, trigger stop directly
                stop();
            } catch (Exception ex) {
                stopException = ex;
            }
            LifecycleException targetException =
                    (t instanceof LifecycleException) ? (LifecycleException) t
                            : new LifecycleException(
                                    "Lifecycle start fail obj={" + this.toString() + "}", t);
            if (stopException != null) {
                targetException.addSuppressed(stopException);
            }
            throw targetException;
        }
    }

    private void preStart() {
        // 1) if not initialized, trigger init first
        if (isNew()) {
            init();
            // 2) if failed, trigger stop and then start
        } else if (isFailed()) {
            stop();
        } else if (!isInitialized() && !isStopped()) {
            // 3) initialized or stopped state can transition to the start state
            invalidTransition(LifecycleEvent.BEFORE_START_EVENT);
        }
    }

    /**
     * Impl of {@link Lifecycle#stop()}
     *
     * @see com.tencent.trpc.core.common.Lifecycle#stop()
     */
    @Override
    public final synchronized void stop() throws LifecycleException {
        if (isStop()) {
            logger.error("Lifecycle stop ignore, state={}, obj={}", state, toString());
            return;
        }
        if (isNew()) {
            setStateInternal(LifecycleState.STOPPED);
            return;
        }
        if (isInvalidStateBeforeStop()) {
            invalidTransition(LifecycleEvent.BEFORE_STOP_EVENT);
        }
        try {
            setStateInternal(LifecycleState.STOPPING);
            stopInternal();
            setStateInternal(LifecycleState.STOPPED);
        } catch (Throwable t) {
            setStateInternal(LifecycleState.FAILED, t);
            if (t instanceof LifecycleException) {
                throw (LifecycleException) t;
            } else {
                throw new LifecycleException("Lifecycle stop fail, obj={" + toString() + "}", t);
            }
        }
    }


    @Override
    public void stopQuietly() {
        try {
            stop();
        } catch (Exception ex) {
            logger.error("LifeCycle stop quietly exception, obj={}, ex:", this, ex);
        }
    }

    /**
     * Whether it is an invalid state before stopping the lifecycle.
     *
     * @return true if the state is invalid, false otherwise
     */
    private boolean isInvalidStateBeforeStop() {
        return !isInitialized() && !isInitializing() && !isStarting() && !isStarted()
                && !isFailed();
    }

    /**
     * Whether it is starting or has started.
     *
     * @return true if starting or started, false otherwise
     */
    private boolean isStart() {
        return isStarting() || isStarted();
    }

    /**
     * Whether it is stopping or has stopped.
     *
     * @return true if stopping or stopped, false otherwise
     */
    private boolean isStop() {
        return isStopped() || isStopping();
    }

    /**
     * Whether it is a new state.
     *
     * @return true if it's a new state, false otherwise
     */
    public boolean isNew() {
        return getState() == LifecycleState.NEW;
    }

    /**
     * Whether it is initializing.
     *
     * @return true if initializing, false otherwise
     */
    public boolean isInitializing() {
        return getState() == LifecycleState.INITIALIZING;
    }

    /**
     * Whether it is initialized.
     *
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return getState() == LifecycleState.INITIALIZED;
    }

    /**
     * Implementation of {@link Lifecycle#isStarted()}.
     *
     * @return true if started, false otherwise
     * @see com.tencent.trpc.core.common.Lifecycle#isStarted()
     */
    @Override
    public boolean isStarted() {
        return getState() == LifecycleState.STARTED;
    }

    /**
     * Implementation of {@link Lifecycle#isStarting()}.
     *
     * @return true if starting, false otherwise
     * @see com.tencent.trpc.core.common.Lifecycle#isStarting()
     */
    @Override
    public boolean isStarting() {
        return getState() == LifecycleState.STARTING;
    }

    /**
     * Implementation of {@link Lifecycle#isStopping()}.
     *
     * @return true if stopping, false otherwise
     * @see com.tencent.trpc.core.common.Lifecycle#isStopping()
     */
    @Override
    public boolean isStopping() {
        return getState() == LifecycleState.STOPPING;
    }

    /**
     * Implementation of {@link Lifecycle#isStopped()}.
     *
     * @return true if stopped, false otherwise
     * @see com.tencent.trpc.core.common.Lifecycle#isStopped()
     */
    @Override
    public boolean isStopped() {
        return getState() == LifecycleState.STOPPED;
    }

    /**
     * Implementation of {@link Lifecycle#isFailed()}
     *
     * @see com.tencent.trpc.core.common.Lifecycle#isFailed()
     */
    @Override
    public boolean isFailed() {
        return getState() == LifecycleState.FAILED;
    }

    /**
     * Implementation of {@link Lifecycle#addListener(LifecycleListener)}
     *
     * @see Lifecycle#addListener(Lifecycle.LifecycleListener)
     */
    @Override
    public void addListener(LifecycleListener listener) {
        listeners.add(listener);
    }

    /**
     * Implementation of {@link Lifecycle#removeListener(LifecycleListener)}
     *
     * @see Lifecycle#removeListener(Lifecycle.LifecycleListener)
     */
    @Override
    public void removeListener(LifecycleListener listener) {
        listeners.remove(listener);
    }

    /**
     * Implementation of {@link Lifecycle#getState()}
     *
     * @see com.tencent.trpc.core.common.Lifecycle#getState()
     */
    @Override
    public LifecycleState getState() {
        return state;
    }

    private synchronized void setStateInternal(LifecycleState state) {
        setStateInternal(state, null);
    }

    private synchronized void setStateInternal(LifecycleState state, Throwable ex) {
        logger.debug(">>>Lifecycle state transfer,{obj={}, state({} -> {})}", this, getState(), state);
        this.state = state;
        fireLifecycleEvent(state, ex);
    }


    protected void fireLifecycleEvent(LifecycleState state, Throwable ex) {
        for (LifecycleListener each : listeners) {
            try {
                switch (state) {
                    case INITIALIZING:
                        each.onInitializing(this);
                        break;
                    case INITIALIZED:
                        each.onInitialized(this);
                        break;
                    case STARTING:
                        each.onStarting(this);
                        break;
                    case STARTED:
                        each.onStarted(this);
                        break;
                    case STOPPING:
                        each.onStopping(this);
                        break;
                    case STOPPED:
                        each.onStopped(this);
                        break;
                    case FAILED:
                        each.onFailed(this, ex);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                logger.error(">>>Lifecycle, notify exception, (obj={}, listener={}, state={})",
                        each, this.getClass(), this.getState());
            }
        }
    }

    private void invalidTransition(LifecycleEvent e) throws LifecycleException {
        String errMsg =
                "warning invalidTransition " + this + " {" + getState() + "}, event: " + e + "";
        throw new LifecycleException(errMsg);
    }

}
