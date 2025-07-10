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

package com.tencent.trpc.registry.transporter.curator;

import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.registry.common.RegistryCenterConfig;
import com.tencent.trpc.registry.transporter.AbstractZookeeperFactory;
import com.tencent.trpc.registry.transporter.ZookeeperClient;

/**
 * Curator client factory class
 */
@Extension("curator")
public class CuratorZookeeperFactory extends AbstractZookeeperFactory {

    @Override
    protected ZookeeperClient createClient(RegistryCenterConfig config) {
        return new CuratorZookeeperClient(config);
    }
}
