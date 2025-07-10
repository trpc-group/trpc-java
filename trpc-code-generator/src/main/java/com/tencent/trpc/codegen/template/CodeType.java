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

package com.tencent.trpc.codegen.template;

/**
 * Type of the generated API interface
 */
public enum CodeType {
    /**
     * Standard API interface
     */
    API,
    /**
     * Async API interface
     */
    ASYNC_API,
    /**
     * Stream API interface
     */
    STREAM_API,
    /**
     * pom.xml
     */
    POM_XML,
    /**
     * User custom
     */
    CUSTOM
}
