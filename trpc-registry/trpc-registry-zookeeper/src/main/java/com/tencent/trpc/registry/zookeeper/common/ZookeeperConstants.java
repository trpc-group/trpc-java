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

package com.tencent.trpc.registry.zookeeper.common;

/**
 * Zookeeper constants configuration class
 */
public class ZookeeperConstants {

    /**
     * Splitter for zookeeper registry path
     */
    public static final String ZK_PATH_SEPARATOR = "/";

    /**
     * Zookeeper registry namespace key
     */
    public static final String NAMESPACE_KEY = "namespace";

    /**
     * Zookeeper registry default namespace
     */
    public static final String DEFAULT_NAMESPACE = "trpc";

}
