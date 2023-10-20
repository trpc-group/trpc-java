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

package com.tencent.trpc.spring.context.configuration.schema;

import com.google.common.collect.Maps;
import java.util.Map;

/**
 * Base configs for tRPC servers and clients
 *
 * @see com.tencent.trpc.core.common.config.BaseProtocolConfig
 */
public abstract class AbstractProtocolSchema {

    /**
     * Transport protocol
     */
    private String protocol;

    /**
     * Protocol type, standard/stream
     */
    private String protocolType;

    /**
     * Serialization plugin name
     */
    private String serialization;

    /**
     * Compress plugin name
     */
    private String compressor;

    /**
     * Minimum message size to enable compress
     */
    private Integer compressMinBytes;

    /**
     * Signature method
     */
    private String sign;

    /**
     * Keep-alive connection
     */
    private Boolean keepAlive;

    /**
     * Charset
     */
    private String charset;

    /**
     * Transporter type, netty/jetty
     */
    private String transporter;

    /**
     * Max connections allowed
     */
    private Integer maxConns;

    /**
     * Backlog size
     */
    private Integer backlog;

    /**
     * Network type, udp/tcp
     */
    private String network;

    /**
     * Receiving buffer size
     */
    private Integer receiveBuffer;

    /**
     * Sending buffer size
     */
    private Integer sendBuffer;

    /**
     * Payload size limit
     */
    private Integer payload;

    /**
     * Idle timeout in millis
     */
    private Integer idleTimeout;

    /**
     * Lazy-initialization
     */
    private Boolean lazyinit;

    /**
     * I/O thread sharing switch
     */
    private Boolean ioThreadGroupShare;

    /**
     * I/O thread count
     */
    private Integer ioThreads;

    /**
     * Flush consolidation switch
     */
    private Boolean flushConsolidation;

    /**
     * Batch decoding switch
     */
    private Boolean batchDecoder;

    /**
     * Perform real flush after N flushes
     */
    private Integer explicitFlushAfterFlushes;

    /**
     * Extension configs
     */
    private Map<String, Object> extMap = Maps.newHashMap();

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    public String getSerialization() {
        return serialization;
    }

    public void setSerialization(String serialization) {
        this.serialization = serialization;
    }

    public String getCompressor() {
        return compressor;
    }

    public void setCompressor(String compressor) {
        this.compressor = compressor;
    }

    public Integer getCompressMinBytes() {
        return compressMinBytes;
    }

    public void setCompressMinBytes(Integer compressMinBytes) {
        this.compressMinBytes = compressMinBytes;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public Boolean getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(Boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getTransporter() {
        return transporter;
    }

    public void setTransporter(String transporter) {
        this.transporter = transporter;
    }

    public Integer getMaxConns() {
        return maxConns;
    }

    public void setMaxConns(Integer maxConns) {
        this.maxConns = maxConns;
    }

    public Integer getBacklog() {
        return backlog;
    }

    public void setBacklog(Integer backlog) {
        this.backlog = backlog;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public Integer getReceiveBuffer() {
        return receiveBuffer;
    }

    public void setReceiveBuffer(Integer receiveBuffer) {
        this.receiveBuffer = receiveBuffer;
    }

    public Integer getSendBuffer() {
        return sendBuffer;
    }

    public void setSendBuffer(Integer sendBuffer) {
        this.sendBuffer = sendBuffer;
    }

    public Integer getPayload() {
        return payload;
    }

    public void setPayload(Integer payload) {
        this.payload = payload;
    }

    public Integer getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(Integer idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public Boolean getLazyinit() {
        return lazyinit;
    }

    public void setLazyinit(Boolean lazyinit) {
        this.lazyinit = lazyinit;
    }

    public Boolean getIoThreadGroupShare() {
        return ioThreadGroupShare;
    }

    public void setIoThreadGroupShare(Boolean ioThreadGroupShare) {
        this.ioThreadGroupShare = ioThreadGroupShare;
    }

    public Integer getIoThreads() {
        return ioThreads;
    }

    public void setIoThreads(Integer ioThreads) {
        this.ioThreads = ioThreads;
    }

    public Boolean getFlushConsolidation() {
        return flushConsolidation;
    }

    public void setFlushConsolidation(Boolean flushConsolidation) {
        this.flushConsolidation = flushConsolidation;
    }

    public Boolean getBatchDecoder() {
        return batchDecoder;
    }

    public void setBatchDecoder(Boolean batchDecoder) {
        this.batchDecoder = batchDecoder;
    }

    public Integer getExplicitFlushAfterFlushes() {
        return explicitFlushAfterFlushes;
    }

    public void setExplicitFlushAfterFlushes(Integer explicitFlushAfterFlushes) {
        this.explicitFlushAfterFlushes = explicitFlushAfterFlushes;
    }

    public Map<String, Object> getExtMap() {
        return extMap;
    }

    public void setExtMap(Map<String, Object> extMap) {
        this.extMap = extMap;
    }
}
