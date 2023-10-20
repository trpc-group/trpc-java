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
import com.tencent.trpc.core.cluster.ClusterInterceptorManager;
import com.tencent.trpc.core.cluster.RpcClusterClient;
import com.tencent.trpc.core.cluster.RpcClusterClientFactory;
import com.tencent.trpc.core.cluster.RpcClusterClientManager;
import com.tencent.trpc.core.cluster.StandardRpcClusterClientFactory;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.TRpcProtocolType;
import com.tencent.trpc.core.common.config.annotation.ConfigProperty;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.filter.FilterManager;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.selector.SelectorManager;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.utils.BinderUtils;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Client service config
 * Get Proxy Demo:
 * <pre>{@code
 * BackendConfig backendConfig = new BackendConfig();
 * backendConfig.setNamingUrl("ip://127.0.0.1:12345");
 * ConsumerConfig<GreeterServiceApi> consumerConfig = new ConsumerConfig<>();
 * consumerConfig.setServiceInterface(GreeterServiceApi.class);
 * GreeterServiceApi proxy =
 * backendConfig.getProxy(consumerConfig);  or:{
 * @link com.tencent.trpc.core.rpc.TRpcProxy#getProxy(String, Class)} )}
 * </pre>
 */
@SuppressWarnings("unchecked")
public class BackendConfig extends BaseProtocolConfig {

    protected static final AtomicLong NAME_GENERATOR_INDEX = new AtomicLong();
    protected static final Logger logger = LoggerFactory.getLogger(BackendConfig.class);

    /**
     * Backend cluster name (used for lookup through TRpcProxy)
     */
    @ConfigProperty
    protected String name;
    /**
     * Timeout duration
     */
    @ConfigProperty(value = Constants.DEFAULT_CLIENT_REQUEST_TIMEOUT_MS, type = Integer.class, override = true)
    protected int requestTimeout;
    /**
     * Backup request time. In idempotent scenarios, when the request has not been responded to after this time, resend
     * the request and process the fastest response of these two requests.
     */
    @ConfigProperty(value = Constants.DEFAULT_BACKUP_REQUEST_TIME_MS, type = Integer.class, override = true)
    protected int backupRequestTimeMs;
    /**
     * Filter configuration
     */
    @ConfigProperty(needMerged = true)
    protected List<String> filters;
    /**
     * ClusterInvoker interceptor configuration, only effective for ClusterInvoker
     */
    @ConfigProperty
    protected List<String> interceptors;
    /**
     * Thread pool configuration
     */
    protected WorkerPool workerPoolObj;
    /**
     * Thread pool name
     */
    @ConfigProperty(value = WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_NAME, override = true)
    protected String workerPool;
    /**
     * Proxy type configuration
     */
    @ConfigProperty(value = Constants.DEFAULT_PROXY, override = true)
    protected String proxyType;
    /**
     * When used as an HTTP client, customize the basePath of the remote interface, default /trpc
     */
    @ConfigProperty
    protected String basePath;
    /**
     * Cluster, routing, discovery, load balancing related configuration
     */
    @ConfigProperty
    protected String namingUrl;
    /**
     * The parameters of naming's map and namingUrl together form the naming configuration and put namingOptions
     */
    @ConfigProperty(name = "naming_map")
    protected Map<String, Object> namingMap = Maps.newHashMap();
    /**
     * Environment
     */
    @ConfigProperty(override = true)
    protected String namespace;
    /**
     * Caller service name for client configuration
     */
    @ConfigProperty
    protected String callerServiceName;
    @ConfigProperty(value = Constants.DEFAULT_VERSION)
    protected String version;
    /**
     * Service group
     */
    @ConfigProperty(name = "group", value = Constants.DEFAULT_GROUP)
    protected String group;
    /**
     * Name id for routing discovery
     */
    protected NamingOptions namingOptions;
    /**
     * Service id for routing
     */
    protected ServiceId namingServiceId;
    /**
     * Cluster, routing, discovery, load balancing related configuration is generated based on name, or the client
     * configures callee by itself.
     */
    @ConfigProperty
    protected String callee = "";
    protected String calleeApp = "";
    protected String calleeServer = "";
    protected String calleeService = "";
    /**
     * Class maps to consumerConfig, the interface configuration list introduced on this cluster configuration
     */
    @SuppressWarnings("rawtypes")
    protected Map<Class/* serviceInterface */, Object/* proxy */> proxyMap = Maps
            .newConcurrentMap();
    /**
     * Default proxy.
     */
    protected Object proxy;
    /**
     * Default service configuration.
     */
    @SuppressWarnings("rawtypes")
    protected Class serviceInterface;
    /**
     * Default mock configuration.
     */
    @ConfigProperty(type = Boolean.class)
    protected boolean mock;
    /**
     * Configure mockClass.
     */
    @ConfigProperty
    protected String mockClass;
    /**
     * Mainly used for log printing.
     */
    protected String simpleString;
    protected volatile RpcClusterClient client = null;
    protected volatile boolean setDefault = false;
    protected volatile boolean inited = false;
    protected volatile boolean stoped = false;
    /**
     * RpcClusterClient factory.
     */
    private final RpcClusterClientFactory factory = new StandardRpcClusterClientFactory();

    public BackendConfig() {
        name = createNameByDefault();
    }

    /**
     * Generate proxy.
     *
     * @param <T> the type of the proxy
     * @return the default proxy
     */
    public <T> T getDefaultProxy() {
        if (serviceInterface == null) {
            return null;
        }
        if (proxy == null) {
            synchronized (this) {
                if (proxy == null) {
                    init();
                    proxy = proxyMap.computeIfAbsent(serviceInterface,
                            k -> client.getProxy(newConsumerConfig(serviceInterface, mock, mockClass)));
                }
            }
        }
        return (T) proxy;
    }

    public <T> T getProxy(@SuppressWarnings("rawtypes") ConsumerConfig config) {
        init();
        Preconditions.checkArgument(config != null, "config");
        Preconditions.checkArgument(config.getServiceInterface() != null, "config serviceInterface");
        Class<T> serviceInterface = config.getServiceInterface();
        return (T) proxyMap.computeIfAbsent(serviceInterface, k -> {
            ConsumerConfig<T> clone = config.clone();
            clone.setBackendConfig(this);
            return client.getProxy(clone);
        });
    }

    public <T> T getProxy(Class<T> serviceInterface) {
        init();
        Preconditions.checkArgument(serviceInterface != null, "config serviceInterface");
        return (T) proxyMap.computeIfAbsent(serviceInterface,
                k -> client.getProxy(newConsumerConfig(serviceInterface, this.getMock(), this.getMockClass())));
    }

    public <T> T getProxyWithSourceSet(ConsumerConfig consumerConfig, String setName) {
        extMap.put(NamingOptions.SOURCE_SET, setName);
        return getProxy(consumerConfig);
    }

    public <T> T getProxyWithSourceSet(Class<T> serviceInterface, String setName) {
        extMap.put(NamingOptions.SOURCE_SET, setName);
        return getProxy(serviceInterface);
    }

    public <T> T getProxyWithDestinationSet(ConsumerConfig<T> consumerConfig, String setName) {
        extMap.put(NamingOptions.DESTINATION_SET, setName);
        return getProxy(consumerConfig);
    }

    public <T> T getProxyWithDestinationSet(Class<T> serviceInterface, String setName) {
        extMap.put(NamingOptions.DESTINATION_SET, setName);
        return getProxy(serviceInterface);
    }

    public <T> ConsumerConfig<T> newConsumerConfig(Class<T> clazz) {
        return newConsumerConfig(clazz, false, null);
    }

    public <T> ConsumerConfig<T> newConsumerConfig(Class<T> clazz, boolean mock, String mockClass) {
        ConsumerConfig<T> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setServiceInterface(clazz);
        consumerConfig.setBackendConfig(this);
        consumerConfig.setMock(mock);
        consumerConfig.setMockClass(mockClass);
        return consumerConfig;
    }

    public String createNameByDefault() {
        return "@BackendConfig(" + NAME_GENERATOR_INDEX.getAndAdd(1) + ")";
    }

    public String toSimpleString() {
        return simpleString;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BackendConfig other = (BackendConfig) obj;
        return Objects.equals(name, other.name);
    }

    /**
     * Set default values.
     */
    public synchronized void setDefault() {
        if (!setDefault) {
            setFiledDefault();
            setCalleeInfo();
            setNamingOptions();
            setDefault = true;
        }
    }

    /**
     * Initialize.
     */
    public synchronized void init() {
        if (!inited) {
            setDefault();
            initWorkerPool();
            initFilter();
            validateInterceptor();
            namingServiceId = toNamingServiceId();
            simpleString = "BackendConfig[name=" + name + ", naming=" + namingOptions.getServiceNaming() + "]";
            logger.debug("ConsumerConfig initialized:{}", toString());
            SelectorManager.getManager().validate(namingOptions.getSelectorId());
            initRpcClusterClient();
            inited = true;
        }
    }

    /**
     * Stop service.
     */
    public synchronized void stop() {
        if (!stoped) {
            RpcClusterClientManager.shutdownBackendConfig(this);
            stoped = true;
        }
    }

    protected RpcClusterClient initRpcClusterClient() {
        if (this.client == null) {
            synchronized (this) {
                if (this.client == null) {
                    RpcClusterClient tmp = factory.create(this);
                    boolean succ = false;
                    try {
                        tmp.open();
                        this.client = tmp;
                        succ = true;
                    } finally {
                        if (!succ) {
                            tmp.close();
                        }
                    }
                }
            }
        }
        return client;
    }

    /**
     * Set backend property default values.
     */
    protected void setFiledDefault() {
        BinderUtils.bind(this);
        BinderUtils.bind(this, ConfigConstants.NAME, createNameByDefault());
        BinderUtils.bind(this, ConfigConstants.IO_THREADS, Constants.DEFAULT_IO_THREADS);
        BinderUtils.bind(this, ConfigConstants.EXT_MAP, Maps.newHashMap());
        BinderUtils.bind(this, ConfigConstants.FILTERS, Lists.newArrayList());
        BinderUtils.bind(this, ConfigConstants.INTERCEPTORS, Lists.newArrayList());
    }

    /**
     * Override backendConfig default values with clientConfig.
     *
     * @param clientConfig client configuration
     */
    public void overrideConfigDefault(ClientConfig clientConfig) {
        Objects.requireNonNull(clientConfig, "clientConfig");
        BinderUtils.bind(this, clientConfig, Boolean.TRUE);
        // When the client is not configured, use the global configuration namespace by default
        BinderUtils.bind(this, NamingOptions.NAMESPACE, ConfigManager.getInstance().getGlobalConfig().getNamespace());
        BinderUtils.bind(this, ConfigConstants.FILTERS, CollectionUtils.isNotEmpty(clientConfig.getFilters())
                ? clientConfig.getFilters() : Lists.newArrayList());
        BinderUtils.bind(this, ConfigConstants.INTERCEPTORS, CollectionUtils.isNotEmpty(clientConfig.getInterceptors())
                ? clientConfig.getInterceptors() : Lists.newArrayList());
        // If caller_service_name is configured under ClientConfig, update the variable value
        BinderUtils.bind(this, ConfigConstants.CALLER_SERVICE_NAME,
                StringUtils.isNotBlank(clientConfig.getCallerServiceName())
                        ? clientConfig.getCallerServiceName() : "");

    }

    /**
     * Service configuration prioritizes client:service node configurations. When the configuration conflicts with the
     * Client and the configuration can be merged, merge the Client and Service configurations.
     * Configurations that can be merged require the needMerged in the ConfigProperty annotation to be true and require
     * a List or Map type.
     *
     * @param clientConfig client global configuration
     */
    public void mergeConfig(ClientConfig clientConfig) {
        Objects.requireNonNull(clientConfig, "clientConfig");
        BinderUtils.merge(this, clientConfig);
    }

    protected void initWorkerPool() {
        Objects.requireNonNull(workerPool, "workerPoolName");
        WorkerPoolManager.validate(workerPool);
        workerPoolObj = WorkerPoolManager.get(workerPool);
        Objects.requireNonNull(workerPoolObj, "workerPoolObj");
    }

    protected void setCalleeInfo() {
        String serviceNaming = getNamingOptions().getServiceNaming();
        // callee is set to serviceNaming, setting is not supported
        callee = serviceNaming;

        // in the TRPC scenario, serviceId is in the format trpc.calleeapp.calleeserver.calleeservice
        if (StringUtils.isNotBlank(callee) && callee.startsWith(Constants.STANDARD_NAMING_PRE)) {
            String[] strings = callee.split("\\.");
            if (StringUtils.isBlank(calleeApp)) {
                calleeApp = (strings.length < 2 ? "" : strings[1]);
            }
            if (StringUtils.isBlank(calleeServer)) {
                calleeServer = (strings.length < 3 ? "" : strings[2]);
            }
            if (StringUtils.isBlank(calleeService)) {
                calleeService = (strings.length < 4 ? "" : strings[3]);
            }
        }
    }

    protected void initFilter() {
        Optional.ofNullable(filters).ifPresent(filters -> filters.forEach(FilterManager::validate));
    }

    /**
     * Validate if the interceptor exists.
     */
    protected void validateInterceptor() {
        Optional.ofNullable(interceptors).ifPresent(i -> i.forEach(ClusterInterceptorManager::validate));
    }

    /**
     * Check if it's a generic interface.
     *
     * @return the local address
     */
    public InetSocketAddress getLocalAddress() {
        return Optional.ofNullable(ConfigManager.getInstance().getServerConfig())
                .map(ServerConfig::getLocalAddress).orElse(null);
    }

    /**
     * Generate protocol parameters.
     *
     * @param host server address
     * @param port server port
     * @param network network type
     * @return protocol configuration
     */
    public ProtocolConfig generateProtocolConfig(String host, int port, String network) {
        return generateProtocolConfig(host, port, network, TRpcProtocolType.STANDARD.getName());
    }

    /**
     * Generate protocol parameters.
     *
     * @param host server address
     * @param port server port
     * @param network network type
     * @param protocolType protocol type [stream, standard]
     * @return protocol configuration
     */
    public ProtocolConfig generateProtocolConfig(String host, int port, String network, String protocolType) {
        ProtocolConfig config = new ProtocolConfig();
        config.setIp(host);
        config.setPort(port);
        config.setNetwork(network);
        config.setIdleTimeout(idleTimeout);
        config.setCharset(charset);
        config.setLazyinit(lazyinit);
        config.setConnsPerAddr(connsPerAddr);
        config.setConnTimeout(connTimeout);
        config.setProtocol(protocol);
        config.setSerialization(serialization);
        config.setCompressor(compressor);
        config.setCompressMinBytes(compressMinBytes);
        config.setSendBuffer(sendBuffer);
        config.setReceiveBuffer(receiveBuffer);
        config.setIoThreadGroupShare(ioThreadGroupShare);
        config.setIoThreads(ioThreads);
        config.setTransporter(transporter);
        config.setBacklog(backlog);
        config.setBatchDecoder(batchDecoder);
        config.setExplicitFlushAfterFlushes(explicitFlushAfterFlushes);
        config.setPayload(payload);
        config.setFlushConsolidation(flushConsolidation);
        config.setIoMode(ioMode);
        config.setKeepAlive(keepAlive);
        config.setMaxConns(maxConns);
        config.setProtocolType(protocolType);
        config.setSign(this.getSign());
        config.setDefault();
        return config;
    }

    /**
     * Generate protocol parameters.
     *
     * @param host server address
     * @param port server port
     * @param network network type
     * @param extMap extension map
     * @return protocol configuration
     */
    public ProtocolConfig generateProtocolConfig(String host, int port, String network, Map<String, Object> extMap) {
        return generateProtocolConfig(host, port, network, TRpcProtocolType.STANDARD.getName(), extMap);
    }

    /**
     * Generate protocol parameters.
     *
     * @param host server address
     * @param port server port
     * @param network network type
     * @param protocolType protocol type [stream, standard]
     * @param extMap extension configuration
     * @return protocol configuration
     */
    public ProtocolConfig generateProtocolConfig(String host, int port, String network, String protocolType,
            Map<String, Object> extMap) {
        ProtocolConfig config = this.generateProtocolConfig(host, port, network, protocolType);
        config.setExtMap(extMap);
        return config;
    }

    /**
     * Construct serviceId.
     *
     * @return serviceId
     */
    public ServiceId toNamingServiceId() {
        if (namingServiceId != null) {
            return namingServiceId;
        }
        Objects.requireNonNull(namingOptions, "namingOptions");
        ServiceId serviceId = new ServiceId();
        serviceId.setGroup(group);
        serviceId.setServiceName(namingOptions.getServiceNaming());
        serviceId.setVersion(version);
        serviceId.setParameters(namingOptions.getExtMap());
        if (StringUtils.isNotBlank(callerServiceName)) {
            serviceId.setCallerServiceName(callerServiceName);
            serviceId.setCallerNamespace(ConfigManager.getInstance().getGlobalConfig().getNamespace());
            serviceId.setCallerEnvName(ConfigManager.getInstance().getGlobalConfig().getEnvName());
        }
        return serviceId;
    }

    /**
     * Get naming related configuration.
     *
     * @return naming options
     */
    public NamingOptions getNamingOptions() {
        if (namingOptions == null) {
            if (StringUtils.isNotBlank(namingUrl)) {
                return NamingOptions.parseNamingUrl(namingUrl, namingMap);
            }
        }
        return namingOptions;
    }

    public void setNamingOptions(NamingOptions namingOptions) {
        checkFiledModifyPrivilege();
        this.namingOptions = namingOptions;
    }

    public void setNamingOptions() {
        Preconditions.checkArgument(StringUtils.isNotBlank(namingUrl), "namingUrl");
        namingOptions = NamingOptions.parseNamingUrl(namingUrl, namingMap);
        if (!namingOptions.getExtMap().containsKey(NamingOptions.NAMESPACE)) {
            namingOptions.getExtMap().put(NamingOptions.NAMESPACE, namespace);
        }
    }

    /**
     * Thread pool configuration: associated by id for dynamic adjustment of connection pool.
     *
     * @return worker pool object
     */
    public WorkerPool getWorkerPoolObj() {
        return workerPoolObj;
    }

    @Override
    public String toString() {
        return "BackendConfig{"
                + "  factory=" + factory
                + ", name='" + name + '\''
                + ", requestTimeout=" + requestTimeout
                + ", backupRequestTimeMs=" + backupRequestTimeMs
                + ", filters=" + filters
                + ", interceptors=" + interceptors
                + ", workerPoolObj=" + workerPoolObj
                + ", workerPool='" + workerPool + '\''
                + ", proxyType='" + proxyType + '\''
                + ", basePath='" + basePath + '\''
                + ", namingUrl='" + namingUrl + '\''
                + ", namingMap=" + namingMap
                + ", namespace='" + namespace + '\''
                + ", version='" + version + '\''
                + ", group='" + group + '\''
                + ", namingOptions=" + namingOptions
                + ", namingServiceId=" + namingServiceId
                + ", callee='" + callee + '\''
                + ", calleeApp='" + calleeApp + '\''
                + ", calleeServer='" + calleeServer + '\''
                + ", calleeService='" + calleeService + '\''
                + ", proxyMap=" + proxyMap
                + ", proxy=" + proxy
                + ", serviceInterface=" + serviceInterface
                + ", mock=" + mock
                + ", mockClass='" + mockClass + '\''
                + ", simpleString='" + simpleString + '\''
                + ", client=" + client
                + ", setDefault=" + setDefault
                + ", inited=" + inited
                + ", stoped=" + stoped
                + ", callerServiceName=" + callerServiceName
                + '}';
    }

    @Override
    protected void checkFiledModifyPrivilege() {
        super.checkFiledModifyPrivilege();
        Preconditions.checkArgument(!isInited(), "Not allow to modify field,state is(init)");
    }

    public String getProxyType() {
        return proxyType;
    }

    public void setProxyType(String proxyType) {
        checkFiledModifyPrivilege();
        this.proxyType = proxyType;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        checkFiledModifyPrivilege();
        this.namespace = namespace;
    }

    public String getCallee() {
        return callee;
    }

    public void setCallee(String callee) {
        checkFiledModifyPrivilege();
        this.callee = callee;
    }

    public Boolean isLazyinit() {
        return lazyinit;
    }

    @SuppressWarnings("rawtypes")
    public Class getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(@SuppressWarnings("rawtypes") Class serviceInterface) {
        checkFiledModifyPrivilege();
        this.serviceInterface = serviceInterface;
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        checkFiledModifyPrivilege();
        this.filters = filters;
    }

    public List<String> getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(List<String> interceptors) {
        checkFiledModifyPrivilege();
        this.interceptors = interceptors;
    }

    public void setExtMap(Map<String, Object> extMap) {
        checkFiledModifyPrivilege();
        this.extMap = extMap;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        checkFiledModifyPrivilege();
        this.requestTimeout = requestTimeout;
    }

    public int getBackupRequestTimeMs() {
        return backupRequestTimeMs;
    }

    public void setBackupRequestTimeMs(int backupRequestTimeMs) {
        checkFiledModifyPrivilege();
        this.backupRequestTimeMs = backupRequestTimeMs;
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

    public String getNamingUrl() {
        return namingUrl;
    }

    public void setNamingUrl(String namingUrl) {
        checkFiledModifyPrivilege();
        this.namingUrl = namingUrl;
    }

    /**
     * target|namingUrl相同，与其他语言一致
     */
    public void setTarget(String namingUrl) {
        checkFiledModifyPrivilege();
        this.namingUrl = namingUrl;
    }

    public Map<String, Object> getNamingMap() {
        return namingMap;
    }

    public void setNamingMap(Map<String, Object> namingMap) {
        checkFiledModifyPrivilege();
        this.namingMap = namingMap;
    }

    public String getCalleeApp() {
        return calleeApp;
    }

    public void setCalleeApp(String calleeApp) {
        checkFiledModifyPrivilege();
        this.calleeApp = calleeApp;
    }

    public String getCalleeService() {
        return calleeService;
    }

    public void setCalleeService(String calleeService) {
        checkFiledModifyPrivilege();
        this.calleeService = calleeService;
    }

    public String getCalleeServer() {
        return calleeServer;
    }

    public void setCalleeServer(String calleeServer) {
        checkFiledModifyPrivilege();
        this.calleeServer = calleeServer;
    }

    public Boolean isKeepAlive() {
        return keepAlive;
    }

    public boolean isSetDefault() {
        return setDefault;
    }

    public boolean isInited() {
        return inited;
    }

    public boolean isStoped() {
        return stoped;
    }

    public boolean getMock() {
        return mock;
    }

    public void setMock(boolean mock) {
        checkFiledModifyPrivilege();
        this.mock = mock;
    }

    public String getMockClass() {
        return mockClass;
    }

    public void setMockClass(String mockClass) {
        checkFiledModifyPrivilege();
        this.mockClass = mockClass;
    }

    public Boolean isIoThreadGroupShare() {
        return ioThreadGroupShare;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        checkFiledModifyPrivilege();
        this.basePath = basePath;
    }

    public void setCallerServiceName(String callerServiceName) {
        this.callerServiceName = callerServiceName;
    }

    public String getCallerServiceName() {
        return this.callerServiceName;
    }

}
