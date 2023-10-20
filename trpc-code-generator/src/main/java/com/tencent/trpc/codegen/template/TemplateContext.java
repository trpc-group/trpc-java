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

package com.tencent.trpc.codegen.template;

/**
 * Define abilities of TemplateContext
 *
 * @param <C> actual type of the context object
 */
public interface TemplateContext<C> {
    /**
     * Get the context object
     *
     * @return the context object
     */
    C getContext();

    /**
     * set variable to the context
     *
     * @param key key
     * @param value value
     */
    void put(String key, Object value);
}
