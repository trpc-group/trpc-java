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

package com.tencent.trpc.core.metrics;

import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.RequestMeta;
import java.net.InetSocketAddress;

@Deprecated
public class PassiveInvocation {

    private final String passiveService;
    private final String passiveMethodName;
    private String passiveExtension;
    private final MetricsCallInfo activeCallInfo;

    public PassiveInvocation(String activeIp, String passiveService) {
        this(passiveService, "", new MetricsCallInfo("", "", "", "",
                activeIp, "", ""));
    }

    public PassiveInvocation(String passiveService, String passiveInterface, MetricsCallInfo activeCallInfo) {
        this.passiveService = passiveService;
        this.passiveMethodName = passiveInterface;
        this.activeCallInfo = activeCallInfo;
    }

    public static PassiveInvocation createPassiveInvocation(RequestMeta requestMeta) {
        CallInfo callInfo = requestMeta.getCallInfo();
        String aContainerName = callInfo.getCallerContainerName();
        String aSetName = callInfo.getCallerSetName();
        InetSocketAddress remoteAddress = requestMeta.getRemoteAddress();
        MetricsCallInfo activeCallInfo = new MetricsCallInfo(callInfo.getCallerApp(), callInfo.getCallerServer(),
                callInfo.getCallerService(), callInfo.getCallerMethod(),
                remoteAddress == null ? "" : remoteAddress.getHostString(),
                aContainerName == null ? "" : aContainerName, aSetName == null ? "" : aSetName);
        return new PassiveInvocation(callInfo.getCalleeService(), callInfo.getCalleeMethod(), activeCallInfo);
    }

    public String getPassiveService() {
        return passiveService;
    }

    public String getPassiveMethodName() {
        return passiveMethodName;
    }

    public String getPassiveExtension() {
        return passiveExtension;
    }

    public void setPassiveExtension(String passiveExtension) {
        this.passiveExtension = passiveExtension;
    }

    public MetricsCallInfo getActiveCallInfo() {
        return activeCallInfo;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("PassiveInvocation{");
        sb.append("passiveService='").append(passiveService).append('\'');
        sb.append(", passiveInterface='").append(passiveMethodName).append('\'');
        sb.append(", passiveExtension='").append(passiveExtension).append('\'');
        sb.append(", activeCallInfo=").append(activeCallInfo);
        sb.append('}');
        return sb.toString();
    }

}