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

import java.util.List;

/**
 * Zookeeper child node change listener to listen for changes to the node's children
 */
public interface ChildListener {

    /**
     * Callback interface for child node changes
     *
     * @param path The path of the listener node
     * @param children The names of all children under the listener's node
     */
    void childChanged(String path, List<String> children);

}
