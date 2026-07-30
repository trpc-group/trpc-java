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
     * Default shared IO thread pool: true. The shared pool now supports both NIO and
     * Epoll: each transport joins one of two reference-counted shared groups based on
     * {@code Epoll.isAvailable() && config.useEpoll()}, so enabling epoll no longer forces
     * the user to also flip {@code ioThreadGroupShare} off. Server-side transports do not
     * consult this flag (they always own their own group).
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
     * Default business core thread count 50
     */
    public static final int DEFAULT_CORE_THREADS = 50;
    /**
     * Default business max thread count 200
     */
    public static final int DEFAULT_MAX_THREADS = 200;
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
     * Default stop service wait timeout 5 s
     */
    public static final String DEFAULT_SERVER_WAIT_TIMEOUT = "5000";
    /**
     * Default connection timeout 1 s
     */
    public static final String DEFAULT_CONNECT_TIMEOUT = "1000";
    /**
     * Default maximum number of connections.
     *
     * <p>200 covers the great majority of internal RPC workloads while keeping the worst-case
     * resource footprint bounded:</p>
     * <ul>
     *     <li>By Little's Law (concurrent in-flight = QPS × RT) 200 connections sustain
     *         e.g. {@code QPS=20000 × RT=10ms} or {@code QPS=2000 × RT=100ms} on the HTTP/1.1
     *         path where each connection serves one request at a time;</li>
     *     <li>Far below the Linux default ephemeral-port range (~28k usable) so a connection
     *         storm against an unreachable backend cannot exhaust the port pool;</li>
     *     <li>Friendly to backend LBs whose per-source keep-alive limits are typically in the
     *         low thousands.</li>
     * </ul>
     *
     * <p>Workloads that genuinely need more (high-fanout aggregation services, very high QPS
     * with long RT) should override per-{@code BackendConfig} via {@code maxConns}.</p>
     *
     * <p>Note: the trpc protocol path does not read this field — it sizes the connection pool
     * via {@code connsPerAddr} (default 4). Only the HTTP / HTTP/2 paths consume it.</p>
     */
    public static final String DEFAULT_MAX_CONNECTIONS = "200";
    /**
     * Default maximum payload limit 10M
     */
    public static final String DEFAULT_PAYLOAD = "10485760";
    /**
     * Default buffer size 16KB
     */
    public static final String DEFAULT_BUFFER_SIZE = "16384";
    /**
     * Default client idle timeout 3 minutes. Drives the read-idle close handler installed
     * by {@code NettyTcpClientTransport}: a long connection that has not received any
     * inbound bytes for this duration is recycled by the client (the next request goes
     * through lazy reconnect). Half-dead detection in faster paths is delegated to the
     * configurable TCP keepalive parameters below; the read-idle handler is the slow
     * universal fallback that also covers macOS / Windows where TCP keepalive tuning is
     * not available.
     */
    public static final String DEFAULT_IDLE_TIMEOUT = "180000";
    /**
     * Default TCP keepalive idle (Linux {@code TCP_KEEPIDLE}) in seconds: 30 s without
     * traffic on the socket before the kernel starts emitting keepalive probes. Together
     * with {@link #DEFAULT_TCP_KEEPALIVE_INTVL} and {@link #DEFAULT_TCP_KEEPALIVE_CNT}
     * (Dubbo-style 30/10/3) this yields a half-dead detection window of roughly 60 s on
     * Linux + epoll (30 + 10 × 3 = 60). Value 0 leaves the OS default (Linux: 7200 s).
     */
    public static final String DEFAULT_TCP_KEEPALIVE_IDLE = "30";
    /**
     * Default TCP keepalive interval (Linux {@code TCP_KEEPINTVL}) in seconds between
     * consecutive keepalive probes after the idle window has elapsed.
     */
    public static final String DEFAULT_TCP_KEEPALIVE_INTVL = "10";
    /**
     * Default TCP keepalive probe count (Linux {@code TCP_KEEPCNT}): the number of
     * unacknowledged keepalive probes after which the kernel marks the connection dead
     * and emits a RST.
     */
    public static final String DEFAULT_TCP_KEEPALIVE_CNT = "3";
    /**
     * Default server idle timeout 4 minutes
     */
    public static final String DEFAULT_SERVER_IDLE_TIMEOUT = "240000";
    /**
     * Default number of long-lived TCP connections to a single peer address.
     *
     * <p>Each business request is dispatched round-robin across these N channels
     * ({@code channelIdx.getAndIncrement() % N}); on a single channel Netty multiplexes many
     * in-flight RPCs via the protocol-level sequence id, so {@code N=1} would already saturate
     * a 1Gbps link in pure throughput. The default of <b>4</b> is chosen so that:</p>
     * <ul>
     *     <li>Multiple Netty {@code EventLoop} threads run in parallel for a single peer —
     *         no single EventLoop becomes a CPU bottleneck under high QPS;</li>
     *     <li>Failure-domain isolation: when a channel is invalidated by READ_IDLE / RST /
     *         half-close, the remaining 3 channels absorb traffic with minimal RT impact
     *         (compared to {@code N=2} which would double the load on the surviving channel);</li>
     *     <li>Resource cost stays bounded: 4 fd × ephemeral ports × ~16KB Netty buffer per
     *         peer is negligible even with hundreds of peer addresses.</li>
     * </ul>
     *
     * <p>Tuning guidance (override per-{@code BackendConfig} via {@code connsPerAddr}):</p>
     * <ul>
     *     <li>Low QPS / few peers: {@code 1~2} (saves resources);</li>
     *     <li>High QPS / few peers: {@code 8~16} (more EventLoop parallelism);</li>
     *     <li>Many peer IPs (hundreds+): leave at {@code 4}, the IP fan-out already
     *         provides parallelism &amp; failure isolation;</li>
     *     <li>Gateway / aggregation services: {@code 8}.</li>
     * </ul>
     *
     * <p><b>Compatibility note</b>: this default was {@code 2} in earlier versions. Upgrades
     * will see <b>per-peer inbound connections double</b> on the server side; review server
     * fd ulimits and LB per-source connection limits before rollout.</p>
     */
    public static final String DEFAULT_CONNECTIONS_PERADDR = "4";
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

    /**
     * Assemble plugin url prefix
     */
    public static final String ASSEMBLE_PLUGIN_URL_PREFIX = "assemble://";

    /**
     * Polaris plugin url prefix
     */
    public static final String POLARIS_PLUGIN_URL_PREFIX = "polaris://";

    /**
     * Polaris plugin set name key
     */
    public static final String POLARIS_PLUGIN_SET_NAME_KEY = "internal-set-name";

    /**
     * Polaris plugin enable set router key
     */
    public static final String POLARIS_PLUGIN_ENABLE_SET_KEY = "internal-enable-set";

    /**
     * Polaris plugin enable set value
     */
    public static final String POLARIS_PLUGIN_ENABLE_SET = "Y";
}
