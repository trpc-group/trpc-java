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

package com.tencent.trpc.registry.zookeeper;

import static com.tencent.trpc.registry.common.Constants.ANY_VALUE;
import static com.tencent.trpc.registry.common.Constants.DEFAULT_REGISTRY_CENTER_SERVICE_TYPE;
import static com.tencent.trpc.registry.common.Constants.REGISTRY_CENTER_SERVICE_TYPE_KEY;
import static com.tencent.trpc.registry.zookeeper.common.ZookeeperConstants.ZK_PATH_SEPARATOR;

import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.registry.center.AbstractFailedRetryRegistryCenter;
import com.tencent.trpc.registry.center.NotifyListener;
import com.tencent.trpc.registry.common.RegistryCenterEnum;
import com.tencent.trpc.registry.zookeeper.common.ZookeeperRegistryCenterConfig;
import com.tencent.trpc.registry.transporter.ChildListener;
import com.tencent.trpc.registry.transporter.StateListener.State;
import com.tencent.trpc.registry.transporter.ZookeeperClient;
import com.tencent.trpc.registry.transporter.ZookeeperFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Zookeeper-Based Registry Center, inherited from {@link AbstractFailedRetryRegistryCenter}, implements only 4
 * operational interfaces for registration/subscription (including cancellation operations)
 * Storage format of data in zookeeper:
 * The first layer is the namespace, default is trpc;
 * The second layer is the service name, globally unique;
 * The third layer is the operation node of the service, each service has 3 nodes by default, providers service
 * provider node, routers route node, consumers service consumer node
 * The fourth layer is the child nodes under the operation nodes. If there are as many child nodes under the
 * providers node, it represents how many service providers, the node name is the uri string format after
 * RegisterInfo conversion
 * trpc
 * v
 * v
 * serviceName1    serviceName2      serviceName...
 * v     v      v
 * v      v       v
 * providers    routers    consumers
 * v           v           v
 * v           v           v
 * p1          r1          c1
 * p2          r2          c2
 * p..         r..         c..
 */
@Extension("zookeeper")
public class ZookeeperRegistryCenter extends AbstractFailedRetryRegistryCenter {

    private static final Logger logger = LoggerFactory.getLogger(AbstractFailedRetryRegistryCenter.class);

    /**
     * Registration center subscription service, and its callback interface and zk node listener's binding cache map
     * for the secondary cache map
     */
    private final ConcurrentMap<RegisterInfo, ConcurrentMap<NotifyListener, ChildListener>>
            zkListeners = new ConcurrentHashMap<>();

    /**
     * Zookeeper client
     */
    private ZookeeperClient zkClient;

    /**
     * Root path, default is ZK_PATH_SEPARATOR + DEFAULT_NAMESPACE
     */
    private String rootPath;

    @Override
    public void setPluginConfig(PluginConfig pluginConfig) throws TRpcExtensionException {
        super.setPluginConfig(pluginConfig);
        this.config = new ZookeeperRegistryCenterConfig(pluginConfig);
    }

    /**
     * Initialize the zookeeper client
     */
    @Override
    public void init() throws TRpcExtensionException {

        this.rootPath = getRootPath();

        // 1. get zookeeper creation factory class, default is curator
        ZookeeperFactory zookeeperFactory = ExtensionLoader.getExtensionLoader(
                ZookeeperFactory.class).getDefaultExtension();
        // 2. Create and get the zk client
        this.zkClient = zookeeperFactory.connect(this.config);
        // 3. Add a zk state change listener. When zk reconnects, re-register/subscribe to the service and cancel
        // cache invalidation. Activate cache invalidation when zk loses connection
        zkClient.addStateListener(state -> {
            if (state == State.RECONNECTED) {
                try {
                    recover();
                    cancelExpireCache();
                } catch (Exception e) {
                    logger.error("recover error. cause: {}", e.getMessage(), e);
                }
            } else if (state == State.DISCONNECTED || state == State.SUSPENDED) {
                expireCache();
            }
        });
    }

    /**
     * Register the service. Converts registerInfo to a zk node path. The default path is:
     * /trpc/serviceName/providers/registerInfo
     *
     * @param registerInfo The service to operate on
     */
    @Override
    public void doRegister(RegisterInfo registerInfo) {
        try {
            zkClient.create(toUrlPath(registerInfo), true);
        } catch (Exception e) {
            logger.error("zk doRegistry failed, registerInfo: {}", registerInfo.toString(), e);
            throw e;
        }
    }

    /**
     * Unregister the service and remove the node from the service
     *
     * @param registerInfo service of the operation
     */
    @Override
    public void doUnregister(RegisterInfo registerInfo) {
        try {
            zkClient.delete(toUrlPath(registerInfo));
        } catch (Exception e) {
            logger.error("zk doUnregistry failed, registerInfo: {}", registerInfo.toString(), e);
            throw e;
        }
    }

    /**
     * Subscribe to the service. Subscribe to the providers node by default
     * Optionally, if you also subscribe to the routes node, the parameters of registerInfo should have the
     * type:providers,routers key-value pair
     *
     * @param registerInfo service of the operation
     * @param notifyListener The listener of the service
     */
    @Override
    public void doSubscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {
        try {
            List<RegisterInfo> updatingRegisterInfos = new ArrayList<>();
            // 1. Get the type to subscribe to and convert it to the corresponding path. The default listener
            // path is /trpc/serviceName/providers
            for (String path : toRegistryTypePaths(registerInfo)) {

                // 2. Get the change listener of the child node
                ChildListener zkListener = buildChildListener(registerInfo, notifyListener);

                // 3. Create a persistent node. Child nodes can only be created under persistent nodes
                zkClient.create(path, false);

                // 4. Add child node change listeners to the node
                List<String> children = zkClient.addChildListener(path, zkListener);

                // 5. When there are child nodes under the listener node, convert all child node names to RegisterInfo
                // so that the next callback notification can be made directly
                if (CollectionUtils.isNotEmpty(children)) {
                    updatingRegisterInfos.addAll(toRegisterInfos(registerInfo, children));
                }

            }
            // 6. Notify registration discovery of updated data
            notify(registerInfo, notifyListener, updatingRegisterInfos);
        } catch (Exception e) {
            logger.error("zk doSubscribe failed, registerInfo: {}", registerInfo, e);
            throw e;
        }

    }

    /**
     * Unsubscribe from the service. Default subscription to providers node
     *
     * @param registerInfo Service of the operation
     * @param notifyListener The listener of the service
     */
    @Override
    public void doUnsubscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {
        try {
            Map<NotifyListener, ChildListener> listeners = zkListeners.get(registerInfo);
            if (listeners == null) {
                return;
            }
            ChildListener childListener = listeners.get(notifyListener);
            if (childListener == null) {
                return;
            }
            toRegistryTypePaths(registerInfo).stream()
                    .forEach(path -> zkClient.removeChildListener(path, childListener));
        } catch (Exception e) {
            logger.error("zk doUnsubscribe failed, registerInfo: {}", registerInfo.toString(), e);
            throw e;
        }
    }

    /**
     * Cancel all service registrations and subscriptions and close the zk client. Automatically invoke
     * when the framework exits
     */
    @Override
    public void destroy() {
        super.destroy();
        try {
            zkClient.close();
        } catch (Exception e) {
            logger.warn("Failed to close zookeeper client: {}, cause: {}", config,
                    e.getMessage(), e);
        }
    }

    public void setZkClient(ZookeeperClient zkClient) {
        this.zkClient = zkClient;
    }

    /**
     * Get the root path of zk
     *
     * @return root node path
     */
    private String getRootPath() {
        String namespace = ((ZookeeperRegistryCenterConfig) config).getNamespace();
        if (!namespace.startsWith(ZK_PATH_SEPARATOR)) {
            namespace = ZK_PATH_SEPARATOR + namespace;
        }
        return namespace;
    }

    /**
     * Build and cache the child node listener
     *
     * @param registerInfo Service of the operation
     * @param notifyListener The listener for the service
     * @return child node listener
     */
    private ChildListener buildChildListener(RegisterInfo registerInfo,
            NotifyListener notifyListener) {
        ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners
                .computeIfAbsent(registerInfo,
                        r -> new ConcurrentHashMap<>());
        ChildListener zkListener = listeners.get(notifyListener);
        if (zkListener == null) {
            // 创建NotifyListener与ChildListener的绑定关系。当子节点变化时，触发notify回调通知ChildListener
            listeners.put(notifyListener, (parentPath, children) -> {
                List<RegisterInfo> childUpdatingRegisterInfos = toRegisterInfos(registerInfo,
                        children);
                ZookeeperRegistryCenter.this.notify(registerInfo,
                        notifyListener,
                        childUpdatingRegisterInfos);
            });
            zkListener = listeners.get(notifyListener);
        }
        return zkListener;
    }

    /**
     * Convert RegisterInfo to uri format and use it as the full path for zk node operations.
     * Example: /trpc/serviceName/providers/registerInfo converted uri
     *
     * @param registerInfo the service to be operated on
     * @return full path of the zk node
     */
    private String toUrlPath(RegisterInfo registerInfo) {
        return this.toRegistryTypePath(registerInfo) + ZK_PATH_SEPARATOR + RegisterInfo
                .encode(registerInfo);

    }

    /**
     * Get the zk path to the service level node. Example: /trpc/serviceName
     *
     * @param registerInfo The service to be operated on
     * @return zk path to the service level node
     */
    private String toServicePath(RegisterInfo registerInfo) {
        return this.rootPath + ZK_PATH_SEPARATOR + registerInfo.getServiceName();
    }

    /**
     * Get the path to the service type layer node. Example: /trpc/serviceName/providers
     *
     * @param registerInfo The service to operate on
     * @return path to the service type level node
     */
    private String toRegistryTypePath(RegisterInfo registerInfo) {
        return this.toServicePath(registerInfo) + ZK_PATH_SEPARATOR
                + registerInfo.getParameter(REGISTRY_CENTER_SERVICE_TYPE_KEY, RegistryCenterEnum.PROVIDERS.getType());
    }

    /**
     * Get the path to all type nodes to operate on registerInfo. Example: /trpc/serviceName/providers
     * If registerInfo's parameters have type:*, get the full path of providers, consumers, and routes nodes by default
     * or get the path specified by type, you can freely combine
     * Default get path of providers node
     *
     * @param registerInfo The service to be operated.
     * @return path to all types of nodes to operate registerInfo
     */
    private List<String> toRegistryTypePaths(RegisterInfo registerInfo) {
        String[] types;
        if (ANY_VALUE.equals(registerInfo.getParameter(REGISTRY_CENTER_SERVICE_TYPE_KEY,
                DEFAULT_REGISTRY_CENTER_SERVICE_TYPE))) {
            types = Arrays.stream(RegistryCenterEnum.values())
                    .map(RegistryCenterEnum::getType).toArray(String[]::new);
        } else {
            types = registerInfo.getParameter(REGISTRY_CENTER_SERVICE_TYPE_KEY, new String[]{
                    DEFAULT_REGISTRY_CENTER_SERVICE_TYPE});
        }

        List<String> results = new ArrayList<>(types.length);
        for (int i = 0; i < types.length; i++) {
            String fullPath = toServicePath(registerInfo) + ZK_PATH_SEPARATOR + types[i];
            results.add(fullPath);
        }
        return results;
    }

    /**
     * Get all zk leaf nodes (in uri format) decoded and converted to the actual registerInfo
     *
     * @param registerInfo the service to be operated
     * @param paths all zk leaf node names (in uri format) decoded to registerInfo
     * @return all zk leaf node decoded registerInfos
     */
    private List<RegisterInfo> toRegisterInfos(RegisterInfo registerInfo, List<String> paths) {
        List<RegisterInfo> registerInfos = new ArrayList<>();
        for (String path : paths) {
            try {
                RegisterInfo pathRegisterInfo = RegisterInfo.decode(path);
                if (registerInfo.getServiceName().equals(pathRegisterInfo.getServiceName())) {
                    registerInfos.add(pathRegisterInfo);
                }
            } catch (Exception e) {
                logger.error("toRegisterInfos error, cause: {}", e.getMessage(), e);
            }
        }
        return registerInfos;
    }

}
