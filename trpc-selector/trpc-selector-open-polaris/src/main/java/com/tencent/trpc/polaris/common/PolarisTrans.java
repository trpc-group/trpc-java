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

package com.tencent.trpc.polaris.common;

import static com.tencent.trpc.core.exception.ErrorCode.TRPC_CLIENT_ROUTER_ERR;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.plugin.weight.WeightType;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.factory.config.consumer.ConsumerConfigImpl;
import com.tencent.polaris.factory.config.consumer.LocalCacheConfigImpl;
import com.tencent.polaris.factory.config.global.APIConfigImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.selector.polaris.common.pojo.PolarisServiceInstances;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Polaris and trpc Transfer utils class
 */
public class PolarisTrans {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolarisTrans.class);

    /**
     * Convert TRPC ServiceInstance from Polaris Response and Instance
     *
     * @param response nameService returned Response
     * @param polarisInstance Polaris service instance
     */
    public static ServiceInstance toServiceInstance(InstancesResponse response,
            Instance polarisInstance) {
        String containerName = getContainerName(polarisInstance);
        String setName = getSetName(polarisInstance);

        ServiceInstance serviceInstance = new ServiceInstance(polarisInstance.getHost(),
                polarisInstance.getPort(), polarisInstance.isHealthy());
        serviceInstance.getParameters().putAll(polarisInstance.getMetadata());
        serviceInstance.getParameters().put(PolarisConstant.POLARIS_RESPONSE, response);
        serviceInstance.getParameters().put(PolarisConstant.POLARIS_INSTANCE, polarisInstance);
        serviceInstance.getParameters().put(Constants.CONTAINER_NAME, containerName);
        serviceInstance.getParameters().put(Constants.SET_DIVISION, setName);
        return serviceInstance;
    }

    /**
     * Get instance set name by polarisInstance
     *
     * @param polarisInstance polarisInstance
     */
    public static String getSetName(Instance polarisInstance) {
        String setName = null;
        if (polarisInstance.getMetadata() != null) {
            if (PolarisConstant.INTERNAL_ENABLE_SET_Y
                    .equals(polarisInstance.getMetadata().get(PolarisConstant.INTERNAL_ENABLE_SET_KEY))
                    && polarisInstance.getMetadata().containsKey(PolarisConstant.INTERNAL_SET_NAME_KEY)) {
                setName = polarisInstance.getMetadata().get(PolarisConstant.INTERNAL_SET_NAME_KEY);
            }
        }
        return setName == null ? PolarisConstant.EMPTY_STRING : setName;
    }

    /**
     * Get container name by polarisInstance
     *
     * @param polarisInstance polarisInstance
     */
    public static String getContainerName(Instance polarisInstance) {
        String containerName = null;
        if (polarisInstance.getMetadata() != null) {
            if (polarisInstance.getMetadata().containsKey(PolarisConstant.CONTAINER_NAME)) {
                containerName = polarisInstance.getMetadata().get(PolarisConstant.CONTAINER_NAME);
            } else {
                containerName = polarisInstance.getMetadata().get(PolarisConstant.CONTAINER_NAME_TAF);
            }
        }
        return containerName == null ? PolarisConstant.EMPTY_STRING : containerName;
    }

    public static Instance toPolarisInstance(ServiceInstance serviceInstance) {
        return (Instance) serviceInstance.getObject(PolarisConstant.POLARIS_INSTANCE);
    }

    /**
     * Convert trpc ServiceInstance to the Polaris ServiceInstances
     */
    public static ServiceInstances toPolarisInstance(List<ServiceInstance> serviceInstances) {
        if (serviceInstances != null && serviceInstances.size() > 0) {
            ServiceInstance serviceInstance = serviceInstances.get(0);
            InstancesResponse response = (InstancesResponse) serviceInstance
                    .getObject(PolarisConstant.POLARIS_RESPONSE);
            Instance instance = (Instance) serviceInstance.getObject(PolarisConstant.POLARIS_INSTANCE);
            PolarisServiceInstances polarisServiceInstances = new PolarisServiceInstances(Lists.newArrayList(instance));

            if (response != null) {
                polarisServiceInstances.setWeightType(WeightType.DYNAMIC);
                polarisServiceInstances.setTotalWeight(response.getTotalWeight());
                polarisServiceInstances.setInitialized(true);
                polarisServiceInstances.setNamespace(response.getNamespace());
                polarisServiceInstances.setService(response.getService());
                List<Instance> polarisInstances = Lists.newArrayList();
                serviceInstances.forEach(trpcServiceInstance -> polarisInstances
                        .add((Instance) trpcServiceInstance.getObject(PolarisConstant.POLARIS_INSTANCE)));
                polarisServiceInstances.setInstances(polarisInstances);
            }

            if (instance != null) {
                polarisServiceInstances.setRevision(instance.getRevision());
                polarisServiceInstances.setMetadata(instance.getMetadata());
            }
            return polarisServiceInstances;
        }
        return null;
    }


    public static String getPolarisInstanceId(ServiceInstance serviceInstance) {
        return serviceInstance.getParameter(PolarisConstant.POLARIS_ID);
    }

    /**
     * Origin map to polaris map
     * The metadata here will be routed through metadata,
     * so only the parameters in the user-defined 'metadata' will be placed
     *
     * @param originMap origin map
     * @return return polaris map
     */
    public static Map<String, String> trans2PolarisMetadata(Map<String, Object> originMap) {
        Object metadataObj = originMap.get(Constants.METADATA);
        if (metadataObj == null || !(metadataObj instanceof Map)) {
            return Maps.newHashMap();
        }
        Map<String, Object> metadata = (Map) metadataObj;
        return metadata.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, val -> {
            Object value = val.getValue();
            String strValue;
            try {
                strValue = ((value instanceof String) ? (String) value : JsonUtils.toJson(value));
            } catch (Exception e) {
                LOGGER.error("to json failed. value type:{}", value);
                strValue = String.valueOf(value);
            }
            return strValue;
        }));
    }

    /**
     * Get polaris ServiceInfo
     * Obtain the main call metadata to support request matching rules for rule routing
     *
     * @param pluginProperties plugin config
     * @param serviceId service id
     * @param request request
     * @return return ServiceMetadata
     */
    public static ServiceInfo getPolarisServiceInfo(Map<String, Object> pluginProperties,
            ServiceId serviceId, Request request) {
        String callerServiceName = serviceId.getCallerServiceName();
        String callerNamespace = serviceId.getCallerNamespace();
        String callerEnvName = serviceId.getCallerEnvName();

        // Obtain the main caller service information for user-defined
        // settings (Priority: Custom Settings>Profile) from req attachments
        Map<String, Object> reqAttachments = new HashMap<>();
        if (request != null) {
            reqAttachments = request.getAttachments();
            if (RpcContextUtils.getAttachValue(request, PolarisConstant.SELECTOR_SERVICE_NAME) != null) {
                callerServiceName = RpcContextUtils.getAttachValue(request, PolarisConstant.SELECTOR_SERVICE_NAME);
            }
            if (RpcContextUtils.getAttachValue(request, PolarisConstant.SELECTOR_NAMESPACE) != null) {
                callerNamespace = RpcContextUtils.getAttachValue(request, PolarisConstant.SELECTOR_NAMESPACE);
            }
            if (RpcContextUtils.getAttachValue(request, PolarisConstant.SELECTOR_ENV_NAME) != null) {
                callerEnvName = RpcContextUtils.getAttachValue(request, PolarisConstant.SELECTOR_ENV_NAME);
            }
        }

        // Pick meta data
        boolean enableTransMeta = MapUtils.getBoolean(pluginProperties,
                PolarisConstant.POLARIS_ENABLE_TRANS_META, Boolean.FALSE);
        Map<String, String> metadata = pickMetaData(reqAttachments, enableTransMeta, callerEnvName);
        boolean disableCallerServiceName = MapUtils.getBoolean(pluginProperties,
                PolarisConstant.POLARIS_DISABLE_CALLER_SERVICE_NAME, Boolean.FALSE);
        return generateServiceInfo(metadata, callerServiceName, callerNamespace, disableCallerServiceName);
    }

    /**
     * Attachments to string map
     * The calling metadata only puts data with the prefix "service-meta-", consistent with 'go'
     *
     * @param enableTransMeta Whether to transmit meta
     * @param reqAttachments request attachments
     * @param callerEnvName calling env info
     * @return metadata map
     */
    public static Map<String, String> pickMetaData(Map<String, Object> reqAttachments, boolean enableTransMeta,
            String callerEnvName) {
        // metadata
        Map<String, String> metadata = new HashMap<>();
        // put env in metadata
        if (StringUtils.isNotEmpty(callerEnvName)) {
            metadata.put(PolarisConstant.POLARIS_PB_ENV, callerEnvName);
        }
        // Resolve the issue of request transparency fields being unable to be passed to Polaris for meta matching
        // Agree on the transparent field of the 'selector-meta-' prefix,
        // remove the prefix and fill in the meta for matching with the North Star, consistent with go
        if (!enableTransMeta) {
            return metadata;
        }
        Map<String, String> metadataTemp = reqAttachments.entrySet().stream()
                .filter(map -> map.getKey().startsWith(PolarisConstant.SELECTOR_META_PREFIX))
                .collect(Collectors.toMap(
                        kv -> {
                            String key = kv.getKey().substring(PolarisConstant.SELECTOR_META_PREFIX.length());
                            return key;
                        }, kv -> {
                            Object value = kv.getValue();
                            if (value instanceof byte[]) {
                                return new String((byte[]) value, StandardCharsets.UTF_8);
                            } else {
                                return value.toString();
                            }
                        }));
        metadata.putAll(metadataTemp);
        return metadata;
    }

    public static ServiceInfo generateServiceInfo(Map<String, String> metadata, String callerServiceName,
            String callerNamespace, boolean disableCallerServiceName) {
        // ServiceInfo ,Request matching rules for supporting rule router
        ServiceInfo serviceInfo = new ServiceInfo();
        // If the setting is enabled, the service name of SourceServiceInfo will not be filled in
        // Scenarios where compatible calling services are not registered on Polaris
        if (StringUtils.isNotEmpty(callerServiceName) && !disableCallerServiceName) {
            serviceInfo.setService(callerServiceName);
        }
        // Set caller namespace
        if (StringUtils.isNotEmpty(callerNamespace)) {
            serviceInfo.setNamespace(callerNamespace);
        }
        // Set caller metadata
        if (metadata.size() > 0) {
            serviceInfo.setMetadata(metadata);
        }
        return serviceInfo;
    }

    public static void updateApiConfig(APIConfigImpl apiConfig,
            Map<String, Object> extMap) {
        if (extMap != null && extMap.size() > 0) {
            if (extMap.containsKey(PolarisConstant.POLARIS_API_MAXRETRYTIMES_KEY)) {
                apiConfig.setMaxRetryTimes((int) extMap.get(PolarisConstant.POLARIS_API_MAXRETRYTIMES_KEY));
            }
            if (extMap.containsKey(PolarisConstant.POLARIS_API_BINDIF_KEY)) {
                apiConfig.setBindIf((String) extMap.get(PolarisConstant.POLARIS_API_BINDIF_KEY));
            }
        }
    }

    /**
     * Update server side connection configuration
     *
     * @param serverConnectorConfig service origin config
     * @param extMap update config info
     */
    public static void updateServerConnectorConfig(
            ServerConnectorConfigImpl serverConnectorConfig,
            Map<String, Object> extMap) {
        if (extMap != null && extMap.size() > 0) {
            if (extMap.containsKey(PolarisConstant.POLARIS_ADDRESSES_KEY)) {
                serverConnectorConfig
                        .setAddresses(Arrays.asList(
                                ((String) extMap.get(PolarisConstant.POLARIS_ADDRESSES_KEY)).split(",")));
            }
            if (extMap.containsKey(PolarisConstant.POLARIS_PROTOCOL_KEY)) {
                serverConnectorConfig.setProtocol((String) extMap.get(PolarisConstant.POLARIS_PROTOCOL_KEY));
            }
        }
    }


    /**
     * Update Polaris ConsumerConfig based on some configurations supported by trpc
     */
    public static void updateConsumerConfig(ConsumerConfigImpl consumerConfig,
            Map<String, Object> extMap) {
        LocalCacheConfigImpl localCacheConfig = consumerConfig.getLocalCache();
        if (extMap != null && extMap.containsKey(PolarisConstant.POLARIS_LOCALCACHE) && (extMap
                .get(PolarisConstant.POLARIS_LOCALCACHE) instanceof Map)) {
            Map<String, Object> localCacheMap = (Map<String, Object>) extMap.get(PolarisConstant.POLARIS_LOCALCACHE);
            if (localCacheMap == null) {
                return;
            }
            if (localCacheMap.containsKey(PolarisConstant.POLARIS_LOCALCACHE_PERSISTDIR)) {
                localCacheConfig.setPersistDir(
                        (String) localCacheMap.get(PolarisConstant.POLARIS_LOCALCACHE_PERSISTDIR));
            }
            if (localCacheMap.containsKey(PolarisConstant.POLARIS_LOCALCACHE_TYPE)) {
                localCacheConfig.setType((String) localCacheMap.get(PolarisConstant.POLARIS_LOCALCACHE_TYPE));
            }
            if (localCacheMap.containsKey(PolarisConstant.POLARIS_LOCALCACHE_PERSISTMAXREADRETRY)) {
                localCacheConfig.setPersistMaxReadRetry(
                        (Integer) localCacheMap.get(PolarisConstant.POLARIS_LOCALCACHE_PERSISTMAXREADRETRY));
            }
            if (localCacheMap.containsKey(PolarisConstant.POLARIS_LOCALCACHE_PERSISTMAXWRITERETRY)) {
                localCacheConfig.setPersistMaxWriteRetry(
                        (Integer) localCacheMap.get(PolarisConstant.POLARIS_LOCALCACHE_PERSISTMAXWRITERETRY));
            }
        }
    }

    /**
     * Transfer trpc ServiceInstance by polarisInstances
     */
    public static List<ServiceInstance> transfer2ServiceInstance(List<Instance> polarisInstances,
            InstancesResponse response) {
        if (CollectionUtils.isEmpty(polarisInstances)) {
            return Collections.emptyList();
        }

        return polarisInstances.stream()
                .map(polarisInstance -> PolarisTrans.toServiceInstance(response, polarisInstance))
                .collect(Collectors.toList());
    }

    /**
     * Parse polaris Router Result
     *
     * @param routeResult Router Result
     * @param response polaris response
     * @return trpc ServiceInstance
     */
    public static List<ServiceInstance> parseRouterResult(RouteResult routeResult,
            InstancesResponse response) {
        if (routeResult != null && CollectionUtils.isNotEmpty(routeResult.getInstances())) {
            List<Instance> polarisInstances = routeResult.getInstances();
            return PolarisTrans.transfer2ServiceInstance(polarisInstances, response);
        }
        throw TRpcException.newFrameException(TRPC_CLIENT_ROUTER_ERR, "can not find any instances.");
    }

}
