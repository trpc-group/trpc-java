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

/**
 * Polaris registry center constant class.
 */
public interface PolarisRegistryConstant {

    /**
     * Polaris api configuration
     */
    String POLARIS_API_MAX_RETRY_TIMES_KEY = "maxRetryTimes";
    String POLARIS_API_BIND_IF_KEY = "bindIf";
    /**
     * Commonly used Polaris connect configurations include
     */
    String POLARIS_RUN_MODE_KEY = "mode";
    String POLARIS_ADDRESSES_KEY = "address_list";
    String POLARIS_PROTOCOL_KEY = "protocol";

    /**
     * Polaris heartbeat thread configuration.
     */
    String HEARTBEAT_THREAD_CORE_SIZE = "heartbeatThreadCoreSize";
    String HEARTBEAT_THREAD_MAXSIZE = "heartbeatThreadMaxSize";
    String HEARTBEAT_THREAD_QUEUE_SIZE = "heartbeatThreadQueueSize";
    String HEARTBEAT_THREAD_KEEP_ALIVE_SECONDS = "heartbeatThreadKeepAliveSeconds";

    /**
     * Service key, configuration related to the registry center.
     */
    String TIMEOUT_PARAM_KEY = "timeout";
    String TTL_KEY = "ttl";

    /**
     * Configuration key for automatic registration.
     */
    String REGISTER_SELF = "register_self";
    /**
     * By default, automatic registration is not enabled.
     */
    boolean REGISTER_SELF_DEFAULT = false;

    /**
     * Some related configurations of services under the Polaris registry center.
     */
    String NAME = "name";
    String NAMESPACE_KEY = "namespace";
    String WEIGHT_PARAM_KEY = "weight";
    String TOKEN_PARAM_KEY = "token";
    String PRIORITY_PARAM_KEY = "priority";
    String INSTANCE_ID = "instance_id";

    /**
     * The Key of heartbeat interval configuration.
     */
    String HEARTBEAT_INTERVAL_KEY = "heartbeat_interval";
    /**
     * Default heartbeat timeout interval.
     */
    int HEARTBEAT_INTERVAL_DEFAULT = 3000;
    /**
     * ttl default time
     */
    int TTL_DEFAULT = 5000;


    /**
     * Whether to enable set routing.
     */
    String INTERNAL_ENABLE_SET_KEY = "internal-enable-set";
    /**
     * enable set routing.
     */
    String INTERNAL_ENABLE_SET_Y = "Y";
    /**
     * Name of the set routing.
     */
    String INTERNAL_SET_NAME_KEY = "internal-set-name";

    /**
     * Name of the container.
     */
    String CONTAINER_NAME = "container_name";
}

