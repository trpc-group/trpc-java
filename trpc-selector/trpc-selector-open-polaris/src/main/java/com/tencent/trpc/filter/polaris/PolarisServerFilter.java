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

package com.tencent.trpc.filter.polaris;

import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.trpc.core.extension.Activate;
import com.tencent.trpc.core.extension.ActivationGroup;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.polaris.common.PolarisConstant;
import com.tencent.trpc.polaris.common.PolarisContextUtil;

import java.util.concurrent.CompletionStage;

@Activate(group = ActivationGroup.PROVIDER)
@Extension(value = "polaris_server")
public class PolarisServerFilter implements Filter {

    @Override
    public CompletionStage<Response> filter(Invoker<?> filterChain, Request req) {
        MetadataContext metadataContext = PolarisContextUtil.getMetadataContext(req);
        RpcContextUtils.putValueMapValue(req.getContext(), PolarisConstant.RPC_CONTEXT_POALRIS_METADATA, metadataContext);
        return filterChain.invoke(req);
    }

}
