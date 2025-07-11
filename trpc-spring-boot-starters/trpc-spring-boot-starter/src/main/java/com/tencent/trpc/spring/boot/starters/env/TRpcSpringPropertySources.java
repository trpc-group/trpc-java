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

package com.tencent.trpc.spring.boot.starters.env;

public interface TRpcSpringPropertySources {

    /**
     * User-defined configuration, specified by -Dtrpc_conf_path=/path/to/file.
     */
    String CUSTOM = "TRpcCustomPropertySources";

    /**
     * TRpc system default-loaded configuration files.
     *
     * @see TRpcConfigurationEnvironmentPostProcessor#DEFAULT_PROPERTY_SOURCE_LOCATIONS
     */
    String DEFAULT = "TRpcDefaultPropertySources";
}