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

package com.tencent.trpc.core.selector.spi;

import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.router.support.NoneRouter;
import java.util.List;

@Extensible(NoneRouter.NAME)
public interface Router {

    /**
     * Route service instance.
     *
     * @param instances the instances
     * @param request the request
     * @return the comply with routing rules service instance
     * @throws TRpcException the t rpc exception if any exception happens
     */
    List<ServiceInstance> route(List<ServiceInstance> instances, Request request)
            throws TRpcException;

}
