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

package com.tencent.trpc.support;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.catalog.CatalogServicesRequest;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.support.proxy.ConsulClientProxy;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tencent.trpc.support.constant.ConsulConstant.SERVICE_TAG;
import static com.tencent.trpc.support.constant.ConsulConstant.ACL_TOKEN;
import static com.tencent.trpc.support.constant.ConsulConstant.CHECK_PASS_INTERVAL;
import static com.tencent.trpc.support.constant.ConsulConstant.DEFAULT_CHECK_PASS_INTERVAL;
import static com.tencent.trpc.support.util.ConsulServiceUtils.getBuildService;
import static com.tencent.trpc.support.util.ConsulServiceUtils.getInstanceId;
import static com.tencent.trpc.support.util.TtlSchedulerInstanceUtils.addTtlScheduler;
import static com.tencent.trpc.support.util.TtlSchedulerInstanceUtils.removeTtlScheduler;

/**
 * Consul instance management class, used to encapsulate operations on the Consul client
 */
public class ConsulInstanceManager {

    private ConsulClient consulClient;

    private String token;

    private final ProtocolConfig protocolConfig;

    private long consulServerTtlCheckTimeInterval;


    public ConsulInstanceManager(ProtocolConfig protocolConfig) {
        this.protocolConfig = protocolConfig;
        this.token = MapUtils.getString(protocolConfig.getExtMap(), ACL_TOKEN, null);
        this.consulServerTtlCheckTimeInterval = MapUtils.getLong(protocolConfig.getExtMap(), CHECK_PASS_INTERVAL,
                DEFAULT_CHECK_PASS_INTERVAL);

        // Get Consul client proxy object
        this.consulClient = new ConsulClientProxy(this).getProxy();
    }

    /**
     * Convert to get healthy services
     *
     * @param services List of services
     * @return {@code List<HealthService>}
     */
    public List<HealthService> getHealthServices(Map<String, List<String>> services) {
        return services.entrySet().stream()
                .filter(s -> s.getValue().contains(SERVICE_TAG))
                .map(s -> getHealthServices(s.getKey(), -1, -1).getValue())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * Get healthy service
     *
     * @param service Service
     * @param index Index
     * @param watchTimeout Watch timeout
     * @return {@code Response<List<HealthService>>}
     */
    public Response<List<HealthService>> getHealthServices(String service, long index, int watchTimeout) {
        HealthServicesRequest request = HealthServicesRequest.newBuilder().setTag(SERVICE_TAG)
                .setQueryParams(new QueryParams(watchTimeout, index)).setPassing(true).setToken(token).build();
        return consulClient.getHealthServices(service, request);
    }

    /**
     * Get all service information based on index
     *
     * @param index Index
     * @param watchTimeout Timeout
     * @return List of services
     */
    public Response<Map<String, List<String>>> getAllServices(long index, int watchTimeout) {
        CatalogServicesRequest request = CatalogServicesRequest.newBuilder()
                .setQueryParams(new QueryParams(watchTimeout, index)).setToken(token).build();
        return consulClient.getCatalogServices(request);
    }

    /**
     * Register provider information
     *
     * @param registerInfo Registration information
     */
    public void register(RegisterInfo registerInfo) {
        // Add TTL scheduling task
        addTtlScheduler(registerInfo, this);
        NewService newService = getBuildService(registerInfo, consulServerTtlCheckTimeInterval,
                this.getProtocolConfig().getExtMap());
        if (StringUtils.isEmpty(token)) {
            consulClient.agentServiceRegister(newService);
            return;
        }
        consulClient.agentServiceRegister(newService, token);
    }


    /**
     * Cancel registration of provider information
     *
     * @param registerInfo Registration instance information
     */
    public void unregister(RegisterInfo registerInfo) {
        // Remove TTL scheduling task
        removeTtlScheduler(registerInfo);
        String instanceId = getInstanceId(registerInfo);
        if (StringUtils.isEmpty(token)) {
            consulClient.agentServiceDeregister(instanceId);
            return;
        }
        consulClient.agentServiceDeregister(instanceId, token);
    }

    /**
     * Check if the Consul client is valid
     */
    public boolean isAvailable() {
        return null != consulClient.getAgentSelf();
    }

    public void agentCheckPass(String checkId) {
        consulClient.agentCheckPass(checkId);
    }

    /**
     * Consul high availability, reset client
     *
     * @param consulClient Consul client
     */
    public void resetClient(ConsulClient consulClient) {
        this.consulClient = consulClient;
    }

    /**
     * Get protocol configuration information
     *
     * @return Protocol configuration information
     */
    public ProtocolConfig getProtocolConfig() {
        return protocolConfig;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
