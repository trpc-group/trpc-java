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

package com.tencent.trpc.container.config.system.parser;

import com.tencent.trpc.core.extension.Extensible;
import java.util.Map;

/**
 * Property source parser interface.
 */
@Extensible("default")
public interface PropertySourceParser {

    /**
     * Generate a multi-level structured map.
     *
     * @param source source property map
     * @return multi-level structured map
     */
    Map<String, Object> getFlattableMap(Map<String, Object> source);

    /**
     * Parse a multi-level structured map, convert it to a single-level map.
     *
     * @param source source property map
     * @return single-level structured map
     */
    Map<String, Object> parseFlattableMap(Map<String, Object> source);

}
