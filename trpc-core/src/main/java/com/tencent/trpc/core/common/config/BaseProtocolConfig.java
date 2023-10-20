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

import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.config.annotation.ConfigProperty;
import java.io.Serializable;
import java.util.Map;

/**
 * Basic protocol configuration.
 */
public class BaseProtocolConfig implements Serializable, Cloneable {

    @ConfigProperty(value = Constants.DEFAULT_PROTOCOL, override = true)
    protected String protocol;
    /**
     * Serialization plugin.
     */
    @ConfigProperty(value = Constants.DEFAULT_SERIALIZATION, override = true)
    protected String serialization;
    /**
     * Compression plugin.
     */
    @ConfigProperty(value = Constants.DEFAULT_COMPRESSOR, override = true)
    protected String compressor;
    /**
     * Minimum number of bytes to enable compression.
     */
    @ConfigProperty(value = Constants.DEFAULT_COMPRESS_MIN_BYTES, type = Integer.class, override = true)
    protected int compressMinBytes;
    /**
     * Digital signature method.
     */
    @ConfigProperty(override = true)
    protected String sign;
    /**
     * Whether it is a long connection.
     */
    @ConfigProperty(value = Constants.DEFAULT_KEEP_ALIVE, type = Boolean.class, override = true)
    protected Boolean keepAlive;
    /**
     * Character encoding.
     */
    @ConfigProperty(value = Constants.DEFAULT_CHARSET, override = true)
    protected String charset;
    /**
     * Network transporter, netty, jetty etc.
     */
    @ConfigProperty(value = Constants.DEFAULT_TRANSPORTER, override = true)
    protected String transporter;
    /**
     * Maximum connection data limit.
     */
    @ConfigProperty(value = Constants.DEFAULT_MAX_CONNECTIONS, type = Integer.class, override = true)
    protected int maxConns;
    /**
     * Connection waiting queue size.
     */
    @ConfigProperty(value = Constants.DEFAULT_BACK_LOG_SIZE, type = Integer.class, override = true)
    protected int backlog;
    /**
     * Network type: udp/tcp.
     */
    @ConfigProperty(value = Constants.DEFAULT_NETWORK_TYPE, override = true)
    protected String network;
    /**
     * Receive buffer size.
     */
    @ConfigProperty(value = Constants.DEFAULT_BUFFER_SIZE, type = Integer.class, override = true)
    protected int receiveBuffer;
    /**
     * Send buffer size.
     */
    @ConfigProperty(value = Constants.DEFAULT_BUFFER_SIZE, type = Integer.class, override = true)
    protected int sendBuffer;
    /**
     * Maximum data packet size limit.
     */
    @ConfigProperty(value = Constants.DEFAULT_PAYLOAD, type = Integer.class, override = true)
    protected int payload;
    /**
     * Idle timeout duration.
     */
    @ConfigProperty(value = Constants.DEFAULT_IDLE_TIMEOUT, type = Integer.class, override = true, moreZero = false)
    protected Integer idleTimeout;
    /**
     * Whether to delay initialization.
     */
    @ConfigProperty(value = Constants.DEFAULT_LAZY_INIT, type = Boolean.class, override = true)
    protected Boolean lazyinit;
    /**
     * Number of client connections per address.
     */
    @ConfigProperty(value = Constants.DEFAULT_CONNECTIONS_PERADDR, type = Integer.class, override = true)
    protected int connsPerAddr;
    /**
     * Client connection timeout duration.
     */
    @ConfigProperty(value = Constants.DEFAULT_CONNECT_TIMEOUT, type = Integer.class, override = true)
    protected int connTimeout;
    /**
     * Network mode.
     */
    @ConfigProperty(value = Constants.DEFAULT_IO_MODE, override = true)
    protected String ioMode;
    /**
     * Whether the IO thread group is shared.
     */
    @ConfigProperty(value = Constants.DEFAULT_IO_THREAD_GROUPSHARE, type = Boolean.class, override = true)
    protected Boolean ioThreadGroupShare;
    /**
     * Number of IO threads.
     */
    @ConfigProperty(override = true)
    protected int ioThreads;
    /**
     * Number of boss threads.
     * Only when reuse_port is enabled and the server supports epoll, multiple ports can be occupied.
     * Can improve request processing reception speed.
     */
    @ConfigProperty(value = Constants.DEFAULT_BOSS_THREADS, type = Integer.class, override = true)
    protected int bossThreads;
    /**
     * Whether to enable high-throughput flush.
     */
    @ConfigProperty(value = Constants.DEFAULT_FLUSH_CONSOLIDATION, type = Boolean.class, override = true)
    protected Boolean flushConsolidation = false;
    /**
     * Whether to enable batch decoding, default is true.
     */
    @ConfigProperty(value = Constants.DEFAULT_IS_BATCH_DECODER, type = Boolean.class, override = true)
    protected Boolean batchDecoder = true;
    /**
     * Perform flush after a certain number of writes for high throughput.
     */
    @ConfigProperty(value = Constants.DEFAULT_EXPLICIT_FLUSH_AFTER_FLUSHES, type = Integer.class, override = true)
    protected int explicitFlushAfterFlushes;
    /**
     * Enable reuse_port option.
     */
    @ConfigProperty(value = Constants.DEFAULT_REUSE_PORT, type = Boolean.class, override = true)
    protected Boolean reusePort;
    /**
     * Extension configuration.
     */
    @ConfigProperty
    protected Map<String, Object> extMap = Maps.newHashMap();

    /**
     * Check field modification privilege before setting property values, to be implemented by subclasses.
     */
    protected void checkFiledModifyPrivilege() {
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        checkFiledModifyPrivilege();
        this.protocol = protocol;
    }

    public String getSerialization() {
        return serialization;
    }

    public void setSerialization(String serialization) {
        checkFiledModifyPrivilege();
        this.serialization = serialization;
    }

    public String getCompressor() {
        return compressor;
    }

    public void setCompressor(String compressor) {
        checkFiledModifyPrivilege();
        this.compressor = compressor;
    }

    public int getCompressMinBytes() {
        return compressMinBytes;
    }

    public void setCompressMinBytes(int compressMinBytes) {
        checkFiledModifyPrivilege();
        this.compressMinBytes = compressMinBytes;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        checkFiledModifyPrivilege();
        this.sign = sign;
    }

    public Boolean getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(Boolean keepAlive) {
        checkFiledModifyPrivilege();
        this.keepAlive = keepAlive;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        checkFiledModifyPrivilege();
        this.charset = charset;
    }

    public String getTransporter() {
        return transporter;
    }

    public void setTransporter(String transporter) {
        checkFiledModifyPrivilege();
        this.transporter = transporter;
    }

    public int getMaxConns() {
        return maxConns;
    }

    public void setMaxConns(int maxConns) {
        checkFiledModifyPrivilege();
        this.maxConns = maxConns;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        checkFiledModifyPrivilege();
        this.backlog = backlog;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        checkFiledModifyPrivilege();
        this.network = network;
    }

    public int getReceiveBuffer() {
        return receiveBuffer;
    }

    public void setReceiveBuffer(int receiveBuffer) {
        checkFiledModifyPrivilege();
        this.receiveBuffer = receiveBuffer;
    }

    public int getSendBuffer() {
        return sendBuffer;
    }

    public void setSendBuffer(int sendBuffer) {
        checkFiledModifyPrivilege();
        this.sendBuffer = sendBuffer;
    }

    public int getPayload() {
        return payload;
    }

    public void setPayload(int payload) {
        checkFiledModifyPrivilege();
        this.payload = payload;
    }

    public Integer getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(Integer idleTimeout) {
        checkFiledModifyPrivilege();
        this.idleTimeout = idleTimeout;
    }

    public Boolean getLazyinit() {
        return lazyinit;
    }

    public void setLazyinit(Boolean lazyinit) {
        checkFiledModifyPrivilege();
        this.lazyinit = lazyinit;
    }

    public int getConnsPerAddr() {
        return connsPerAddr;
    }

    public void setConnsPerAddr(int connsPerAddr) {
        checkFiledModifyPrivilege();
        this.connsPerAddr = connsPerAddr;
    }

    public int getConnTimeout() {
        return connTimeout;
    }

    public void setConnTimeout(int connTimeout) {
        checkFiledModifyPrivilege();
        this.connTimeout = connTimeout;
    }

    public String getIoMode() {
        return ioMode;
    }

    public void setIoMode(String ioMode) {
        checkFiledModifyPrivilege();
        this.ioMode = ioMode;
    }

    public Boolean getIoThreadGroupShare() {
        return ioThreadGroupShare;
    }

    public void setIoThreadGroupShare(Boolean ioThreadGroupShare) {
        checkFiledModifyPrivilege();
        this.ioThreadGroupShare = ioThreadGroupShare;
    }

    public int getIoThreads() {
        return ioThreads;
    }

    public void setIoThreads(int ioThreads) {
        checkFiledModifyPrivilege();
        this.ioThreads = ioThreads;
    }


    public Boolean getFlushConsolidation() {
        return flushConsolidation;
    }

    public void setFlushConsolidation(Boolean flushConsolidation) {
        checkFiledModifyPrivilege();
        this.flushConsolidation = flushConsolidation;
    }

    public Boolean getBatchDecoder() {
        return batchDecoder;
    }

    public void setBatchDecoder(Boolean batchDecoder) {
        checkFiledModifyPrivilege();
        this.batchDecoder = batchDecoder;
    }

    public int getExplicitFlushAfterFlushes() {
        return explicitFlushAfterFlushes;
    }

    public void setExplicitFlushAfterFlushes(int explicitFlushAfterFlushes) {
        checkFiledModifyPrivilege();
        this.explicitFlushAfterFlushes = explicitFlushAfterFlushes;
    }

    public Boolean getReusePort() {
        return reusePort;
    }

    public void setReusePort(Boolean reusePort) {
        checkFiledModifyPrivilege();
        this.reusePort = reusePort;
    }

    public int getBossThreads() {
        return bossThreads;
    }

    public void setBossThreads(int bossThreads) {
        checkFiledModifyPrivilege();
        this.bossThreads = bossThreads;
    }

    public Map<String, Object> getExtMap() {
        return extMap;
    }

    public void setExtMap(Map<String, Object> extMap) {
        checkFiledModifyPrivilege();
        this.extMap = extMap;
    }

}