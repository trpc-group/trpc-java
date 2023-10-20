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

package com.tencent.trpc.admin.dto.rpc;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.trpc.admin.dto.CommonDto;
import java.util.Map;

/**
 * RPC-related statistics data
 * Corresponding query interface: /cmds/stats/rpc
 */
public class RpcStatsDto extends CommonDto {

    /**
     * Meaning of whether RPC framework-related fields must be collected
     * rpc_version: Required
     */
    @JsonProperty("rpc_version")
    private String rpcVersion;

    /**
     * Not required, the number of threads created by the RPC framework
     */
    @JsonProperty("rpc_frame_thread_count")
    private Long rpcFrameThreadCount;

    /**
     * Not required, how many services are currently in the service
     */
    @JsonProperty("rpc_service_count")
    private Integer rpcServiceCount;

    @JsonProperty("rpc_service_map")
    private Map<String, Object> rpcServiceMap;

    @JsonProperty("rpc_client_map")
    private Map<String, Object> rpcClientMap;

    public Map<String, Object> getRpcClientMap() {
        return rpcClientMap;
    }

    public void setRpcClientMap(Map<String, Object> rpcClientMap) {
        this.rpcClientMap = rpcClientMap;
    }

    public Map<String, Object> getRpcServiceMap() {
        return rpcServiceMap;
    }

    public void setRpcServiceMap(Map<String, Object> rpcServiceMap) {
        this.rpcServiceMap = rpcServiceMap;
    }

    public String getRpcVersion() {
        return rpcVersion;
    }

    public void setRpcVersion(String rpcVersion) {
        this.rpcVersion = rpcVersion;
    }

    public Long getRpcFrameThreadCount() {
        return rpcFrameThreadCount;
    }

    public void setRpcFrameThreadCount(Long rpcFrameThreadCount) {
        this.rpcFrameThreadCount = rpcFrameThreadCount;
    }

    public Integer getRpcServiceCount() {
        return rpcServiceCount;
    }

    public void setRpcServiceCount(Integer rpcServiceCount) {
        this.rpcServiceCount = rpcServiceCount;
    }

}
