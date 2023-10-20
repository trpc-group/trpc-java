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

package com.tencent.trpc.support.util;

import cn.hutool.core.convert.Convert;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.model.HealthService;
import com.google.common.collect.Lists;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.support.constant.ConsulConstant;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.tencent.trpc.support.constant.ConsulConstant.URL_META_KEY;
import static com.tencent.trpc.support.constant.ConsulConstant.SERVICE_TAG;
import static com.tencent.trpc.support.constant.ConsulConstant.TTL_ENABLED;
import static com.tencent.trpc.support.constant.ConsulConstant.DEREGISTER_AFTER;
import static com.tencent.trpc.support.constant.ConsulConstant.DEFAULT_DEREGISTER_TIME;
import static com.tencent.trpc.support.constant.ConsulConstant.HEALTH_CHECK_URL;
import static com.tencent.trpc.support.constant.ConsulConstant.SCHEME;
import static com.tencent.trpc.support.constant.ConsulConstant.DEFAULT_SCHEME;
import static com.tencent.trpc.support.constant.ConsulConstant.HEALTH_PORT;
import static com.tencent.trpc.support.constant.ConsulConstant.DEFAULT_HEALTH_PORT;
import static com.tencent.trpc.support.constant.ConsulConstant.HEALTH_CHECK_PATH;
import static com.tencent.trpc.support.constant.ConsulConstant.DEFAULT_HEALTH_CHECK_PATH;
import static com.tencent.trpc.support.constant.ConsulConstant.HEALTH_CHECK_INTERVAL;
import static com.tencent.trpc.support.constant.ConsulConstant.DEFAULT_HEALTH_TIME;
import static com.tencent.trpc.support.constant.ConsulConstant.HEALTH_CHECK_TIMEOUT;
import static com.tencent.trpc.support.constant.ConsulConstant.INSTANCE_ID_SEPARATOR;
import static com.tencent.trpc.support.constant.ConsulConstant.WATCH_TIMEOUT;
import static com.tencent.trpc.support.constant.ConsulConstant.DEFAULT_WATCH_TIMEOUT;


/**
 * Consul builds API request parameter utility class
 */
public class ConsulServiceUtils {


    /**
     * Get the built Consul NewService
     *
     * @param registerInfo Registration information
     * @return NewService
     */
    public static NewService getBuildService(RegisterInfo registerInfo,
            long consulServerTtlCheckTimeInterval, Map<String, Object> extMap) {
        NewService service = new NewService();
        service.setAddress(registerInfo.getHost());
        service.setPort(registerInfo.getPort());
        service.setId(getInstanceId(registerInfo));
        service.setName(registerInfo.getServiceName());
        service.setCheck(getBuildCheck(registerInfo, consulServerTtlCheckTimeInterval, extMap));
        service.setTags(getBuildTags(registerInfo));
        service.setMeta(Collections.singletonMap(URL_META_KEY, RegisterInfo.encode(registerInfo)));
        return service;
    }

    /**
     * Get the built Consul tag
     *
     * @param registerInfo Registration information
     * @return tags
     */
    private static List<String> getBuildTags(RegisterInfo registerInfo) {
        List<String> tags = Lists.newArrayList();
        if (StringUtils.isNotEmpty(registerInfo.getParameter(ConsulConstant.TAG))) {
            tags.addAll(Arrays.stream(registerInfo.getParameter(ConsulConstant.TAG).split(","))
                    .collect(Collectors.toList()));
        }
        tags.add(SERVICE_TAG);
        return tags;
    }


    /**
     * Get the built Consul check
     *
     * @param registerInfo Registration information
     * @return NewService.Check
     */
    private static NewService.Check getBuildCheck(RegisterInfo registerInfo, long consulServerTtlCheckTimeInterval,
            Map<String, Object> extMap) {
        NewService.Check check = new NewService.Check();
        // If heartbeat detection is enabled, no TTL health check is performed on the Consul server.
        boolean ttlEnabled = MapUtils.getBooleanValue(registerInfo.getParameters(), TTL_ENABLED, Boolean.TRUE);
        if (ttlEnabled) {
            check.setDeregisterCriticalServiceAfter(registerInfo
                    .getParameter(DEREGISTER_AFTER, DEFAULT_DEREGISTER_TIME));
            check.setTtl((consulServerTtlCheckTimeInterval / 1000) + "s");
            return check;
        }

        getBuildConsulHealthCheck(registerInfo, check, extMap);
        return check;
    }

    /**
     * Get the built Consul active heartbeat health check check
     *
     * @param registerInfo Registration information
     * @param check check object
     * @param extMap config under the plugin
     */
    private static void getBuildConsulHealthCheck(RegisterInfo registerInfo, NewService.Check check,
            Map<String, Object> extMap) {
        // Config for the service under the plugin.
        Map<String, Object> pluginInServiceConfigMap = registerInfo.getParameters();

        String healthCheckUrl = Convert.toStr(
                getKeyForPluginsInServiceConfig(extMap, pluginInServiceConfigMap, HEALTH_CHECK_URL), StringUtils.EMPTY);
        if (StringUtils.isNotEmpty(healthCheckUrl)) {
            check.setHttp(healthCheckUrl);
        } else {
            check.setHttp(String.format("%s://%s:%s%s",
                    Convert.toStr(
                            getKeyForPluginsInServiceConfig(extMap, pluginInServiceConfigMap, SCHEME),
                            DEFAULT_SCHEME),
                    registerInfo.getHost(),
                    Convert.toStr(
                            getKeyForPluginsInServiceConfig(extMap, pluginInServiceConfigMap, HEALTH_PORT),
                            DEFAULT_HEALTH_PORT),
                    Convert.toStr(
                            getKeyForPluginsInServiceConfig(extMap, pluginInServiceConfigMap, HEALTH_CHECK_PATH),
                            DEFAULT_HEALTH_CHECK_PATH)));
        }

        String healthCheckInterval = Convert.toStr(
                getKeyForPluginsInServiceConfig(extMap, pluginInServiceConfigMap, HEALTH_CHECK_INTERVAL),
                DEFAULT_HEALTH_TIME);
        String healthCheckTimeout = Convert.toStr(
                getKeyForPluginsInServiceConfig(extMap, pluginInServiceConfigMap, HEALTH_CHECK_TIMEOUT)
                , DEFAULT_HEALTH_TIME);
        check.setInterval(healthCheckInterval);
        check.setTimeout(healthCheckTimeout);
    }


    /**
     * Get the configuration of the specified key according to the priority. The configuration under the plugin's
     * service overrides the configuration under the plugin.
     *
     * @param pluginConfigMap Configuration under the plugin
     * @param pluginInServiceConfigMap Configuration under the plugin's service
     * @param key Key to be queried
     * @return The final query result
     */
    public static Object getKeyForPluginsInServiceConfig(Map<String, Object> pluginConfigMap,
            Map<String, Object> pluginInServiceConfigMap, String key) {
        if (null == pluginInServiceConfigMap) {
            return pluginConfigMap.get(key);
        }
        return null != pluginInServiceConfigMap.get(key)
                ? pluginInServiceConfigMap.get(key) : pluginConfigMap.get(key);
    }

    /**
     * Get the instanceId
     *
     * @param registerInfo Registration information
     * @return instanceId
     */
    public static String getInstanceId(RegisterInfo registerInfo) {
        return String.join(INSTANCE_ID_SEPARATOR, registerInfo.getServiceName(), registerInfo.getHost(),
                String.valueOf(registerInfo.getPort()));
    }

    /**
     * Convert the list of healthy services obtained into registration information
     *
     * @param services List of healthy services
     * @param consumerRegisterInfo Registration information of the consumer
     * @return RegisterInfos The final converted list of registrars
     */
    public static List<RegisterInfo> convert(List<HealthService> services, RegisterInfo consumerRegisterInfo) {
        if (CollectionUtils.isEmpty(services)) {
            return Lists.newArrayList(consumerRegisterInfo.clone());
        }
        return services.stream()
                .map(HealthService::getService)
                .filter(Objects::nonNull)
                .map(HealthService.Service::getMeta)
                .filter(m -> m != null && m.containsKey(URL_META_KEY))
                .map(m -> m.get(URL_META_KEY))
                .map(RegisterInfo::decode)
                .filter(deRegisterInfo -> deRegisterInfo.getServiceName().equals(consumerRegisterInfo.getServiceName()))
                .collect(Collectors.toList());
    }


    /**
     * Get the watch timeout
     *
     * @param registerInfo Registration information
     * @return Watch timeout
     */
    public static int getWatchTimeout(RegisterInfo registerInfo) {
        return registerInfo.getParameter(WATCH_TIMEOUT, DEFAULT_WATCH_TIMEOUT) / 1000;
    }

}
