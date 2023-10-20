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

import com.tencent.trpc.registry.transporter.StateListener.State;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Interface class for zookeeper clients, encapsulating basic operations
 */
public interface ZookeeperClient {

    /**
     * Create node
     *
     * @param path node path
     * @param ephemeral true for temporary nodes, false for persistent nodes
     */
    void create(String path, boolean ephemeral);

    /**
     * Create the node and the contents of the node
     *
     * @param path node path
     * @param content The content of the node
     * @param ephemeral true for temporary nodes, false for persistent nodes
     */
    void create(String path, String content, boolean ephemeral);

    /**
     * Delete the node
     *
     * @param path The path of the deleted node
     */
    void delete(String path);

    /**
     * Get the names of all children of the node
     *
     * @param path parent node
     * @return all child node names
     */
    List<String> getChildren(String path);

    /**
     * Add a listener for child node changes of the node
     *
     * @param path The node to be listened to.
     * @param listener The listener for child node changes
     * @return the names of all the children under the listener node
     */
    List<String> addChildListener(String path, ChildListener listener);

    /**
     * The listener for child node changes of the deleted node
     *
     * @param path The node to be listened to.
     * @param listener The listener for child node changes
     */
    void removeChildListener(String path, ChildListener listener);

    /**
     * Add a listener for the content change of the node
     *
     * @param path The node to be listened to.
     * @param listener The listener for content changes
     */
    void addDataListener(String path, DataListener listener, Executor executor);

    /**
     * Remove the content change listener of the node
     *
     * @param path The node to be listened to.
     * @param listener The listener for content changes
     */
    void removeDataListener(String path, DataListener listener);

    /**
     * Add a listener for zk state change
     *
     * @param listener Listener for state change
     */
    void addStateListener(StateListener listener);

    /**
     * Remove the listener for zk state changes
     *
     * @param listener Listener for state change
     */
    void removeStateListener(StateListener listener);

    /**
     * Whether the zk client is connected or not. This interface needs to be overridden
     */
    boolean isConnected();

    /**
     * Get the current state of zk
     *
     * @return zk's current state
     */
    State getState();

    /**
     * Close the zk client
     */
    void close();

    /**
     * Get the content of the node
     *
     * @param path The path to the node to get the content
     * @return the content of the node
     */
    String getContent(String path);


}
