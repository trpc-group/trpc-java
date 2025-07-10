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

import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.registry.common.RegistryCenterConfig;

/**
 * Default use the curator framework-based client, if you want to use other zk client, you can inherit the
 * interface implementation
 */
@Extensible("curator")
public interface ZookeeperFactory {

    /**
     * Get zk client
     *
     * @param config zk client connection configuration
     * @return zk client
     */
    ZookeeperClient connect(RegistryCenterConfig config);

}
