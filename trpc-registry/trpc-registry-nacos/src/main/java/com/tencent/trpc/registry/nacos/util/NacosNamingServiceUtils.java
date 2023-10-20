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

package com.tencent.trpc.registry.nacos.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.google.common.collect.Lists;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.registry.nacos.config.NacosRegistryCenterConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static com.tencent.trpc.registry.nacos.constant.NacosConstant.URL_META_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_ADDRESSED_KEY;
import static com.tencent.trpc.registry.nacos.util.StringConstantFieldValuePredicateUtils.of;
import static com.tencent.trpc.registry.nacos.constant.NacosConstant.DEFAULT_REGISTRY_CENTER_ADDRESSED_KEY;

/**
 * Nacos client utility class
 */
public class NacosNamingServiceUtils {

    private static final Logger logger = LoggerFactory.getLogger(NacosNamingServiceUtils.class);

    /**
     * Convert the obtained list of healthy services into registry information
     *
     * @param services List of healthy services
     * @param consumerRegisterInfo Registry information of the consumer
     * @return RegisterInfos Final converted list of registries
     */
    public static List<RegisterInfo> convert(List<Instance> services, RegisterInfo consumerRegisterInfo) {
        if (CollectionUtils.isEmpty(services)) {
            return Lists.newArrayList(consumerRegisterInfo.clone());
        }
        return services.stream()
                .filter(Objects::nonNull)
                .map(Instance::getMetadata)
                .filter(m -> m != null && m.containsKey(URL_META_KEY))
                .map(m -> m.get(URL_META_KEY))
                .map(RegisterInfo::decode)
                .filter(deRegisterInfo -> deRegisterInfo.getServiceName().equals(consumerRegisterInfo.getServiceName()))
                .collect(Collectors.toList());
    }
    
    /**
     * Create a NamingService object for Nacos
     *
     * @param config RegistryCenterConfig connection config
     * @return NamingService
     */
    public static NamingService createNamingService(NacosRegistryCenterConfig config) {
        Properties nacosProperties = buildNacosProperties(config);
        NamingService namingService;
        try {
            namingService = NacosFactory.createNamingService(nacosProperties);
        } catch (NacosException e) {
            logger.error("nacos createNamingService error" + e.getErrMsg(), e);
            throw new IllegalStateException(e);
        }
        return namingService;
    }

    /**
     * Build NacosProperties
     *
     * @param config Nacos configuration information
     * @return NacosProperties
     */
    private static Properties buildNacosProperties(NacosRegistryCenterConfig config) {
        String addresses = MapUtils.getString(config.getParameters(),
                REGISTRY_CENTER_ADDRESSED_KEY, DEFAULT_REGISTRY_CENTER_ADDRESSED_KEY);
        Properties properties = new Properties();
        properties.put(SERVER_ADDR, addresses);
        Map<String, String> parameters = config.getParameters(of(PropertyKeyConst.class));
        properties.putAll(parameters);
        setNamingLoadCacheStartParam(config, properties);
        return properties;
    }

    /**
     * Set NAMING_LOAD_CACHE_AT_START parameter
     *
     * @param config Nacos configuration information
     * @param properties Properties
     */
    private static void setNamingLoadCacheStartParam(NacosRegistryCenterConfig config,
            Properties properties) {
        String propertyValue = MapUtils.getString(config.getParameters(),
                PropertyKeyConst.NAMING_LOAD_CACHE_AT_START, "true");
        properties.setProperty(PropertyKeyConst.NAMING_LOAD_CACHE_AT_START, propertyValue);
    }
}
