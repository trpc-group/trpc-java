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

package com.tencent.trpc.core.common.config.constant;

/**
 * Configuration constants class, mainly for framework constants, plugin constants are controlled by the plugin itself.
 */
public final class ConfigConstants {

    /**
     * Global configuration tag.
     */
    public static final String GLOBAL = "global";
    /**
     * Whether to enable set-based routing.
     */
    public static final String ENABLE_SET = "enable_set";
    /**
     * Plugin tag.
     */
    public static final String PLUGINS = "plugins";
    /**
     * System type
     */
    public static final String SYSTEM_TYPE = "_type";
    /**
     * Server configuration start tag.
     */
    public static final String SERVER = "server";
    /**
     * Local IP.
     */
    public static final String LOCAL_IP = "local_ip";
    /**
     * Local address.
     */
    public static final String LOCAL_ADDRESS = "localAddress";
    /**
     * Server listener.
     */
    public static final String SERVER_LISTENER = "server_listener";
    /**
     * Server listeners.
     */
    public static final String SERVER_LISTENERS = "serverListeners";
    /**
     * Listener class.
     */
    public static final String LISTENER_CLASS = "listener_class";
    /**
     * Admin tag.
     */
    public static final String ADMIN = "admin";
    /**
     * Server exposed service node.
     */
    public static final String SERVICE = "service";
    public static final String INTERFACE = "interface";
    /**
     * Field name corresponding to the interface.
     */
    public static final String SERVICE_INTERFACE = "serviceInterface";
    /**
     * List of exposed service implementations.
     */
    public static final String IMPLS = "impls";
    /**
     * Registration configuration.
     */
    public static final String REGISTRYS = "registrys";
    public static final String IP = "ip";
    /**
     * Number of IO threads.
     */
    public static final String IO_THREADS = "io_threads";
    /**
     * Extra configuration.
     */
    public static final String EXT_MAP = "ext_map";
    /**
     * Whether to disable the default filter.
     */
    public static final String DISABLE_DEFAULT_FILTER = "disable_default_filter";
    /**
     * Filter configuration.
     */
    public static final String FILTERS = "filters";
    /**
     * Interceptor configuration.
     */
    public static final String INTERCEPTORS = "interceptors";
    /**
     * Client node.
     */
    public static final String CLIENT = "client";
    /**
     * Unique client name.
     */
    public static final String NAME = "name";
    /**
     * Client configuration caller service name.
     */
    public static final String CALLER_SERVICE_NAME = "caller_service_name";

}
