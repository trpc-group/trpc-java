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

package com.tencent.trpc.core.rpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.def.DefRequest;
import java.util.concurrent.ConcurrentMap;
import org.junit.Assert;
import org.junit.Test;

public class DefRequestTest {

    private static final Logger logger = LoggerFactory.getLogger(DefRequestTest.class);

    @Test
    public void test() {
        DefRequest request = new DefRequest();
        ConcurrentMap<String, Object> newHashMap = Maps.newConcurrentMap();
        newHashMap.put("a", "b");
        request.setAttachments(newHashMap);
        request.setRequestId(12);
        assertEquals(request.getAttachments().get("a"), "b");
        assertEquals(request.getRequestId(), 12);
        request.putAttachment("aa", "cc");
        assertEquals(request.getAttachment("aa"), "cc");
        request.removeAttachment("aa");
        assertNull(request.getAttachment("aa"));
        assertNull(request.getAttachReqHead());
        DefRequest defRequest = (DefRequest) request.clone();
        Assert.assertEquals(request.getAttachments().get("a"),
                defRequest.getAttachments().get("a"));
        Assert.assertEquals(request.getRequestId(), defRequest.getRequestId());
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setRpcServiceName("/trpc.a");
        rpcInvocation.setRpcMethodName("/b");
        rpcInvocation.setFunc("/trpc.a/b");
        request.setInvocation(rpcInvocation);
        logger.debug("***request = {}", request);
    }
}
