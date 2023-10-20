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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import org.junit.Assert;
import org.junit.Test;

public class RequestMetaTest {

    private static CallInfo newCallInfo() {
        CallInfo callInfo = new CallInfo();
        callInfo.setCallee("callee");
        callInfo.setCalleeApp("calleeapp");
        callInfo.setCalleeServer("calleeserver");
        callInfo.setCalleeMethod("calleemethod");
        callInfo.setCalleeService("calleeservice");
        callInfo.setCaller("caller");
        callInfo.setCallerApp("callerapp");
        callInfo.setCallerServer("callerserver");
        callInfo.setCallerMethod("callermethod");
        callInfo.setCallerService("callerservice");
        return callInfo;
    }

    @Test
    public void test() {
        long currentTimeMillis = System.currentTimeMillis();
        RequestMeta meta = new RequestMeta();
        meta.setCallInfo(newCallInfo());
        ConsumerConfig cconfig = new ConsumerConfig();
        ProviderConfig pconfig = new ProviderConfig();
        meta.setConsumerConfig(cconfig);
        meta.setCreateTime(currentTimeMillis);
        meta.setDyeingKey("dyeingkey");
        meta.setHashVal("hashval");
        meta.setOneWay(true);
        meta.setProviderConfig(pconfig);
        meta.getMap().put("a", "b");
        meta.setMessageType(100);
        meta.setSize(20);
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(meta.isDyeing());
        assertTrue(meta.hasMessageType(100));
        assertEquals(20, meta.getSize());
        meta.addMessageType(1);
        assertTrue(meta.hasMessageType(100));
        RequestMeta copy = meta.clone();
        Assert.assertEquals(100, copy.getMessageType());
        assertNotSame(copy, meta);
        assertEquals(copy.getDyeingKey(), "dyeingkey");
        assertEquals(copy.getHashVal(), "hashval");
        Assert.assertEquals(cconfig, copy.getConsumerConfig());
        Assert.assertEquals(pconfig, copy.getProviderConfig());
        Assert.assertTrue(copy.getCreateTime() != 0 && copy.getCreateTime() > meta.getCreateTime());
        assertNotSame(copy.getCallInfo(), meta.getCallInfo());
        Assert.assertEquals("callee", copy.getCallInfo().getCallee());
        Assert.assertEquals("calleeapp", copy.getCallInfo().getCalleeApp());
        Assert.assertEquals("calleeserver", copy.getCallInfo().getCalleeServer());
        Assert.assertEquals("calleemethod", copy.getCallInfo().getCalleeMethod());
        Assert.assertEquals("calleeservice", copy.getCallInfo().getCalleeService());
        Assert.assertEquals("caller", copy.getCallInfo().getCaller());
        Assert.assertEquals("callerapp", copy.getCallInfo().getCallerApp());
        Assert.assertEquals("callerserver", copy.getCallInfo().getCallerServer());
        Assert.assertEquals("callermethod", copy.getCallInfo().getCallerMethod());
        Assert.assertEquals("callerservice", copy.getCallInfo().getCallerService());
        Assert.assertEquals(pconfig, copy.getProviderConfig());
        Assert.assertEquals(cconfig, copy.getConsumerConfig());
        Assert.assertEquals("b", copy.getMap().get("a"));
        Assert.assertTrue(copy.getMap() != meta.getMap());
    }
}
