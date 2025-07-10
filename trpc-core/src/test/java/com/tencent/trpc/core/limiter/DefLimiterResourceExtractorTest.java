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

package com.tencent.trpc.core.limiter;

import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.limiter.spi.LimiterResourceExtractor;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.def.DefRequest;
import org.junit.Assert;
import org.junit.Test;

public class DefLimiterResourceExtractorTest {

    @Test
    public void testExtractor() {
        DefLimiterResourceExtractor extractor = (DefLimiterResourceExtractor) ExtensionLoader
                .getExtensionLoader(LimiterResourceExtractor.class).getExtension("default");

        DefRequest request = new DefRequest();
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setFunc("/test/func");
        request.setInvocation(rpcInvocation);

        String resource = extractor.extract(null, request);
        Assert.assertEquals("/test/func", resource);
    }

}
