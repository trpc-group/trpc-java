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

package com.tencent.trpc.registry.transporter;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.registry.common.RegistryCenterConfig;
import com.tencent.trpc.registry.transporter.StateListener.State;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract class of zookeeper client, encapsulating basic operations
 */
public abstract class AbstractZookeeperClient<DataListenerT, ChildListenerT> implements
        ZookeeperClient {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractZookeeperClient.class);

    /**
     * Connection status
     */
    protected State state;
    /**
     * Connection configuration
     */
    private final RegistryCenterConfig config;

    /**
     * Listeners for connection state changes
     */
    private final Set<StateListener> stateListeners = new CopyOnWriteArraySet<>();

    /**
     * Listener on child node changes
     */
    private final ConcurrentMap<String, ConcurrentMap<ChildListener, ChildListenerT>>
            childListeners = new ConcurrentHashMap<>();

    /**
     * Listener when zookeeper node content changes
     */
    private final ConcurrentMap<String, ConcurrentMap<DataListener, DataListenerT>>
            dataListeners = new ConcurrentHashMap<>();

    /**
     * Whether zookeeper client is closed
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public AbstractZookeeperClient(RegistryCenterConfig config) {
        this.config = config;
    }

    @Override
    public void create(String path, boolean ephemeral) {
        if (!ephemeral && checkExists(path)) {
            return;
        }
        int lastIdx = path.lastIndexOf("/");
        if (lastIdx > 0) {
            create(path.substring(0, lastIdx), false);
        }
        if (ephemeral) {
            createEphemeral(path);
        } else {
            createPersistent(path);
        }
    }

    @Override
    public void create(String path, String content, boolean ephemeral) {
        if (!ephemeral && checkExists(path)) {
            delete(path);
        }
        int lastIdx = path.lastIndexOf("/");
        if (lastIdx > 0) {
            create(path.substring(0, lastIdx), false);
        }
        if (ephemeral) {
            createEphemeral(path, content);
        } else {
            createPersistent(path, content);
        }
    }

    @Override
    public List<String> addChildListener(String path, ChildListener listener) {
        Map<ChildListener, ChildListenerT> listeners = childListeners
                .computeIfAbsent(path, n -> new ConcurrentHashMap<>());
        ChildListenerT targetChildListener = listeners
                .computeIfAbsent(listener, n -> createTargetChildListener(path, listener));
        return addTargetChildListener(path, targetChildListener);
    }

    @Override
    public void removeChildListener(String path, ChildListener listener) {
        ConcurrentMap<ChildListener, ChildListenerT> listeners = childListeners.get(path);
        if (listeners != null) {
            ChildListenerT targetChildListener = listeners.remove(listener);
            if (targetChildListener != null) {
                removeTargetChildListener(path, targetChildListener);
            }
        }
    }

    @Override
    public void addDataListener(String path, DataListener listener, Executor executor) {
        ConcurrentMap<DataListener, DataListenerT> dataListenerMap = dataListeners.computeIfAbsent(
                path, s -> new ConcurrentHashMap<>());
        DataListenerT targetListener = dataListenerMap
                .computeIfAbsent(listener, l -> createTargetDataListener(path, listener));
        addTargetDataListener(path, targetListener, executor);
    }

    @Override
    public void removeDataListener(String path, DataListener listener) {
        ConcurrentMap<DataListener, DataListenerT> dataListenerMap = dataListeners.get(path);
        if (dataListenerMap != null) {
            DataListenerT targetListener = dataListenerMap.remove(listener);
            if (targetListener != null) {
                removeTargetDataListener(path, targetListener);
            }
        }
    }

    @Override
    public void addStateListener(StateListener listener) {
        stateListeners.add(listener);
    }

    @Override
    public void removeStateListener(StateListener listener) {
        stateListeners.remove(listener);
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void close() {
        if (closed.get()) {
            return;
        }
        closed.set(true);
        try {
            doClose();
        } catch (Throwable t) {
            LOGGER.warn("close client error. cause: {}", t.getMessage(), t);
        }
    }

    @Override
    public String getContent(String path) {
        return null;
    }

    /**
     * Callback interface to trigger the state change listener when the zookeeper state changes
     *
     * @param state The changed state
     */
    protected void stateChanged(State state) {
        this.state = state;
        for (StateListener sessionListener : getStatenListeners()) {
            sessionListener.stateChanged(state);
        }
    }

    /**
     * Get all status changers
     */
    public Set<StateListener> getStatenListeners() {
        return stateListeners;
    }

    @Override
    public State getState() {
        return this.state;
    }

    /**
     * Actual shutdown operation of zookeeper
     */
    protected abstract void doClose();

    /**
     * Determine if the path exists
     *
     * @param path node path
     */
    protected abstract boolean checkExists(String path);

    /**
     * Create persistent nodes
     *
     * @param path node path
     */
    protected abstract void createPersistent(String path);

    /**
     * Create persistent nodes and their contents
     *
     * @param path node path
     * @param data node content
     */
    protected abstract void createPersistent(String path, String data);

    /**
     * Create temporary nodes
     *
     * @param path node path
     */
    protected abstract void createEphemeral(String path);

    /**
     * Create temporary nodes and their contents
     *
     * @param path node path
     * @param data node content
     */
    protected abstract void createEphemeral(String path, String data);

    /**
     * Create a listener for child node changes of a node
     *
     * @param path The node to listen to
     * @param listener Node change listener
     */
    protected abstract ChildListenerT createTargetChildListener(String path,
            ChildListener listener);

    /**
     * Add a listener for changes to the node's children
     *
     * @param path The node to listen to
     * @param listener Node change listener
     * @return All child node names of the listener node
     */
    protected abstract List<String> addTargetChildListener(String path,
            ChildListenerT listener);

    /**
     * Delete the child change listener of a node
     *
     * @param path The node to listen to
     * @param listener Node change listener
     * @return All child node names of the listener node
     */
    protected abstract void removeTargetChildListener(String path, ChildListenerT listener);

    /**
     * Create a listener for content changes of nodes
     *
     * @param path The node to listen to
     * @param listener Content change listener
     */
    protected abstract DataListenerT createTargetDataListener(String path, DataListener listener);

    /**
     * Add a listener for content changes of nodes
     *
     * @param path The node to listen to
     * @param listener Content change listener
     */
    protected abstract void addTargetDataListener(String path, DataListenerT listener,
            Executor executor);

    /**
     * Delete the content change listener of a node
     *
     * @param path The node to listen to
     * @param listener Content change listener
     */
    protected abstract void removeTargetDataListener(String path, DataListenerT listener);
}
