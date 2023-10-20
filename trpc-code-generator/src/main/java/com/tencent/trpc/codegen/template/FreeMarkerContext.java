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

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link TemplateContext} that adapts FreeMarker
 */
public class FreeMarkerContext implements TemplateContext<Map<String, Object>> {
    private final Map<String, Object> context = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getContext() {
        return context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(String key, Object value) {
        context.put(key, value);
    }
}
