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

package com.tencent.trpc.polaris.common;

import com.tencent.trpc.core.common.Constants;

/**
 * Polaris selector use Constant
 */
public interface PolarisConstant {

    /**
     * Polaris api config in common use key
     */
    String POLARIS_API_MAXRETRYTIMES_KEY = "maxRetryTimes";
    String POLARIS_API_BINDIF_KEY = "bindIf";

    /**
     * Polaris connect config in common use key
     */
    String POLARIS_RUNMODE_KEY = "mode";
    String POLARIS_ADDRESSES_KEY = "address_list";
    String POLARIS_PROTOCOL_KEY = "protocol";

    /**
     * Enable settings, remove the prefix from the transparent field of the 'selector-meta' prefix,
     * and fill in the metaData of SourceServiceInfo
     */
    String POLARIS_ENABLE_TRANS_META = "enable_trans_meta";

    /**
     * The service name of SourceServiceInfo will not be filled in,
     * which is compatible with scenarios where the calling service is not registered on Polaris
     */
    String POLARIS_DISABLE_CALLER_SERVICE_NAME = "disable_caller_service_name";

    String POLARIS_CONSUMER_KEY = "consumer";
    String POLARIS_CIRCUIT_BREAKER_KEY = "circuitbreaker";

    /**
     * Polaris consumer config in common use key
     */
    String POLARIS_LOCALCACHE = "localCache";
    String POLARIS_LOCALCACHE_PERSISTDIR = "persistDir";
    String POLARIS_LOCALCACHE_TYPE = "type";
    String POLARIS_LOCALCACHE_MAXEJECTPERCENTTHRESHOLD = "maxEjectPercentThreshold";
    String POLARIS_LOCALCACHE_PERSISTMAXWRITERETRY = "persistMaxWriteRetry";
    String POLARIS_LOCALCACHE_PERSISTMAXREADRETRY = "persistMaxReadRetry";

    enum TrpcPolarisParams {

        /**
         * Get instances timeout key
         */
        TIMEOUT_PARAM_KEY("timeout", true),

        /**
         * if skip route filter or not key
         */
        SKIP_FILTER_PARAM_KEY("skipRouteFilter", true),

        NAMESPACE_KEY(Constants.NAMESPACE, true),

        /**
         * if include unhealthy instance or not key
         */
        INCLUDE_UNHEALTHY(Constants.INCLUDE_UNHEALTHY, true),

        /**
         * if include circuitbreak instance or not key
         */
        INCLUDE_CIRCUITBREAK(Constants.INCLUDE_CIRCUITBREAK, true),

        CANARY("canary", true),

        METADATA(Constants.METADATA, true);

        String key;

        /**
         * if is trpc plugin key or not
         */
        boolean isTrpcPluginKey;

        TrpcPolarisParams(String key, boolean isTrpcPluginKey) {
            this.key = key;
            this.isTrpcPluginKey = isTrpcPluginKey;
        }

        public String getKey() {
            return key;
        }

        public boolean isTrpcPluginKey() {
            return isTrpcPluginKey;
        }

        /**
         * get trpc polaris params by key from TrpcPolarisParams
         *
         * @param key use key
         * @return TrpcPolarisParams: trpc polaris params
         */
        public static TrpcPolarisParams getByKey(String key) {
            for (TrpcPolarisParams value : TrpcPolarisParams.values()) {
                if (value.getKey().equals(key)) {
                    return value;
                }
            }
            return null;
        }
    }

    /**
     * Configuration of enabling canary routing in the protocol
     */
    String CANARY_OPEN = "1";

    /**
     * Does the parameter key of the polaris plugin,
     * allow cross environment calls or not, the default is not allowed
     */
    String NAMESPACE_DIFF_ALLOWED = "namespace_diff_allowed";
    boolean NAMESPACE_DIFF_ALLOWED_DEFAULT = false;

    /**
     * Polaris some keys for using functions in callback results
     */
    String POLARIS_PRE = "polaris_";
    String POLARIS_RESPONSE = POLARIS_PRE + "response";
    String POLARIS_INSTANCE = POLARIS_PRE + "instance";
    String POLARIS_REVISION = POLARIS_PRE + "revision";
    String POLARIS_HEALTHY = POLARIS_PRE + "healthy";
    String POLARIS_PROTOCOL = POLARIS_PRE + "protocol";
    String POLARIS_ID = POLARIS_PRE + "id";
    String POLARIS_VERSION = POLARIS_PRE + "version";
    String POLARIS_CMDBREGION = POLARIS_PRE + "cmdbRegion";
    String POLARIS_CMDBZONE = POLARIS_PRE + "cmdbZone";
    String POLARIS_CMDBCAMPUS = POLARIS_PRE + "cmdbCampus";
    String POLARIS_PRIORITY = POLARIS_PRE + "priority";
    String POLARIS_SERVICE = POLARIS_PRE + "service";

    /**
     * User defined key in rule router
     */
    String POLARIS_ROUTER_KEY = "trpc-key";
    /**
     * Environment Priority Key in rule router
     */
    String POLARIS_ROUTER_ENV = "trpc-env";

    /**
     * Enable key for set router
     */
    String INTERNAL_ENABLE_SET_KEY = "internal-enable-set";
    /**
     * set router name
     */
    String INTERNAL_SET_NAME_KEY = "internal-set-name";
    /**
     * When set router is enabled, INTERNAL_ SET_ NAME_ KEY value
     */
    String INTERNAL_ENABLE_SET_Y = "Y";

    /**
     * The key of the result returned in the Polaris rule routing
     */
    String POLARIS_PB_KEY = "key";
    String POLARIS_PB_ENV = "env";

    /**
     * operate Platform registered container name key
     */
    String CONTAINER_NAME = "container_name";
    /**
     * operate Platform registered container name key
     */
    String CONTAINER_NAME_TAF = "internal-seer-container_name";
    String EMPTY_STRING = "";

    /**
     * Calling service metadata prefix
     */
    String SELECTOR_META_PREFIX = "selector-meta-";

    /**
     * Calling service namespace
     */
    String SELECTOR_NAMESPACE = "selector-namespace";

    /**
     * Calling service service name
     */
    String SELECTOR_SERVICE_NAME = "selector-service-name";

    /**
     * Callee service env name
     */
    String SELECTOR_ENV_NAME = "selector-env-name";

    /**
     * Tag identification for data transmission across tRPC processes
     */
    String RPC_CONTEXT_TRANSITIVE_METADATA = "tRPC-Polaris-Transitive-Metadata";

    /**
     * Data Transmission Label Identification for tRPC Process
     */
    String RPC_CONTEXT_POALRIS_METADATA = "tRPC-Polaris-Metadata";

}

