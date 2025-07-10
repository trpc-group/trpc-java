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

package com.tencent.trpc.selector.polaris.common;

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.weight.WeightType;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.consumer.CircuitBreakerConfigImpl;
import com.tencent.polaris.factory.config.consumer.ConsumerConfigImpl;
import com.tencent.polaris.factory.config.global.APIConfigImpl;
import com.tencent.polaris.factory.config.global.GlobalConfigImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.polaris.common.PolarisTrans;
import com.tencent.trpc.selector.polaris.common.pojo.PolarisServiceInstances;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PolarisCommon {

    public static void report(ConsumerAPI polarisAPI, ServiceInstance serviceInstance, int code,
            long costMs) {
        ServiceCallResult callRes = new ServiceCallResult();
        callRes.setDelay(costMs);
        callRes.setInstance(PolarisTrans.toPolarisInstance(serviceInstance));
        callRes.setRetCode(code);
        callRes.setRetStatus(
                code == ErrorCode.TRPC_INVOKE_SUCCESS ? RetStatus.RetSuccess : RetStatus.RetFail);
        try {
            polarisAPI.updateServiceCallResult(callRes);
        } catch (PolarisException e) {
            throw TRpcException.trans(e);
        }
    }

    /**
     * Convert the instance object of TRPC to the object of Polaris
     *
     * @param instances trpc instances
     */
    public static ServiceInstances toServiceInstances(List<ServiceInstance> instances) {
        List<Instance> polarisInsList =
                instances.stream().map(PolarisTrans::toPolarisInstance)
                        .collect(Collectors.toList());

        Instance anyInstance = polarisInsList.get(0);

        return createServiceInstances(anyInstance.getNamespace(), anyInstance.getService(),
                polarisInsList);
    }

    /**
     * Convert the Polaris Instance object collection into a ServiceInstances object
     *
     * @param instanceList InstanceList
     * @return ServiceInstances object
     */
    public static ServiceInstances createServiceInstances(List<Instance> instanceList) {
        Instance one = instanceList.get(0);
        return createServiceInstances(one.getNamespace(), one.getService(), instanceList);
    }

    /**
     * Create a Polaris instance based on the trpc instance
     *
     * @param namespace namespace
     * @param serviceName serviceName
     * @param instanceList instanceList
     */
    public static ServiceInstances createServiceInstances(String namespace, String serviceName,
            List<Instance> instanceList) {
        PolarisServiceInstances serviceInstances = new PolarisServiceInstances(instanceList);
        serviceInstances.setInitialized(true);
        serviceInstances.setInstances(instanceList);
        serviceInstances.setMetadata(new HashMap<>());
        serviceInstances.setNamespace(namespace);
        serviceInstances.setService(serviceName);
        serviceInstances.setWeightType(WeightType.DYNAMIC);
        serviceInstances.setRevision("");
        int totalWeight = instanceList.stream().mapToInt(Instance::getWeight).sum();
        serviceInstances.setTotalWeight(totalWeight);
        return serviceInstances;
    }


    /**
     * Generate Polaris configuration based on plugin configuration
     *
     * @param extMap Plugin Configuration
     * @return Polaris configuration, complete configuration ensures that all configuration items have default values
     */
    public static ConfigurationImpl genPolarisConfiguration(Map<String, Object> extMap) {
        ConfigurationImpl configuration;
        if (extMap == null) {
            configuration = new ConfigurationImpl();
            configuration.setDefault();
        } else {
            configuration = JsonUtils.convertValue(extMap, ConfigurationImpl.class);
            configuration.setDefault();
            GlobalConfigImpl globalConfig = configuration.getGlobal();
            APIConfigImpl apiConfig = globalConfig.getAPI();
            ServerConnectorConfigImpl serverConnectorConfig = globalConfig.getServerConnector();

            PolarisTrans.updateApiConfig(apiConfig, extMap);
            PolarisTrans.updateServerConnectorConfig(serverConnectorConfig, extMap);

            ConsumerConfigImpl consumerConfig = configuration.getConsumer();
            PolarisTrans.updateConsumerConfig(consumerConfig, extMap);
        }
        return configuration;
    }

    /**
     * Generate Polaris configuration based on plugin configuration
     *
     * @param extMap plugin config
     * @return Polaris config
     */
    public static CircuitBreakerConfigImpl genCircuitBreakerConfiguration(ConfigurationImpl configuration,
            Map<String, Object> extMap) {
        CircuitBreakerConfigImpl defaultConfig = configuration.getConsumer().getCircuitBreaker();

        if (MapUtils.isEmpty(extMap)) {
            return defaultConfig;
        }
        CircuitBreakerConfigImpl circuitBreakerConfig = JsonUtils.convertValue(extMap, CircuitBreakerConfigImpl.class);
        circuitBreakerConfig.setDefault(defaultConfig);
        return circuitBreakerConfig;
    }

}
