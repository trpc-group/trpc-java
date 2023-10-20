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

package com.tencent.trpc.core.extension;

/**
 * Plugin activation grouping.
 */
public interface ActivationGroup {

    /**
     * Indicates the plugin used on the PROVIDER side.
     */
    String PROVIDER = "PROVIDER";

    /**
     * Indicates the plugin used on the CONSUMER side.
     */
    String CONSUMER = "CONSUMER";

}