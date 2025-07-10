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

package com.tencent.trpc.registry.transporter;


import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener.Type;

/**
 * Zookeeper node content change listener, callback when node content changes
 */
public interface DataListener {

    /**
     * Callback interface for node content changes
     *
     * @param type Node change type, create, update, delete
     * @param oldData old node data
     * @param data New node data
     */
    void dataChanged(Type type, ChildData oldData, ChildData data);
}
