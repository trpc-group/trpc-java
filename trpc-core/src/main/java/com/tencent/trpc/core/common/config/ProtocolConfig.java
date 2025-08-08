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

package com.tencent.trpc.core.common.config;

import com.google.common.base.Preconditions;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.rpc.RpcClient;
import com.tencent.trpc.core.rpc.RpcServer;
import com.tencent.trpc.core.rpc.RpcServerManager;
import com.tencent.trpc.core.rpc.spi.RpcClientFactory;
import com.tencent.trpc.core.utils.BinderUtils;
import com.tencent.trpc.core.utils.NetUtils;
import com.tencent.trpc.core.utils.PreconditionUtils;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;

/**
 * <pre>
 * Protocol-related configuration.
 * Note: Do we need to convert {@link ProtocolConfig} to {@link URI}?
 *       Currently, ProtocolConfig performs the same responsibilities as URI.
 * </pre>
 */
public class ProtocolConfig extends BaseProtocolConfig implements Cloneable {

    protected String name;
    protected String ip;
    protected int port;
    protected String nic;
    protected InetSocketAddress address;
    /**
     * Protocol type [stream, standard].
     */
    protected String protocolType;
    protected volatile boolean setDefault = false;
    protected volatile boolean inited = false;

    protected ServiceConfig serviceConfig;

    public static ProtocolConfig newInstance() {
        return new ProtocolConfig();
    }

    public static String toUniqId(String host, int port, String networkType) {
        return host + Constants.COLON + port + Constants.COLON + networkType;
    }

    public String toUniqId() {
        return toUniqId(ip, port, network);
    }

    /**
     * Set default values.
     */
    public synchronized void setDefault() {
        if (!setDefault) {
            setFieldDefault();
            setDefault = true;
        }
    }

    /**
     * Initialize client service
     */
    public synchronized void init() {
        if (!inited) {
            setDefault();
            inited = true;
        }
    }

    /**
     * Set protocol default values.
     */
    protected void setFieldDefault() {
        BinderUtils.bind(this);
        BinderUtils.lazyBind(this, ConfigConstants.IP, nic, obj -> NetUtils.resolveMultiNicAddr((String) obj));
        BinderUtils.bind(this, ConfigConstants.IO_THREADS, Constants.DEFAULT_IO_THREADS);
        PreconditionUtils.checkArgument(StringUtils.isNotBlank(ip), "Protocol(%s), ip is null", toSimpleString());
        BinderUtils.bind(this, "address", new InetSocketAddress(this.getIp(), this.getPort()));
    }

    public RpcClient createClient() {
        setDefault();
        RpcClientFactory extension = ExtensionLoader.getExtensionLoader(RpcClientFactory.class)
                .getExtension(getProtocol());
        PreconditionUtils.checkArgument(extension != null, "rpc client extension %s not exist",
                getProtocol());
        return extension.createRpcClient(this);
    }

    public RpcServer createServer() {
        setDefault();
        return RpcServerManager.getOrCreateRpcServer(this);
    }

    @Override
    public String toString() {
        return "ProtocolConfig [name=" + name + ", ip=" + ip + ", port=" + port + ", nic=" + nic
                + ", address=" + address + ", protocol=" + protocol + ", serializationType="
                + serialization
                + ", compressorType=" + compressor + ", keepAlive=" + keepAlive + ", charset="
                + charset
                + ", transporter=" + transporter + ", maxConns=" + maxConns + ", backlog=" + backlog
                + ", network=" + network + ", receiveBuffer=" + receiveBuffer + ", sendBuffer="
                + sendBuffer
                + ", payload=" + payload + ", idleTimeout=" + idleTimeout + ", lazyinit=" + lazyinit
                + ", connsPerAddr=" + connsPerAddr + ", connTimeout=" + connTimeout + ", ioMode="
                + ioMode
                + ", ioThreadGroupShare=" + ioThreadGroupShare + ", ioThreads=" + ioThreads
                + ", extMap="
                + extMap + ", setDefault=" + setDefault + "]";
    }

    public ProtocolConfig clone() {
        ProtocolConfig newConfig = null;
        try {
            newConfig = (ProtocolConfig) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException("", ex);
        }
        newConfig.inited = false;
        newConfig.setDefault = false;
        newConfig.setExtMap(new HashMap<>(this.getExtMap()));
        return newConfig;
    }

    /**
     * Convert to InetSocketAddress.
     *
     * @return InetSocketAddress
     */
    public InetSocketAddress toInetSocketAddress() {
        if (address == null) {
            if (StringUtils.isNotBlank(ip)) {
                return new InetSocketAddress(ip, port);
            }
        }
        return address;
    }

    /**
     * Registration information: Polaris name + protocol + ip + port + network.
     *
     * @return Polaris name + protocol + ip + port + network
     */
    public String toSimpleString() {
        return (name == null ? "<null>" : name) + ":" + protocol + ":" + ip + ":" + port + ":" + network;
    }

    public boolean useEpoll() {
        return StringUtils.equalsIgnoreCase(ioMode, Constants.IO_MODE_EPOLL);
    }

    @Override
    protected void checkFiledModifyPrivilege() {
        super.checkFiledModifyPrivilege();
        Preconditions.checkArgument(!isInited(), "Not allow to modify field,state is(init)");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        checkFiledModifyPrivilege();
        this.name = name;
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

    public Boolean isKeepAlive() {
        return keepAlive;
    }

    public Boolean isLazyinit() {
        return lazyinit;
    }

    public Boolean isIoThreadGroupShare() {
        return ioThreadGroupShare;
    }

    @Override
    public void setBossThreads(int bossThreads) {
        checkFiledModifyPrivilege();
        Preconditions.checkArgument(bossThreads < Runtime.getRuntime().availableProcessors() * 4,
                "boss threads Cannot be greater than CPU * 4:" + Runtime.getRuntime().availableProcessors() * 4);
        Preconditions.checkArgument(bossThreads < ioThreads,
                "boss threads Cannot be greater than io Threads:" + this.ioThreads);
        this.bossThreads = bossThreads;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        checkFiledModifyPrivilege();
        this.nic = nic;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        checkFiledModifyPrivilege();
        this.protocolType = protocolType;
    }

    public boolean isSetDefault() {
        return setDefault;
    }

    public boolean isInited() {
        return inited;
    }

    public ServiceConfig getServiceConfig() {
        return serviceConfig;
    }

    public  void setServiceConfig(ServiceConfig serviceConfig) {
        checkFiledModifyPrivilege();
        this.serviceConfig = serviceConfig;
    }

}