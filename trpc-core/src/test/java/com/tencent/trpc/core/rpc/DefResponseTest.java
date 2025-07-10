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

package com.tencent.trpc.core.rpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import java.util.concurrent.ConcurrentMap;
import org.junit.Assert;
import org.junit.Test;

public class DefResponseTest {

    @Test
    public void test() {
        DefResponse rsp = new DefResponse();
        ConcurrentMap<String, Object> newHashMap = Maps.newConcurrentMap();
        newHashMap.put("a", "b");
        rsp.setAttachments(newHashMap);
        Object rspHead = new Object();
        rsp.setAttachRspHead(rspHead);
        Exception ex = new Exception();
        rsp.setException(ex);
        DefRequest req = new DefRequest();
        rsp.setRequest(req);
        rsp.setRequestId(12);
        rsp.setValue(123);
        assertEquals(rsp.getAttachments().get("a"), "b");
        assertEquals(rsp.getAttachRspHead(), rspHead);
        assertEquals(rsp.getException(), ex);
        assertEquals(rsp.getRequest(), req);
        assertEquals(rsp.getRequestId(), 12);
        assertEquals(rsp.getValue(), 123);
        rsp.putAttachment("aa", "cc");
        assertEquals(rsp.getAttachment("aa"), "cc");
        rsp.removeAttachment("aa");
        assertNull(rsp.getAttachment("aa"));
        assertNotNull(rsp.getMeta());

        DefResponse defResponse = (DefResponse) rsp.clone();
        Assert.assertEquals(rsp.getAttachments().get("a"), defResponse.getAttachments().get("a"));
        Assert.assertEquals(rsp.getException(), defResponse.getException());
    }
}
