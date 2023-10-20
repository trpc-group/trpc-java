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

package com.tencent.trpc.core.cluster;

import com.tencent.trpc.core.common.config.BackendConfig;

public interface RpcClusterClientFactory {

    /**
     * Create an instance of RpcClusterClient.
     * Select to create a standard or streaming RpcClusterClient
     * based on the protocolType in the ProtocolConfig of BackendConfig.
     *
     * @return RpcClusterClient
     */
    RpcClusterClient create(BackendConfig config);

}
