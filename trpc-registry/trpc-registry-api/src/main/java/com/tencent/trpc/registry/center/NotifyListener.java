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

package com.tencent.trpc.registry.center;

import com.tencent.trpc.core.extension.DisposableExtension;
import com.tencent.trpc.core.registry.RegisterInfo;
import java.util.EventListener;
import java.util.List;

/**
 * Callback notification interface for subscribed services when data changes, mainly used for service discovery.
 * Inherits the destroy method, which needs to be overridden when necessary.
 */
public interface NotifyListener extends DisposableExtension, EventListener {

    /**
     * Callback interface for subscribed services when data changes.
     * The implementation needs to be processed according to different data types
     * {@link com.tencent.trpc.registry.common.RegistryCenterEnum}.
     *
     * @param registerInfos The subscribed data. When the data is empty, clear the serviceInstances.
     */
    void notify(List<RegisterInfo> registerInfos);

}
