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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        Assertions.assertEquals("callee", callInfo.getCallee());
        Assertions.assertEquals("calleeapp", callInfo.getCalleeApp());
        Assertions.assertEquals("calleeserver", callInfo.getCalleeServer());
        Assertions.assertEquals("calleemethod", callInfo.getCalleeMethod());
        Assertions.assertEquals("calleeservice", callInfo.getCalleeService());
        Assertions.assertEquals("caller", callInfo.getCaller());
        Assertions.assertEquals("callerapp", callInfo.getCallerApp());
        Assertions.assertEquals("callerserver", callInfo.getCallerServer());
        Assertions.assertEquals("callermethod", callInfo.getCallerMethod());
        Assertions.assertEquals("callerservice", callInfo.getCallerService());
        Assertions.assertEquals("calleeContainerName", callInfo.getCalleeContainerName());
        Assertions.assertEquals("calleeSetName", callInfo.getCalleeSetName());

        CallInfo newCallInfo = callInfo.clone();
        Assertions.assertEquals("callee", newCallInfo.getCallee());
        Assertions.assertEquals("calleeapp", newCallInfo.getCalleeApp());
        Assertions.assertEquals("calleeserver", newCallInfo.getCalleeServer());
        Assertions.assertEquals("calleemethod", newCallInfo.getCalleeMethod());
        Assertions.assertEquals("calleeservice", newCallInfo.getCalleeService());
        Assertions.assertEquals("caller", newCallInfo.getCaller());
        Assertions.assertEquals("callerapp", newCallInfo.getCallerApp());
        Assertions.assertEquals("callerserver", newCallInfo.getCallerServer());
        Assertions.assertEquals("callermethod", newCallInfo.getCallerMethod());
        Assertions.assertEquals("callerservice", newCallInfo.getCallerService());
        Assertions.assertEquals("calleeContainerName", callInfo.getCalleeContainerName());
        Assertions.assertEquals("calleeSetName", callInfo.getCalleeSetName());
    }
}
