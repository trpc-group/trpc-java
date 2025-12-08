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

package com.tencent.trpc.registry.transporter.curator;


import static com.tencent.trpc.registry.transporter.common.Constants.CLIENT_CONN_TIMEOUT_MS;
import static com.tencent.trpc.registry.transporter.common.Constants.RETRY_TIMES;
import static com.tencent.trpc.registry.transporter.common.Constants.SLEEP_MS_BETWEEN_RETRIES;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.registry.common.RegistryCenterConfig;
import com.tencent.trpc.registry.transporter.AbstractZookeeperClient;
import com.tencent.trpc.registry.transporter.ChildListener;
import com.tencent.trpc.registry.transporter.DataListener;
import com.tencent.trpc.registry.transporter.StateListener.State;
import com.tencent.trpc.registry.transporter.curator.CuratorZookeeperClient.CuratorChildWatcherImpl;
import com.tencent.trpc.registry.transporter.curator.CuratorZookeeperClient.CuratorDataCacheImpl;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.WatchedEvent;


/**
 * Curator-based zookeeper client, which implements the basic operations of zookeeper, and related listeners
 */
public class CuratorZookeeperClient extends
        AbstractZookeeperClient<CuratorDataCacheImpl, CuratorChildWatcherImpl> {

    protected static final Logger logger = LoggerFactory.getLogger(CuratorZookeeperClient.class);

    /**
     * The character set of the zookeeper node content, coded and decoded using
     */
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    /**
     * Zookeeper client
     */
    private CuratorFramework client;

    /**
     * Zookeeper content caching for nodes
     */
    private final Map<String, CuratorCache> curatorCacheMap = new ConcurrentHashMap<>();


    /**
     * Create and initialize the curator-based zookeeper client
     *
     * @param config Registration Center Configuration Items
     */
    public CuratorZookeeperClient(RegistryCenterConfig config) {
        super(config);
        try {
            // 1. Configure zookeeper basic configuration, address, retry policy, timeout time
            int timeout = Math.max(config.getConnTimeoutMs(), CLIENT_CONN_TIMEOUT_MS);
            CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                    .connectString(getCuratorConnectString(config))
                    .retryPolicy(new RetryNTimes(RETRY_TIMES, SLEEP_MS_BETWEEN_RETRIES))
                    .connectionTimeoutMs(timeout);

            // 2. Configure the authentication information of zk
            if (StringUtils.isNotEmpty(config.getUsername())
                    && StringUtils.isNotEmpty(config.getPassword())) {
                String authority =
                        String.format("%s:%s", config.getUsername(), config.getPassword());
                builder.authorization("digest", authority.getBytes());
            }

            // 3. Build the zookeeper client
            client = builder.build();

            // 4. Add zookeeper status change listener
            client.getConnectionStateListenable().addListener((client, newState) -> {
                switch (newState) {
                    case LOST:
                        stateChanged(State.DISCONNECTED);
                        break;
                    case CONNECTED:
                        stateChanged(State.CONNECTED);
                        break;
                    case RECONNECTED:
                        stateChanged(State.RECONNECTED);
                        break;
                    case SUSPENDED:
                        stateChanged(State.SUSPENDED);
                        break;
                    default:
                        logger.warn("Connection state can't identify, ignore it: {}", newState);
                }
            });

            // 5. Start zookeeper client
            client.start();
        } catch (Exception e) {
            throw new IllegalArgumentException("start curator failed.", e);
        }
    }

    /**
     * Whether the zookeeper client is connected.
     */
    @Override
    public boolean isConnected() {
        return this.client.getZookeeperClient().isConnected();
    }

    /**
     * The actual closing interface of the zookeeper client
     */
    @Override
    protected void doClose() {
        try {
            client.close();
        } catch (Exception e) {
            logger.error("Fail to close zookeeper. error: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if the path exists
     *
     * @param path node path
     */
    @Override
    protected boolean checkExists(String path) {
        try {
            return client.checkExists().forPath(path) != null;
        } catch (Exception e) {
            logger.error("check {} exists failed", path, e);
        }
        return false;
    }

    /**
     * Creating persistent nodes
     *
     * @param path node path
     */
    @Override
    protected void createPersistent(String path) {
        try {
            client.create().forPath(path);
        } catch (NodeExistsException e) {
            logger.warn("zk node already exists. path: {}", path);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Creating persistent nodes and their contents
     *
     * @param path node path
     * @param data node data
     */
    @Override
    protected void createPersistent(String path, String data) {
        byte[] dataBytes = data.getBytes(CHARSET);
        try {
            client.create().forPath(path, dataBytes);
        } catch (NodeExistsException e) {
            try {
                client.setData().forPath(path, dataBytes);
            } catch (Exception e1) {
                throw new IllegalStateException(e.getMessage(), e1);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Create temporary nodes
     *
     * @param path node path
     */
    @Override
    protected void createEphemeral(String path) {
        try {
            client.create().withMode(CreateMode.EPHEMERAL).forPath(path);
        } catch (NodeExistsException e) {
            logger.warn("zk node already exists. path: {}", path);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Create temporary nodes and their contents
     *
     * @param path node path
     * @param data node content
     */
    @Override
    protected void createEphemeral(String path, String data) {
        byte[] dataBytes = data.getBytes(CHARSET);
        try {
            client.create().withMode(CreateMode.EPHEMERAL).forPath(path, dataBytes);
        } catch (NodeExistsException e) {
            try {
                client.setData().forPath(path, dataBytes);
            } catch (Exception e1) {
                throw new IllegalStateException(e.getMessage(), e1);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Create a listener for child node changes of a node
     *
     * @param path The node to listen to
     * @param listener Node change listener
     */
    @Override
    protected CuratorChildWatcherImpl createTargetChildListener(String path,
            ChildListener listener) {
        return new CuratorChildWatcherImpl(client, listener);
    }

    /**
     * Add a listener for changes to the node's children
     *
     * @param path The node to listen to
     * @param listener Node change listener
     * @return All child node names of the listener node
     */
    @Override
    protected List<String> addTargetChildListener(String path, CuratorChildWatcherImpl listener) {
        try {
            return client.getChildren().usingWatcher(listener).forPath(path);
        } catch (NoNodeException e) {
            logger.warn("zookeeper node doesn't exist. path: {}", path);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * Delete the child change listener of a node
     *
     * @param path The node to listen to
     * @param listener Node change listener
     * @return All child node names of the listener node
     */
    @Override
    protected void removeTargetChildListener(String path, CuratorChildWatcherImpl listener) {
        listener.unwatch();
    }

    /**
     * Create a listener for content changes of nodes
     *
     * @param path The node to listen to
     * @param listener Content change listener
     */
    @Override
    protected CuratorDataCacheImpl createTargetDataListener(String path, DataListener listener) {
        return new CuratorDataCacheImpl(listener);
    }

    /**
     * Add a listener for content changes of nodes
     *
     * @param path The node to listen to
     * @param listener Content change listener
     */
    @Override
    protected void addTargetDataListener(String path, CuratorDataCacheImpl listener,
            Executor executor) {
        try {
            CuratorCache curatorCache = CuratorCache.builder(client, path).build();
            if (executor == null) {
                curatorCache.listenable().addListener(listener);
            } else {
                curatorCache.listenable().addListener(listener, executor);
            }
            curatorCacheMap.put(path, curatorCache);
            curatorCache.start();
        } catch (Exception e) {
            logger.error("Failed to add target data listener. path : {}, cause: {}", path,
                    e.getMessage(), e);
        }
    }

    /**
     * Delete the content change listener of a node
     *
     * @param path The node to listen to
     * @param listener Content change listener
     */
    @Override
    protected void removeTargetDataListener(String path, CuratorDataCacheImpl listener) {
        CuratorCache curatorCache = curatorCacheMap.get(path);
        if (curatorCache != null) {
            curatorCache.listenable().removeListener(listener);
        }
        listener.setDataListener(null);
    }

    /**
     * Delete node
     *
     * @param path The path of the deleted node
     */
    @Override
    public void delete(String path) {
        try {
            client.delete().forPath(path);
        } catch (NoNodeException e) {
            logger.warn("zookeeper node doesn't exist. path: {}", path);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Get the names of all children of the node
     *
     * @param path parent node
     * @return all child node names
     */
    @Override
    public List<String> getChildren(String path) {
        try {
            return client.getChildren().forPath(path);
        } catch (NoNodeException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Get the connection address of zookeeper client
     *
     * @param config zookeeper client connection configuration
     * @return zookeeper client connection address
     */
    private String getCuratorConnectString(RegistryCenterConfig config) {
        if (StringUtils.isNotEmpty(config.getAddresses())) {
            return config.getAddresses();
        }
        throw new IllegalStateException("curator can't get addresses");
    }

    public void setClient(CuratorFramework client) {
        this.client = client;
    }

    /**
     * Event triggers for child node change listeners of a listener node
     */
    static class CuratorChildWatcherImpl implements CuratorWatcher {

        /**
         * zookeeper client
         */
        private CuratorFramework client;

        /**
         * Child node change listener
         */
        private volatile ChildListener childListener;

        CuratorChildWatcherImpl(CuratorFramework client, ChildListener childListener) {
            this.client = client;
            this.childListener = childListener;
        }

        public void unwatch() {
            this.childListener = null;
        }

        /**
         * Trigger interface for child node changes
         *
         * @param event zookeeper change listener event
         */
        @Override
        public void process(WatchedEvent event) throws Exception {
            if (logger.isDebugEnabled()) {
                logger.debug("curator watcher process. event: {}", event);
            }

            if (childListener == null) {
                return;
            }
            String path = event.getPath() == null ? "" : event.getPath();
            childListener.childChanged(path, StringUtils.isNotEmpty(path)
                    ? client.getChildren().usingWatcher(this).forPath(path)
                    : Collections.<String>emptyList());

        }

    }

    /**
     * Event triggers for the content change listener of zookeeper nodes
     */
    static class CuratorDataCacheImpl implements CuratorCacheListener {

        /**
         * Content change listener
         */
        private volatile DataListener dataListener;

        CuratorDataCacheImpl(DataListener dataListener) {
            this.dataListener = dataListener;
        }

        public DataListener getDataListener() {
            return dataListener;
        }

        public void setDataListener(DataListener dataListener) {
            this.dataListener = dataListener;
        }

        /**
         * Callback interface on content changes
         *
         * @param type type of node change, create, update, delete
         * @param oldData old content
         * @param data new content
         */
        @Override
        public void event(Type type, ChildData oldData, ChildData data) {
            if (logger.isDebugEnabled()) {
                logger.debug("zookeeper data changed. type: {}, oldData: {}, newData: {}", type, oldData, data);
            }

            if (dataListener == null) {
                return;
            }
            dataListener.dataChanged(type, oldData, data);
        }
    }
}
