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

package com.tencent.trpc.registry.polaris;

import static com.tencent.trpc.core.common.Constants.ENV_NAME_KEY;
import static com.tencent.trpc.core.common.Constants.POLARIS_ENV;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.CONTAINER_NAME;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.HEARTBEAT_INTERVAL_DEFAULT;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.HEARTBEAT_INTERVAL_KEY;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.INSTANCE_ID;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.INTERNAL_ENABLE_SET_KEY;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.INTERNAL_ENABLE_SET_Y;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.INTERNAL_SET_NAME_KEY;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.NAME;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.NAMESPACE_KEY;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.PRIORITY_PARAM_KEY;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.REGISTER_SELF;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.REGISTER_SELF_DEFAULT;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.TIMEOUT_PARAM_KEY;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.TOKEN_PARAM_KEY;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.TTL_DEFAULT;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.TTL_KEY;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.WEIGHT_PARAM_KEY;

import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceHeartbeatRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.factory.api.APIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.DisposableExtension;
import com.tencent.trpc.core.extension.InitializingExtension;
import com.tencent.trpc.core.extension.PluginConfigAware;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.core.registry.spi.Registry;
import com.tencent.trpc.polaris.common.PolarisRegisterUtil;
import com.tencent.trpc.support.HeartBeatManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.collections4.MapUtils;

/**
 * A registration center plugin implemented based on Polaris registration center.
 * This class provides register and unregister methods to register and unregister services with Polaris, and also has a
 * heartbeat method to keep the connection with the Polaris server alive.
 */
public class PolarisRegistry implements Registry, PluginConfigAware, InitializingExtension, DisposableExtension {

    private static final Logger logger = LoggerFactory.getLogger(PolarisRegistry.class);
    /**
     * Polaris ProviderAPI object.
     */
    private ProviderAPI providerApi;
    /**
     * Avoid duplicate creation of heartbeat requests.
     */
    private Map<RegisterInfo, InstanceHeartbeatRequest> heartbeatRequestCache = new HashMap<>();
    /**
     * Maintain a relationship between Polaris service name and instanceId in mem after successful registration.
     */
    private Map<String, String> polarisIdRelation = new HashMap<>();
    /**
     * Configuration information for this plugin.
     */
    private PluginConfig pluginConfig;
    /**
     * Whether to auto-register, default is not to auto-register.
     */
    private boolean registerSelf = REGISTER_SELF_DEFAULT;
    private int ttl = TTL_DEFAULT;
    /**
     * Heartbeat interval time.
     */
    private int heartbeatInterval = HEARTBEAT_INTERVAL_DEFAULT;

    @Override
    public void setPluginConfig(PluginConfig pluginConfig) throws TRpcExtensionException {
        this.pluginConfig = pluginConfig;
    }

    @Override
    public void init() throws TRpcExtensionException {
        try {
            Objects.requireNonNull(pluginConfig, "registry config can not be null");
            // Configuration object for Polaris.
            ConfigurationImpl configuration;
            Map<String, Object> extMap = pluginConfig.getProperties();
            if (MapUtils.isNotEmpty(extMap)) {
                configuration = PolarisRegisterUtil.genPolarisConfiguration(extMap);
                heartbeatInterval = MapUtils.getIntValue(extMap, HEARTBEAT_INTERVAL_KEY, HEARTBEAT_INTERVAL_DEFAULT);
                registerSelf = MapUtils.getBooleanValue(extMap, REGISTER_SELF, REGISTER_SELF_DEFAULT);
                ttl = MapUtils.getIntValue(extMap, TTL_KEY, TTL_DEFAULT);
            } else {
                configuration = new ConfigurationImpl();
                configuration.setDefault();
            }
            HeartBeatManager.init(heartbeatInterval);

            providerApi = APIFactory.createProviderAPIByConfig(configuration);
        } catch (PolarisException e) {
            logger.error("failed to init providerApi", e);
            throw new IllegalStateException("failed to create providerApi with config:" + pluginConfig.getProperties(),
                    e);
        }

    }

    /**
     * Sending a heartbeat will be called regularly on a schedule.
     *
     * @param registerInfo Registration information.
     */
    public void heartbeat(RegisterInfo registerInfo) {
        InstanceHeartbeatRequest heartbeatRequest = buildHeartbeatRequest(registerInfo);
        try {
            Objects.requireNonNull(heartbeatRequest.getInstanceID(), "instance_id can not be null");
            Objects.requireNonNull(heartbeatRequest.getNamespace(), "namespace can not be null");
            Objects.requireNonNull(heartbeatRequest.getToken(), "token can not be null");
            providerApi.heartbeat(heartbeatRequest);
            logger.debug("[heartbeat] success, request:{}", heartbeatRequest);
        } catch (Exception e) {
            logger.error("[heartbeat] send heartbeat to polaris failed", e);
            throw new IllegalStateException(
                    "Failed to send heartbeat from " + registerInfo + "to registry," + " cause: " + e.getMessage(), e);
        }
    }

    @Override
    public void register(RegisterInfo registerInfo) {
        if (!registerSelf) {
            //do nothing
        } else {
            InstanceRegisterRequest registerRequest = buildRegisterRequest(registerInfo);
            try {
                InstanceRegisterResponse registerResponse = providerApi.register(registerRequest);
                polarisIdRelation.putIfAbsent(registerInfo.toIdentityString(), registerResponse.getInstanceId());
                logger.debug("[register] success,request:{},response:{}", registerRequest, registerResponse);
            } catch (Exception e) {
                logger.error("[register] register to polaris failed.", e);
                throw new IllegalStateException(
                        "Failed to register provider " + registerInfo + " to registry," + " cause: " + e.getMessage(),
                        e);
            }
        }
        HeartBeatManager.startHeartBeat(registerInfo, this);
    }

    @Override
    public void unregister(RegisterInfo registerInfo) {
        // If not auto-registered, there is no need to unregister.
        if (!registerSelf) {
            return;
        }
        InstanceDeregisterRequest deRegisterRequest = buildDeRegisterRequest(registerInfo);
        try {
            providerApi.deRegister(deRegisterRequest);
            logger.debug("[unregister] success,request:{}", deRegisterRequest);
        } catch (Exception e) {
            logger.error("unregister from polaris failed", e);
            throw new IllegalStateException(
                    "Failed to unregister " + registerInfo + " from registry " + ", cause: " + e.getMessage(), e);
        }
    }

    /**
     * Build a heartbeat request based on the service instance.
     *
     * @param registerInfo Service instance.
     */
    private InstanceHeartbeatRequest buildHeartbeatRequest(RegisterInfo registerInfo) {
        if (heartbeatRequestCache.get(registerInfo) != null) {
            return heartbeatRequestCache.get(registerInfo);
        }
        InstanceHeartbeatRequest heartbeatRequest = new InstanceHeartbeatRequest();
        heartbeatRequest.setInstanceID(polarisIdRelation.get(registerInfo.toIdentityString()));
        heartbeatRequest.setService(registerInfo.getServiceName());
        heartbeatRequest.setHost(registerInfo.getHost());
        heartbeatRequest.setPort(registerInfo.getPort());
        if (registerInfo.getParameter(TOKEN_PARAM_KEY) != null) {
            heartbeatRequest.setToken(registerInfo.getParameter(TOKEN_PARAM_KEY));
        }
        if (registerInfo.getParameter(NAMESPACE_KEY) != null) {
            heartbeatRequest.setNamespace(registerInfo.getParameter(NAMESPACE_KEY));
        }
        if (registerInfo.getParameter(TIMEOUT_PARAM_KEY) != null) {
            heartbeatRequest.setTimeoutMs(Long.parseLong(registerInfo.getParameter(TIMEOUT_PARAM_KEY)));
        }
        if (registerInfo.getParameter(INSTANCE_ID) != null) {
            heartbeatRequest.setInstanceID(String.valueOf(registerInfo.getParameter(INSTANCE_ID)));
        }
        heartbeatRequestCache.put(registerInfo, heartbeatRequest);
        return heartbeatRequest;
    }

    /**
     * Build a Polaris registration request based on the service instance.
     *
     * @param registerInfo The registration instance object in trpc.
     * @return The unregister instance object in the Polaris SDK.
     */
    private InstanceDeregisterRequest buildDeRegisterRequest(RegisterInfo registerInfo) {
        InstanceDeregisterRequest deRegisterRequest = new InstanceDeregisterRequest();
        deRegisterRequest.setInstanceID(polarisIdRelation.get(registerInfo.toIdentityString()));
        if (registerInfo.getParameter(TOKEN_PARAM_KEY) != null) {
            deRegisterRequest.setToken(registerInfo.getParameter(TOKEN_PARAM_KEY));
        }
        if (registerInfo.getParameter(NAMESPACE_KEY) != null) {
            deRegisterRequest.setNamespace(registerInfo.getParameter(NAMESPACE_KEY));
        }
        if (registerInfo.getParameter(INSTANCE_ID) != null) {
            deRegisterRequest.setInstanceID(String.valueOf(registerInfo.getParameter(INSTANCE_ID)));
        }
        if (registerInfo.getParameter(TIMEOUT_PARAM_KEY) != null) {
            deRegisterRequest.setTimeoutMs(Long.parseLong(registerInfo.getParameter(TIMEOUT_PARAM_KEY)));
        }
        deRegisterRequest.setHost(registerInfo.getHost());
        deRegisterRequest.setPort(registerInfo.getPort());
        deRegisterRequest.setService(registerInfo.getServiceName());
        return deRegisterRequest;
    }

    /**
     * Build a Polaris unregister request based on the service instance.
     *
     * @param registerInfo The registration instance object in trpc.
     * @return Unregister instance object in the Polaris SDK.
     */
    private InstanceRegisterRequest buildRegisterRequest(RegisterInfo registerInfo) {
        InstanceRegisterRequest instanceRegisterRequest = new InstanceRegisterRequest();
        instanceRegisterRequest.setService(registerInfo.getServiceName());
        instanceRegisterRequest.setHost(registerInfo.getHost());
        instanceRegisterRequest.setPort(registerInfo.getPort());
        instanceRegisterRequest.setProtocol(registerInfo.getProtocol());
        instanceRegisterRequest.setVersion(registerInfo.getVersion());
        instanceRegisterRequest.setTtl(ttl);
        fillParams(registerInfo, instanceRegisterRequest);
        Map<String, String> metadata = PolarisRegisterUtil.trans2StringMap(registerInfo.getParameters());
        removeRedundantMetadata(metadata);
        instanceRegisterRequest.setMetadata(metadata);

        if (ConfigManager.getInstance().getGlobalConfig().isEnableSet()) {
            instanceRegisterRequest.getMetadata().put(INTERNAL_ENABLE_SET_KEY, INTERNAL_ENABLE_SET_Y);
        }
        if (ConfigManager.getInstance().getGlobalConfig().getFullSetName() != null) {
            instanceRegisterRequest.getMetadata()
                    .put(INTERNAL_SET_NAME_KEY, ConfigManager.getInstance().getGlobalConfig().getFullSetName());
        }
        if (registerInfo.getParameter(CONTAINER_NAME) == null
                && ConfigManager.getInstance().getGlobalConfig().getContainerName() != null) {
            instanceRegisterRequest.getMetadata()
                    .put(CONTAINER_NAME, ConfigManager.getInstance().getGlobalConfig().getContainerName());
        }
        // Environment name.
        String envName = registerInfo.getParameter(ENV_NAME_KEY);
        envName = envName == null ? ConfigManager.getInstance().getGlobalConfig().getEnvName() : envName;
        if (envName != null) {
            instanceRegisterRequest.getMetadata().put(POLARIS_ENV, envName);
        }
        return instanceRegisterRequest;
    }

    private void fillParams(RegisterInfo registerInfo, InstanceRegisterRequest instanceRegisterRequest) {
        if (registerInfo.getParameter(TOKEN_PARAM_KEY) != null) {
            instanceRegisterRequest.setToken(registerInfo.getParameter(TOKEN_PARAM_KEY));
        }
        if (registerInfo.getParameter(NAMESPACE_KEY) != null) {
            instanceRegisterRequest.setNamespace(registerInfo.getParameter(NAMESPACE_KEY));
        }
        if (registerInfo.getParameter(WEIGHT_PARAM_KEY) != null) {
            instanceRegisterRequest.setWeight(Integer.parseInt(registerInfo.getParameter(WEIGHT_PARAM_KEY)));
        }
        if (registerInfo.getParameter(PRIORITY_PARAM_KEY) != null) {
            instanceRegisterRequest.setPriority(Integer.parseInt(registerInfo.getParameter(PRIORITY_PARAM_KEY)));
        }
        if (registerInfo.getParameter(TIMEOUT_PARAM_KEY) != null) {
            instanceRegisterRequest.setTimeoutMs(Long.parseLong(registerInfo.getParameter(TIMEOUT_PARAM_KEY)));
        }
    }

    /**
     * Remove data that does not need to be registered on the instance metadata.
     * Name, namespace, and token are service-level information and do not belong to a single instance.
     * Weight, priority, and timeout are properties of a single instance in Polaris and are not metadata.
     *
     * @param metadata The metadata to be cleaned.
     */
    private void removeRedundantMetadata(Map<String, String> metadata) {
        if (metadata.containsKey(NAME)) {
            metadata.remove(NAME);
        }
        if (metadata.containsKey(NAMESPACE_KEY)) {
            metadata.remove(NAMESPACE_KEY);
        }
        if (metadata.containsKey(TOKEN_PARAM_KEY)) {
            metadata.remove(TOKEN_PARAM_KEY);
        }
        if (metadata.containsKey(WEIGHT_PARAM_KEY)) {
            metadata.remove(WEIGHT_PARAM_KEY);
        }
        if (metadata.containsKey(PRIORITY_PARAM_KEY)) {
            metadata.remove(PRIORITY_PARAM_KEY);
        }
        if (metadata.containsKey(TIMEOUT_PARAM_KEY)) {
            metadata.remove(TIMEOUT_PARAM_KEY);
        }
    }

    @Override
    public void destroy() throws TRpcExtensionException {
        HeartBeatManager.destroy();
    }
}