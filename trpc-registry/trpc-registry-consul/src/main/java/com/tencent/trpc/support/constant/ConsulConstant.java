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

package com.tencent.trpc.support.constant;

/**
 * Consul constants class
 */
public final class ConsulConstant {

    /**
     * Default tag
     */
    public static final String SERVICE_TAG = "trpc";

    /**
     * Registry address, separated by commas. For example, the address of Consul: 192.0.0.1:2181,192.0.0.2:2181
     */
    public static final String REGISTRY_ADDRESSED = "addresses";
    /**
     * Default registry address
     */
    public static final String DEFAULT_REGISTRY_ADDRESSED = "localhost:8500";
    /**
     * Registry address separator
     */
    public static final String REGEX_REGISTRY_ADDRESSED = ":";
    /**
     * Registry multiple addresses separator
     */
    public static final String REGEX_REGISTRY_ADDRESSED_LIST = ",";
    /**
     * Consul exception message constant
     */
    public static final String CONSUL_CLIENT_CONNECTED_EXCEPTION = "Connection refused";
    /**
     * Consul TTL interval
     */
    public static final String CHECK_PASS_INTERVAL = "consul-check-pass-interval";
    /**
     * Consul default TTL interval in MILLISECONDS
     */
    public static final long DEFAULT_CHECK_PASS_INTERVAL = 16000L;
    /**
     * Consul registry center default port
     */
    public static final int DEFAULT_PORT = 8500;
    /**
     * Consul registry center token configuration
     */
    public static final String ACL_TOKEN = "acl_token";
    /**
     * Consul registry center tag configuration
     */
    public static final String TAG = "tag";
    /**
     * Consul registry center ID separator
     */
    public static final String INSTANCE_ID_SEPARATOR = "-";
    /**
     * Consul registry center automatic deregistration time configuration
     */
    public static final String DEREGISTER_AFTER = "consul-deregister-critical-service-after";
    /**
     * Consul registry center automatic deregistration default time configuration
     */
    public static final String DEFAULT_DEREGISTER_TIME = "20s";
    /**
     * Consul registry center service name wildcard
     */
    public static String ANY_VALUE = "*";
    /**
     * Consul registry center watch timeout configuration
     */
    public static final String WATCH_TIMEOUT = "consul-watch-timeout";
    /**
     * Consul registry center default watch timeout (in milliseconds)
     */
    public static final int DEFAULT_WATCH_TIMEOUT = 60 * 1000;
    /**
     * Consul registry center meta data URL configuration
     */
    public static final String URL_META_KEY = "url";
    /**
     * Consul registry center TTL enabled configuration
     */
    public static final String TTL_ENABLED = "ttl_enabled";
    /**
     * Consul registry center health check interval configuration (Consul server actively sends heartbeats)
     */
    public static final String HEALTH_CHECK_INTERVAL = "health_check_interval";
    /**
     * Consul registry center health check timeout configuration (Consul server actively sends heartbeats)
     */
    public static final String HEALTH_CHECK_TIMEOUT = "health_check_timeout";
    /**
     * Consul registry center default health check timeout (Consul server actively sends heartbeats)
     */
    public static final String DEFAULT_HEALTH_TIME = "10s";
    /**
     * Consul registry center custom check URL configuration (Consul server actively sends heartbeats)
     */
    public static final String HEALTH_CHECK_URL = "health_check_url";
    /**
     * Consul registry center communication protocol configuration (Consul server actively sends heartbeats) http or
     * https
     */
    public static final String SCHEME = "scheme";
    /**
     * Consul registry center health check port configuration (Consul server actively sends heartbeats) http or https
     */
    public static final String HEALTH_PORT = "port";
    /**
     * Consul registry center default health port (Consul server actively sends heartbeats)
     */
    public static final String DEFAULT_HEALTH_PORT = "8080";
    /*
     * Consul registry center default communication protocol configuration (Consul server actively sends heartbeats)
     */
    public static final String DEFAULT_SCHEME = "http";
    /*
     * Consul registry center health check path configuration (Consul server actively sends heartbeats)
     */
    public static final String HEALTH_CHECK_PATH = "health_check_path";
    /*
     * Consul registry center default health check path (Consul server actively sends heartbeats)
     */
    public static final String DEFAULT_HEALTH_CHECK_PATH = "/health";
    /*
     * Consul registry center TTL check prefix configuration
     */
    public static final String SERVICE_PRE = "service:";
    /*
     * Query Consul server index, -1 means query all
     */
    public static final long CONSUL_SERVICE_INDEX = -1;
    /*
     * TTL scheduling interval time (in milliseconds)
     */
    public static final String TTL_SCHEDULE_TIME_INTERVAL = "ttl_schedule_time_interval";
    public static final long DEFAULT_TTL_SCHEDULE_TIME_INTERVAL = 2000;

    private ConsulConstant() {
        throw new IllegalStateException("not support invoke");
    }
}
