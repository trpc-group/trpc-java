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

import java.util.Map;

/**
 * Implementation of {@link FreeMarkerContextProvider} that adapts FreeMarker
 */
public class FreeMarkerContextProvider implements TemplateContextProvider<Map<String, Object>> {
    /**
     * {@inheritDoc}
     */
    @Override
    public TemplateContext<Map<String, Object>> createContext() {
        return new FreeMarkerContext();
    }
}
