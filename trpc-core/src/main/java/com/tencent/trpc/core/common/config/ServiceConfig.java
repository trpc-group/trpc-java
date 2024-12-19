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

package com.tencent.trpc.core.common.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.TRpcProtocolType;
import com.tencent.trpc.core.common.config.annotation.ConfigProperty;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.filter.FilterChain;
import com.tencent.trpc.core.filter.FilterManager;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.core.registry.spi.Registry;
import com.tencent.trpc.core.rpc.RpcServer;
import com.tencent.trpc.core.rpc.RpcServerManager;
import com.tencent.trpc.core.rpc.def.DefProviderInvoker;
import com.tencent.trpc.core.utils.BinderUtils;
import com.tencent.trpc.core.utils.NetUtils;
import com.tencent.trpc.core.utils.PreconditionUtils;
import com.tencent.trpc.core.utils.RpcUtils;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Service-related configuration.
 */
@SuppressWarnings("rawtypes")
public class ServiceConfig extends BaseProtocolConfig {

    protected static final Logger logger = LoggerFactory.getLogger(ServiceConfig.class);
    /**
     * Registered name.
     */
    @ConfigProperty
    protected String name;
    /**
     * Service version.
     */
    @ConfigProperty(value = Constants.DEFAULT_VERSION)
    protected String version;
    /**
     * Service group.
     */
    @ConfigProperty(value = Constants.DEFAULT_GROUP)
    protected String group;
    @ConfigProperty
    protected String ip;
    @ConfigProperty
    protected int port;
    /**
     * Network card configuration.
     */
    @ConfigProperty(override = true)
    protected String nic;
    /**
     * Idle timeout duration.
     */
    @ConfigProperty(value = Constants.DEFAULT_SERVER_IDLE_TIMEOUT, type = Integer.class, override = true, moreZero =
            false)
    protected Integer idleTimeout;
    /**
     * Server-side business thread pool.
     */
    @ConfigProperty(value = WorkerPoolManager.DEF_PROVIDER_WORKER_POOL_NAME, override = true)
    protected String workerPool;
    /**
     * Timeout duration.
     */
    @ConfigProperty(value = Constants.DEFAULT_SERVER_TIMEOUT_MS, type = Integer.class, override = true)
    protected int requestTimeout;
    /**
     * Whether to disable default filters:
     * <p>{@link com.tencent.trpc.core.filter.ProviderInvokerHeadFilter}</p>
     * <p>{@link com.tencent.trpc.core.filter.ProviderInvokerTailFilter}</p>
     */
    @ConfigProperty(value = "false", type = Boolean.class)
    protected Boolean disableDefaultFilter;
    /**
     * Filter configuration.
     */
    @ConfigProperty(needMerged = true)
    protected List<String> filters;
    /**
     * Multi-registry configuration, key: registry id, value: registry configuration.
     */
    protected Map<String, Map<String, Object>> registries = Maps.newHashMap();
    /**
     * Extension configuration.
     */
    @ConfigProperty
    protected Map<String, Object> extMap = Maps.newHashMap();
    /**
     * Business service list.
     */
    protected List<ProviderConfig> providerConfigs = Lists.newArrayList();
    /**
     * Registry configurations.
     */
    protected List<PluginConfig> registryConfigs = Collections.emptyList();
    /***
     * Protocol configuration.
     */
    protected ProtocolConfig protocolConfig;
    /**
     * Worker pool instance.
     */
    protected WorkerPool workerPoolObj;
    /**
     * Rpc server.
     */
    protected RpcServer rpcServer;
    /**
     * HTTP base path.
     */
    @ConfigProperty
    protected String basePath;
    /**
     * Whether to enable full link timeout.
     */
    @ConfigProperty(value = "false", type = Boolean.class, override = true)
    protected Boolean enableLinkTimeout;
    /**
     * Enable execute timeout interrupt.
     */
    @ConfigProperty(value="false",type=Boolean.class,override = true)
    protected Boolean enableTimeoutInterrupt;

    protected AtomicBoolean setDefault = new AtomicBoolean(Boolean.FALSE);
    protected AtomicBoolean initialized = new AtomicBoolean(Boolean.FALSE);
    /**
     * Whether the service is started.
     */
    protected AtomicBoolean exported = new AtomicBoolean(Boolean.FALSE);
    /**
     * Whether the service is registered.
     */
    protected AtomicBoolean registered = new AtomicBoolean(Boolean.FALSE);

    /**
     * Set default values combined.
     */
    public synchronized void setDefault() {
        if (setDefault.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
            setFiledDefault();
            Preconditions.checkArgument(requestTimeout > 0, "requestTimeout should > 0");
            Preconditions.checkArgument(protocolConfig != null, "protocolConfig is null");
            // clone providerConfig to prevent providerConfig from being registered to multiple ServiceConfigs,
            // causing serviceConfig property override issues
            for (int i = 0; i < providerConfigs.size(); i++) {
                providerConfigs.set(i, providerConfigs.get(i).clone());
            }
            providerConfigs.forEach(p -> {
                p.overrideConfigDefault(this);
                p.setServiceConfig(this);
                p.setDefault();
            });
        }
    }

    public synchronized void init() {
        if (initialized.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
            setDefault();
            initFilterConfig();
            initWorkerPool();
            providerConfigs.forEach(ProviderConfig::init);
            checkAndSetProtocolType();
            initRegistryConfig();
            logger.info(">>>Init ServiceConfig, initialized info:{}", toString());
        }
    }

    /**
     * Service export.
     */
    @SuppressWarnings("unchecked")
    public synchronized void export() {
        if (exported.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
            init();
            ProtocolConfig protoConfig = this.protocolConfig;
            RpcServer server = RpcServerManager.getOrCreateRpcServer(protoConfig);
            this.rpcServer = server;
            this.providerConfigs.forEach(v -> {
                server.export(FilterChain.buildProviderChain(v, new DefProviderInvoker(protoConfig, v)));
                logger.warn(">>>Export service {} to {}", v.getRefClassName(), protoConfig.toSimpleString());
            });
            server.open();
        }
    }

    /**
     * Service registration.
     */
    public synchronized void register() {
        if (registered.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
            init();
            Preconditions.checkArgument(StringUtils.isNotBlank(name), "name is empty");
            this.registryConfigs.forEach(registryConfig -> {
                Registry extension = ExtensionLoader.getExtensionLoader(Registry.class)
                        .getExtension(registryConfig.getName());
                extension.register(new RegisterInfo(protocolConfig.getProtocol(), protocolConfig.getIp(),
                        protocolConfig.getPort(), this.getName(), this.getGroup(), this.getVersion(),
                        registries.get(registryConfig.getName())));
                logger.info("Register service(name={},protocolConfig={}) to Registry(type={}) success",
                        name, protocolConfig.toSimpleString(), registryConfig.toSimpleString());

            });
        }
    }

    /**
     * Service unregistration.
     */
    public synchronized void unRegister() {
        // to prevent partial registration failure, causing registered to remain false, do not check registered here
        this.registryConfigs.forEach(registryConfig -> {
            try {
                RegisterInfo registerInfo = new RegisterInfo(protocolConfig.getProtocol(), protocolConfig.getIp(),
                        protocolConfig.getPort(), this.getName(), this.getGroup(), this.getVersion(),
                        registries.get(registryConfig.getName()));
                ExtensionLoader.getExtensionLoader(Registry.class).getExtension(registryConfig.getName())
                        .unregister(registerInfo);
                logger.info("UnRegister service(config={}, name={}) from Registry(type={}) success",
                        protocolConfig.toSimpleString(), name, registryConfig.toSimpleString());
            } catch (Exception e) {
                logger.error("UnRegister service(config={}, name={}) from Registry(type={}) exception",
                        protocolConfig.toSimpleString(), name,
                        registryConfig.toSimpleString(), e);
            }
        });
        registered.set(Boolean.FALSE);
    }

    /**
     * Service stop.
     */
    @SuppressWarnings("unchecked")
    public synchronized void unExport() {
        // to prevent partial export failure, causing exported to remain false, do not check exported here
        providerConfigs.forEach(v -> {
            if (this.rpcServer != null) {
                try {
                    this.rpcServer.unexport(v);
                } catch (Exception ex) {
                    logger.error("UnExport exception, config={}, serviceConfig={}", v, this, ex);
                }
            }
        });
        exported.set(Boolean.FALSE);
    }

    public void addProviderConfig(ProviderConfig config) {
        this.getProviderConfigs().add(config);
    }

    protected void initFilterConfig() {
        Optional.ofNullable(filters).ifPresent(fs -> fs.forEach(FilterManager::validate));
    }

    protected void initWorkerPool() {
        Objects.requireNonNull(workerPool, "workerPoolName");
        WorkerPoolManager.validate(workerPool);
        workerPoolObj = WorkerPoolManager.get(workerPool);
        Objects.requireNonNull(workerPoolObj, "Not found worker pool with name <" + workerPool + ">");
    }

    protected void initRegistryConfig() {
        if (MapUtils.isNotEmpty(registries)) {
            registryConfigs = Lists.newArrayListWithExpectedSize(registries.size());
            Map<String, PluginConfig> map = ConfigManager.getInstance().getPluginConfigMap().get(Registry.class);

            Optional.ofNullable(map).ifPresent(m -> registries.keySet().forEach(registry -> {
                PluginConfig registryConfig = Preconditions.checkNotNull(m.get(registry),
                        "registry id[" + registry + "] not exist");
                registryConfigs.add(registryConfig);
            }));
        }
        logger.debug("serviceNaming[{}], registries:[{}]", name, registries);
    }

    /**
     * Set service default values after setting global configuration.
     */
    protected void setFiledDefault() {
        BinderUtils.bind(this);
        BinderUtils.lazyBind(this, ConfigConstants.IP, nic, obj -> NetUtils.resolveMultiNicAddr((String) obj));
        PreconditionUtils.checkArgument(StringUtils.isNotBlank(ip), "ServiceConfig(name=%s), ip is null", this.name);
        BinderUtils.bind(this, ConfigConstants.FILTERS, Lists.newArrayList());
        BinderUtils.bind(this, ConfigConstants.IO_THREADS, Constants.DEFAULT_IO_THREADS);
        checkAndSetPort();
        this.protocolConfig = buildProtocolConfig();
    }

    private void checkAndSetPort() {
        // Support random generation of available port number when port number is -1
        if (this.getPort() < 0) {
            this.setPort(NetUtils.getAvailablePort(NetUtils.getRandomPort()));
        }
    }

    /**
     * After initializing protocolConfig and providerConfigs, check if the service interfaces are mixed and set a
     * reasonable ProtocolType value.
     * Note: Streaming and non-streaming backend Transport principles are inconsistent, cannot listen to the same
     * port, and cannot be mixed.
     */
    protected void checkAndSetProtocolType() {
        TRpcProtocolType protocolType = null;
        ProviderConfig lastProviderConfig = null;
        for (ProviderConfig config : this.providerConfigs) {
            TRpcProtocolType type = RpcUtils.checkAndGetProtocolType(config.getServiceInterface());
            if (protocolType != null && protocolType != type) {
                throw new IllegalArgumentException(
                        "service impls(" + lastProviderConfig.getRefClazz() + "," + config.getRefClazz()
                                + ") have different protocol types");
            }

            protocolType = type;
            lastProviderConfig = config;
        }

        protocolType = protocolType != null ? protocolType : TRpcProtocolType.STANDARD; // default STANDARD
        this.protocolConfig.setProtocolType(protocolType.getName());
    }

    /**
     * Service configuration prioritizes server:service node configurations. When there is no configuration and the
     * value is allowed to be overridden, we use the global configuration for overriding.
     * Finally, set the default value {@link ServiceConfig#setFiledDefault()}.
     *
     * @param serverConfig server global configuration
     */
    public void overrideConfigDefault(ServerConfig serverConfig) {
        Objects.requireNonNull(serverConfig, "serverConfig");
        BinderUtils.bind(this, serverConfig, true);
        BinderUtils.bind(this, ConfigConstants.IP, serverConfig.getLocalIp());
        BinderUtils.bind(this, ConfigConstants.DISABLE_DEFAULT_FILTER, serverConfig.getDisableDefaultFilter());
        BinderUtils.bind(this, ConfigConstants.FILTERS, CollectionUtils.isNotEmpty(serverConfig.getFilters())
                ? serverConfig.getFilters() : Lists.newArrayList());
    }

    /**
     * Service configuration prioritizes server:service node configurations. When the configuration conflicts with the
     * Server and the configuration can be merged, merge the Server and Service configurations.
     * Configurations that can be merged require the needMerged in the ConfigProperty annotation to be true and
     * require a List or Map type.
     *
     * @param serverConfig server global configuration
     */
    public void mergeConfig(ServerConfig serverConfig) {
        Objects.requireNonNull(serverConfig, "serverConfig");
        BinderUtils.merge(this, serverConfig);
    }

    protected ProtocolConfig buildProtocolConfig() {
        ProtocolConfig tempProtocolConfig = new ProtocolConfig();
        tempProtocolConfig.setName(this.getName());
        tempProtocolConfig.setIp(this.getIp());
        tempProtocolConfig.setPort(this.getPort());
        tempProtocolConfig.setNic(this.getNic());
        tempProtocolConfig.setProtocol(this.getProtocol());
        tempProtocolConfig.setSerialization(this.getSerialization());
        tempProtocolConfig.setCompressor(this.getCompressor());
        tempProtocolConfig.setCompressMinBytes(this.getCompressMinBytes());
        tempProtocolConfig.setKeepAlive(this.isKeepAlive());
        tempProtocolConfig.setCharset(this.getCharset());
        tempProtocolConfig.setTransporter(this.getTransporter());
        tempProtocolConfig.setMaxConns(this.getMaxConns());
        tempProtocolConfig.setBacklog(this.getBacklog());
        tempProtocolConfig.setNetwork(this.getNetwork());
        tempProtocolConfig.setReceiveBuffer(this.getReceiveBuffer());
        tempProtocolConfig.setSendBuffer(this.getSendBuffer());
        tempProtocolConfig.setPayload(this.getPayload());
        tempProtocolConfig.setIdleTimeout(this.getIdleTimeout());
        tempProtocolConfig.setLazyinit(this.isLazyinit());
        tempProtocolConfig.setIoMode(this.getIoMode());
        tempProtocolConfig.setIoThreadGroupShare(this.isIoThreadGroupShare());
        tempProtocolConfig.setIoThreads(this.getIoThreads());
        tempProtocolConfig.setExtMap(new HashMap<>(this.getExtMap()));
        tempProtocolConfig.setFlushConsolidation(this.getFlushConsolidation());
        tempProtocolConfig.setBatchDecoder(this.getBatchDecoder());
        tempProtocolConfig.setExplicitFlushAfterFlushes(this.getExplicitFlushAfterFlushes());
        tempProtocolConfig.setReusePort(this.getReusePort());
        tempProtocolConfig.setSign(this.getSign());
        tempProtocolConfig.setDefault();
        return tempProtocolConfig;
    }


    @Override
    public String toString() {
        return "ServiceConfig [name=" + name + ", version=" + version + ", group=" + group + ", ip="
                + ip + ", port=" + port + ", nic=" + nic + ", protocol=" + protocol
                + ", serialization="
                + serialization + ", compressor=" + compressor + ", keepAlive=" + keepAlive
                + ", charset="
                + charset + ", transporter=" + transporter + ", maxConns=" + maxConns + ", backlog="
                + backlog + ", network=" + network + ", receiveBuffer=" + receiveBuffer
                + ", sendBuffer="
                + sendBuffer + ", payload=" + payload + ", idleTimeout=" + idleTimeout
                + ", lazyinit="
                + lazyinit + ", ioMode=" + ioMode + ", ioThreadGroupShare=" + ioThreadGroupShare
                + ", ioThreads=" + ioThreads + ", workerPool=" + workerPool + ", requestTimeout="
                + requestTimeout + ", filters=" + filters + ", extMap=" + extMap + ", setDefault="
                + setDefault + ", inited=" + initialized + ", exported=" + exported + ", registed="
                + registered
                + "]";
    }

    @Override
    protected void checkFiledModifyPrivilege() {
        super.checkFiledModifyPrivilege();
        Preconditions.checkArgument(!isInitialized(), "Not allow to modify field,state is(init)");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        checkFiledModifyPrivilege();
        this.name = name;
    }

    public String getWorkerPool() {
        return workerPool;
    }

    public void setWorkerPool(String workerPool) {
        checkFiledModifyPrivilege();
        this.workerPool = workerPool;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        checkFiledModifyPrivilege();
        this.requestTimeout = requestTimeout;
    }

    public List<ProviderConfig> getProviderConfigs() {
        return providerConfigs;
    }

    public void setProviderConfigs(List<ProviderConfig> providerConfigs) {
        checkFiledModifyPrivilege();
        this.providerConfigs = providerConfigs;
    }

    public ProtocolConfig getProtocolConfig() {
        return protocolConfig;
    }

    public Boolean isRegisted() {
        return registered.get();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        checkFiledModifyPrivilege();
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        checkFiledModifyPrivilege();
        this.group = group;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        checkFiledModifyPrivilege();
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        checkFiledModifyPrivilege();
        this.port = port;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        checkFiledModifyPrivilege();
        this.nic = nic;
    }

    public Boolean isKeepAlive() {
        return keepAlive;
    }

    public Boolean isLazyinit() {
        return lazyinit;
    }

    public Map<String, Map<String, Object>> getRegistries() {
        return registries;
    }

    public void setRegistries(Map<String, Map<String, Object>> registries) {
        checkFiledModifyPrivilege();
        this.registries = registries;
    }

    public Boolean getDisableDefaultFilter() {
        return disableDefaultFilter;
    }

    public void setDisableDefaultFilter(Boolean disableDefaultFilter) {
        checkFiledModifyPrivilege();
        this.disableDefaultFilter = disableDefaultFilter;
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        checkFiledModifyPrivilege();
        this.filters = filters;
    }

    public Map<String, Object> getExtMap() {
        return extMap;
    }

    public void setExtMap(Map<String, Object> extMap) {
        checkFiledModifyPrivilege();
        this.extMap = extMap;
    }

    public List<PluginConfig> getRegistryConfigs() {
        return registryConfigs;
    }

    public void setRegistryConfigs(List<PluginConfig> registryConfigs) {
        checkFiledModifyPrivilege();
        this.registryConfigs = registryConfigs;
    }

    public WorkerPool getWorkerPoolObj() {
        return workerPoolObj;
    }

    @Override
    public Integer getIdleTimeout() {
        return idleTimeout;
    }

    @Override
    public void setIdleTimeout(Integer idleTimeout) {
        checkFiledModifyPrivilege();
        this.idleTimeout = idleTimeout;
    }

    public Boolean isIoThreadGroupShare() {
        return ioThreadGroupShare;
    }

    public boolean isSetDefault() {
        return setDefault.get();
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public boolean isExported() {
        return exported.get();
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        checkFiledModifyPrivilege();
        this.basePath = basePath;
    }

    public Boolean getEnableLinkTimeout() {
        return enableLinkTimeout;
    }

    public void setEnableLinkTimeout(Boolean enableLinkTimeout) {
        checkFiledModifyPrivilege();
        this.enableLinkTimeout = enableLinkTimeout;
    }

    public Boolean getEnableTimeoutInterrupt() {
        return enableTimeoutInterrupt;
    }

    public void setEnableTimeoutInterrupt(Boolean enableTimeoutInterrupt) {
        checkFiledModifyPrivilege();
        this.enableTimeoutInterrupt = enableTimeoutInterrupt;
    }
}
