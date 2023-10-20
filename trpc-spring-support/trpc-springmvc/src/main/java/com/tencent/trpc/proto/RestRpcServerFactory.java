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

package com.tencent.trpc.proto;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.AbstractRpcServer;
import com.tencent.trpc.core.rpc.AbstractRpcServerFactory;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.rpc.RpcServer;

/**
 * As REST is implemented through Spring MVC, here is an empty REST factory provided for the TRPC framework to use.
 */
public class RestRpcServerFactory extends AbstractRpcServerFactory {

    @Override
    public RpcServer createRpcServer(ProtocolConfig config) throws TRpcException {
        return new AbstractRpcServer() {
            {
                setConfig(config);
            }

            @Override
            protected <T> void doExport(ProviderInvoker<T> invoker) {
                // no-op
            }

            @Override
            protected <T> void doUnExport(ProviderConfig<T> config) {
                // no-op
            }

            @Override
            protected void doOpen() throws Exception {
                // no-op
            }

            @Override
            protected void doClose() {
                // no-op
            }

        };
    }

}
