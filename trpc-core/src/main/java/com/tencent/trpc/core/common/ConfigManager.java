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

import static com.tencent.trpc.core.common.TRpcProtocolType.STANDARD;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.trpc.core.cluster.RpcClusterClientManager;
import com.tencent.trpc.core.cluster.def.DefClusterInvoker;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ClientConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.GlobalConfig;
import com.tencent.trpc.core.common.config.NamingOptions;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.compressor.CompressorSupport;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.filter.Ordered;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.metrics.Metrics;
import com.tencent.trpc.core.rpc.AppInitializer;
import com.tencent.trpc.core.rpc.RpcServerManager;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.selector.SelectorManager;
import com.tencent.trpc.core.selector.spi.Selector;
import com.tencent.trpc.core.serialization.SerializationSupport;
import com.tencent.trpc.core.sign.SignSupport;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ServerConfig.class);
    private static ConfigManager instance = new ConfigManager();
    private final LifecycleObj lifecycleObj = new LifecycleObj();
    /**
     * Container startup listener
     */
    private final Set<TRPCRunListener> tRPCRunListeners = Sets.newConcurrentHashSet();
    /**
     * Business initialization
     */
    private AppInitializer appInitializer;
    /**
     * Global configuration
     */
    private GlobalConfig globalConfig = new GlobalConfig();
    /**
     * Service configuration
     */
    private ServerConfig serverConfig = new ServerConfig();
    /**
     * Client configuration
     */
    private ClientConfig clientConfig = new ClientConfig();
    /**
     * Plugin configuration
     */
    private Map<Class<?>, Map<String, PluginConfig>> pluginConfigMap = Maps.newConcurrentMap();
    /**
     * Build flag
     */
    private volatile boolean setDefault = false;
    /**
     * Stop hook function
     */
    private Thread shutdownHook;
    /**
     * TRPC service global class loader, used for TRPC framework class loading, ensuring the uniqueness of the class
     * loader
     */
    private ClassLoader cachedClassLoader;

    private ConfigManager() {
    }

    public static ConfigManager getInstance() {
        return instance;
    }

    /**
     * For test purpose
     */
    public static void stopTest() {
        instance = new ConfigManager();
        ExtensionLoader.destroyAllPlugin();
        RpcClusterClientManager.close();
        RpcServerManager.shutdown();
        RpcClusterClientManager.reset();
        RpcServerManager.reset();
        WorkerPoolManager.reset();
        instance.setDefault = false;
    }

    public static void startTest() {
        instance.initDefault();
        RpcServerManager.reset();
    }

    /**
     * Lifecycle startup, register shutdown hook function by default.
     */
    public void start() {
        start(true);
    }

    /**
     * Lifecycle startup and register shutdown hook function.
     *
     * @param registerShutdownHook registerShutdownHook whether to register the shutdown hook function
     */
    public void start(boolean registerShutdownHook) {
        lifecycleObj.start();
        if (registerShutdownHook) {
            this.registerShutdownHook();
        }
    }

    /**
     * Lifecycle stop.
     */
    public void stop() {
        lifecycleObj.stop();
    }

    /**
     * Execute the listener list when starting the container.
     */
    private void tRPCStarting() {
        mergeTRPCRunListenersFromServerConfig();
        tRPCRunListeners.forEach(TRPCRunListener::starting);
    }

    private void mergeTRPCRunListenersFromServerConfig() {
        serverConfig.getRunListeners()
                .stream()
                .map(TRPCRunListenerManager::getExtension)
                .sorted(Comparator.comparingInt(Ordered::getOrder))
                .forEach(tRPCRunListeners::add);
    }


    private void initPlugin() {
        logger.info(">>>Starting init plugin");
        // Initialize the configured plugins once, and start the process if the initialization is implemented
        getPluginConfigMap().forEach((pluginClass, value) -> value.values().forEach(pluginConfig ->
                ExtensionLoader.getExtensionLoader(pluginClass).getExtension(pluginConfig.getName())));
        logger.info(">>>Started init plugin");
    }

    public synchronized void setDefault() {
        if (!setDefault) {
            initDefault();
            globalConfig.setDefault();
            clientConfig.setDefault();
            serverConfig.setDefault();
            setDefault = true;
        }
    }

    private void initDefault() {
        // Register plugin configuration in ExtensionLoader
        pluginConfigMap.values().stream().map(Map::values).flatMap(Collection::stream).forEach(this::registerPlugin);
        WorkerPoolManager.registDefaultPluginConfig();
    }

    /**
     * Register stop hook function.
     */
    public synchronized void registerShutdownHook() {
        if (this.shutdownHook == null) {
            this.shutdownHook = new Thread(this::stop, "container-shutdown-hook");
            Runtime.getRuntime().addShutdownHook(this.shutdownHook);
        }
    }

    /**
     * Register plugin, just proxy.
     */
    public synchronized <T, K extends T> void registerPlugin(PluginConfig pluginConfig) {
        ExtensionLoader.registerPlugin(pluginConfig);
    }

    public GlobalConfig getGlobalConfig() {
        return globalConfig;
    }

    public void setGlobalConfig(GlobalConfig globalConfig) {
        this.globalConfig = globalConfig;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public void setClientConfig(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    public Map<Class<?>, Map<String, PluginConfig>> getPluginConfigMap() {
        return pluginConfigMap;
    }

    public void setPluginConfigMap(Map<Class<?>, Map<String, PluginConfig>> pluginConfigMap) {
        this.pluginConfigMap = pluginConfigMap;
    }

    public AppInitializer getAppInitializer() {
        return appInitializer;
    }

    public void setAppInitializer(AppInitializer appInitializer) {
        this.appInitializer = appInitializer;
    }

    public ClassLoader getCachedClassLoader() {
        return cachedClassLoader;
    }

    public void addTRPCRunListener(TRPCRunListener trpcRunListener) {
        // if the container is starting or started, adding is not allowed
        if (lifecycleObj.isStarting() || lifecycleObj.isStarted()) {
            return;
        }
        tRPCRunListeners.add(trpcRunListener);
    }

    @Override
    public String toString() {
        return "ApplicationConfig {globalConfig=" + globalConfig + ", serverConfig=" + serverConfig
                + ", clientConfig=" + clientConfig + ", pluginConfigMap=" + pluginConfigMap + "}";
    }

    private class LifecycleObj extends LifecycleBase {

        @Override
        protected void startInternal() throws Exception {
            super.startInternal();
            tRPCStarting();
            // 1) set default configuration
            setDefault();
            // 2) start client
            clientConfig.init();
            // 3) initialize plugins
            initPlugin();
            Metrics.init();
            // 4) preheat
            warmup();
            // 5) initialize business application related
            if (appInitializer != null) {
                appInitializer.init();
            }
            // 6) start service
            serverConfig.init();
            // 7) register service
            serverConfig.register();
            // 8) cache startup class loader
            cachedClassLoader = ConfigManager.getInstance().getClass().getClassLoader();
        }

        private void warmupCodec() {
            try {
                SerializationSupport.preLoadSerialization();
                CompressorSupport.preLoadCompressors();
                SignSupport.preLoadSign();
            } catch (Exception ex) {
                logger.warn("Warm up warmupCodec exception：", ex);
            }
        }

        private void warmupSelector(BackendConfig backendConfig) {
            // warmup naming selector
            Optional.ofNullable(backendConfig.getNamingOptions())
                    .ifPresent(nm -> {
                        try {
                            String selectorId = nm.getSelectorId();
                            SelectorManager.getManager().get(selectorId).warmup(backendConfig.toNamingServiceId());
                            logger.warn("Warm up selector success。(selectorId={},naming={}) ", selectorId,
                                    backendConfig.getNamingOptions().getServiceNaming());
                        } catch (Exception ex) {
                            logger.warn("Warm up selector exception。(selectorId={}, naming={}) ",
                                    nm.getSelectorId(), nm.getServiceNaming(), ex);
                        }
                    });
        }

        private void warmupClient(BackendConfig backendConfig) {
            try {
                backendConfig.getDefaultProxy();
                NamingOptions namingOptions = backendConfig.getNamingOptions();
                Selector selector = SelectorManager.getManager().get(namingOptions.getSelectorId());
                selector.asyncSelectAll(backendConfig.toNamingServiceId(), new DefRequest()).whenComplete(
                        (serviceInstances, throwable) -> serviceInstances.forEach(serviceInstance -> {
                            logger.warn("Warm up client create&open ::::host={} port={}   getLazyinit={}",
                                    serviceInstance.getHost(), serviceInstance.getPort(),
                                    backendConfig.getLazyinit());
                            ProtocolConfig c = backendConfig.generateProtocolConfig(serviceInstance.getHost(),
                                    serviceInstance.getPort(),
                                    backendConfig.getNetwork(), backendConfig.getExtMap());
                            c.createClient();
                            if (STANDARD.equals(TRpcProtocolType.valueOfName(c.getProtocolType()))) {
                                if (!clientConfig.isLazyinit() && !backendConfig.getLazyinit()) {
                                    logger.warn("Warm up client create&open start! Name={},ServiceInterface={}",
                                            backendConfig.getName(), backendConfig.getServiceInterface());
                                    ConsumerConfig consumerConfig = backendConfig.newConsumerConfig(
                                            backendConfig.getServiceInterface());
                                    DefClusterInvoker defClusterInvoker = new DefClusterInvoker(consumerConfig);
                                    defClusterInvoker.createInvoker(serviceInstance);
                                }
                            }
                            logger.warn("Warm up client create&open success! Warm up Name={},ServiceInterface={}",
                                    backendConfig.getName(), backendConfig.getServiceInterface());
                        }));
            } catch (Exception ex) {
                logger.warn("Warm up client create&open exception(getName={}, getBackendConfig={}), Exception={}",
                        backendConfig.getName(), backendConfig, ex.getMessage(), ex);
            }
        }

        /**
         * If preheating fails, only warn
         */
        private void warmup() {
            warmupCodec();
            clientConfig.getBackendConfigMap().values().forEach((backendConfig) -> {
                warmupSelector(backendConfig);
                warmupClient(backendConfig);
            });
        }


        @Override
        protected void stopInternal() throws Exception {
            super.stopInternal();
            logger.info(">>>tRPC Server stopping");
            long closeTime = -1;
            long waitTime = -1;
            // 1) unregister
            if (serverConfig != null) {
                serverConfig.unregister();
                closeTime = serverConfig.getCloseTimeout();
                waitTime = serverConfig.getWaitTimeout();
            }
            // 2) wait waitTime before stop service
            Thread.sleep(waitTime);
            // 3) service stop, do not accept new requests
            if (serverConfig != null) {
                serverConfig.stop();
            }
            // 4) business-related
            if (appInitializer != null) {
                appInitializer.stop();
            }
            // 5) wait for threads to close
            WorkerPoolManager.shutdown(closeTime, TimeUnit.MILLISECONDS);
            // 6) close the client side
            clientConfig.stop();
            // 7) close plugins
            ExtensionLoader.destroyAllPlugin();
            // 8) close client cluster
            RpcClusterClientManager.close();
            logger.info(">>>tRPC Server stopped");
        }

        @Override
        public String toString() {
            return ConfigManager.this.toString();
        }
    }

}
