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

package com.tencent.trpc.core.common;

import com.tencent.trpc.core.compressor.support.NoneCompressor;
import com.tencent.trpc.core.proxy.support.ByteBuddyProxyFactory;
import com.tencent.trpc.core.serialization.support.PBSerialization;

/**
 * Constant definitions, system variables please move to {@link TRpcSystemProperties}
 */
public class Constants {

    public static final int CPUS = Runtime.getRuntime().availableProcessors();
    public static final String UNKNOWN = "unknown";
    public static final String COLON = ":";

    public static final String NORMAL = "normal";
    public static final String DEFAULT = "default";
    public static final String NETWORK_TCP = "tcp";
    public static final String NETWORK_UDP = "udp";
    public static final String TRANSPORTER_NETTY = "netty";
    public static final String IO_MODE_EPOLL = "epoll";
    public static final String PROTOCOL_TRPC = "trpc";
    public static final String COFNFIG_TYPE_YAML = "yaml";
    public static final String CONTAINER_TYPE = "container";

    public static final String DEFAULT_KEEP_ALIVE = "true";
    /**
     * Default shared IO thread pool true
     */
    public static final String DEFAULT_IO_THREAD_GROUPSHARE = "true";
    /**
     * Default reuse port false
     */
    public static final String DEFAULT_REUSE_PORT = "false";
    /**
     * Default delay initialization false
     */
    public static final String DEFAULT_LAZY_INIT = "false";
    /**
     * CLIENT request default timeout unit milliseconds
     */
    public static final String DEFAULT_CLIENT_REQUEST_TIMEOUT_MS = "1000";

    /**
     * Request default processing timeout on Server side unit milliseconds
     */
    public static final String DEFAULT_SERVER_TIMEOUT_MS = "2147483647";

    /**
     * Backup request time, unit is ms. Default is 0ms not enabled
     */
    public static final String DEFAULT_BACKUP_REQUEST_TIME_MS = "0";
    /**
     * Default connection queue size
     */
    public static final String DEFAULT_BACK_LOG_SIZE = "1024";
    /**
     * Default io thread count cpus * 2
     */
    public static final int DEFAULT_IO_THREADS = CPUS * 2;
    /**
     * Default boss thread count  Default 1
     */
    public static final String DEFAULT_BOSS_THREADS = "1";
    /**
     * Default business thread count cpus * 2
     */
    public static final int DEFAULT_BIZ_THREADS = CPUS * 2;
    /**
     * Default coroutine pool core count
     */
    public static final int DEFAULT_BIZ_VIRTUAL_CORE_THREADS = 50000;

    /**
     * Default coroutine pool maximum count
     */
    public static final int DEFAULT_BIZ_VIRTUAL_MAX_THREADS = 200000;
    /**
     * Default timeout
     */
    public static final int DEFAULT_TIMEOUT = 1000;
    /**
     * Default service close timeout 30 s
     */
    public static final String DEFAULT_SERVER_CLOSE_TIMEOUT = "30000";
    /**
     * Default connection timeout 1 s
     */
    public static final String DEFAULT_CONNECT_TIMEOUT = "1000";
    /**
     * Default maximum number of connections 20480
     */
    public static final String DEFAULT_MAX_CONNECTIONS = "20480";
    /**
     * Default maximum payload limit 10M
     */
    public static final String DEFAULT_PAYLOAD = "10485760";
    /**
     * Default buffer size 16KB
     */
    public static final String DEFAULT_BUFFER_SIZE = "16384";
    /**
     * Default client idle timeout 3 minutes
     */
    public static final String DEFAULT_IDLE_TIMEOUT = "180000";
    /**
     * Default server idle timeout 4 minutes
     */
    public static final String DEFAULT_SERVER_IDLE_TIMEOUT = "240000";
    /**
     * Default number of connections per address 2
     */
    public static final String DEFAULT_CONNECTIONS_PERADDR = "2";
    public static final String DEFAULT_TRANSPORTER = TRANSPORTER_NETTY;
    public static final String DEFAULT_IO_MODE = IO_MODE_EPOLL;
    /**
     * Default to disable batch flush
     */
    public static final String DEFAULT_FLUSH_CONSOLIDATION = "false";
    /**
     * Default to enable batch decoding
     */
    public static final String DEFAULT_IS_BATCH_DECODER = "true";
    /**
     * Default batch flush size
     */
    public static final String DEFAULT_EXPLICIT_FLUSH_AFTER_FLUSHES = "2048";
    /**
     * Default character set UTF-8
     */
    public static final String DEFAULT_CHARSET = "UTF-8";
    public static final String DEFAULT_NETWORK_TYPE = Constants.NETWORK_TCP; // udp
    public static final String DEFAULT_PROXY = ByteBuddyProxyFactory.NAME;
    /**
     * Default protocol type
     */
    public static final String DEFAULT_PROTOCOL_TYPE = "standard";
    public static final String DEFAULT_CONTAINER = DEFAULT;
    public static final String DEFAULT_CONFIG_TYPE = COFNFIG_TYPE_YAML;
    public static final String DEFAULT_PROTOCOL = PROTOCOL_TRPC;
    public static final String DEFAULT_SERIALIZATION = PBSerialization.NAME;
    public static final String DEFAULT_COMPRESSOR = NoneCompressor.NAME;
    /**
     * Default minimum compression bytes 65535 bytes
     */
    public static final String DEFAULT_COMPRESS_MIN_BYTES = "65535";
    public static final String DEFAULT_GROUP = NORMAL;
    public static final String DEFAULT_VERSION = "v1.0.0";
    public static final String DEFAULT_CONFIGCENTER = "rainbow";

    public static final String OLD_CONFIG_PATH = "config_path";
    public static final String CONFIG_PATH = TRpcSystemProperties.CONFIG_PATH;

    /**
     * Whether to include unhealthy nodes
     */
    public static final String INCLUDE_UNHEALTHY = "includeUnHealthy";
    /**
     * Whether to include circuit break nodes
     */
    public static final String INCLUDE_CIRCUITBREAK = "includeCircuitBreak";
    /**
     * Application name
     */
    public static final String APP = "app";
    /**
     * Service name
     */
    public static final String SERVER = "server";
    /**
     * Environment name
     */
    public static final String ENV_NAME_KEY = "env_name";
    /**
     * Namespace
     */
    public static final String NAMESPACE = "namespace";
    /**
     * Set name
     */
    public static final String SET_DIVISION = "set_name";
    /**
     * Container name
     */
    public static final String CONTAINER_NAME = "container_name";

    /**
     * Environment name registered to Polaris
     */
    public static final String POLARIS_ENV = "env";

    /**
     * User-configured metadata key
     */
    public static final String METADATA = "metadata";

    /**
     * TRPC service serviceId format prefix
     */
    public static final String STANDARD_NAMING_PRE = "trpc.";

}
