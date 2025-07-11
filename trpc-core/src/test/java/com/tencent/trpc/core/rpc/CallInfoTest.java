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

import org.junit.Assert;
import org.junit.Test;

public class CallInfoTest {

    @Test
    public void test() {
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
        callInfo.setCalleeContainerName("calleeContainerName");
        callInfo.setCalleeSetName("calleeSetName");
        Assert.assertEquals("callee", callInfo.getCallee());
        Assert.assertEquals("calleeapp", callInfo.getCalleeApp());
        Assert.assertEquals("calleeserver", callInfo.getCalleeServer());
        Assert.assertEquals("calleemethod", callInfo.getCalleeMethod());
        Assert.assertEquals("calleeservice", callInfo.getCalleeService());
        Assert.assertEquals("caller", callInfo.getCaller());
        Assert.assertEquals("callerapp", callInfo.getCallerApp());
        Assert.assertEquals("callerserver", callInfo.getCallerServer());
        Assert.assertEquals("callermethod", callInfo.getCallerMethod());
        Assert.assertEquals("callerservice", callInfo.getCallerService());
        Assert.assertEquals("calleeContainerName", callInfo.getCalleeContainerName());
        Assert.assertEquals("calleeSetName", callInfo.getCalleeSetName());

        CallInfo newCallInfo = callInfo.clone();
        Assert.assertEquals("callee", newCallInfo.getCallee());
        Assert.assertEquals("calleeapp", newCallInfo.getCalleeApp());
        Assert.assertEquals("calleeserver", newCallInfo.getCalleeServer());
        Assert.assertEquals("calleemethod", newCallInfo.getCalleeMethod());
        Assert.assertEquals("calleeservice", newCallInfo.getCalleeService());
        Assert.assertEquals("caller", newCallInfo.getCaller());
        Assert.assertEquals("callerapp", newCallInfo.getCallerApp());
        Assert.assertEquals("callerserver", newCallInfo.getCallerServer());
        Assert.assertEquals("callermethod", newCallInfo.getCallerMethod());
        Assert.assertEquals("callerservice", newCallInfo.getCallerService());
        Assert.assertEquals("calleeContainerName", callInfo.getCalleeContainerName());
        Assert.assertEquals("calleeSetName", callInfo.getCalleeSetName());
    }
}
