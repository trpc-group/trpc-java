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

/**
 * Caller and callee information.
 */
public class CallInfo implements Cloneable {

    private String caller = "";
    /**
     * Caller application name.
     */
    private String callerApp = "";
    /**
     * Caller server name.
     */
    private String callerServer = "";
    /**
     * Caller service.
     */
    private String callerService = "";
    /**
     * Caller method name.
     */
    private String callerMethod = "";
    /**
     * Caller container name.
     */
    private String callerContainerName = "";
    /**
     * Caller set name.
     */
    private String callerSetName = "";
    private String callee = "";
    /**
     * Callee application name.
     */
    private String calleeApp = "";
    /**
     * Callee server name.
     */
    private String calleeServer = "";
    /**
     * Callee service.
     */
    private String calleeService = "";
    /**
     * Callee method name.
     */
    private String calleeMethod = "";
    /**
     * Callee container name.
     */
    private String calleeContainerName = "";
    /**
     * Callee set name.
     */
    private String calleeSetName = "";

    public CallInfo clone() {
        CallInfo clone = null;
        try {
            clone = (CallInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("", e);
        }
        return clone;
    }

    @Override
    public String toString() {
        return "CallInfo {caller=" + caller + ", callerApp=" + callerApp + ", callerServer="
                + callerServer + ", callerService=" + callerService + ", callerMethod="
                + callerMethod
                + ", callee=" + callee + ", calleeApp=" + calleeApp + ", calleeServer="
                + calleeServer
                + ", calleeService=" + calleeService + ", calleeMethod=" + calleeMethod + "}";
    }

    public String getCaller() {
        return caller;
    }

    public CallInfo setCaller(String caller) {
        this.caller = caller != null ? caller : "";
        return this;
    }

    public String getCallee() {
        return callee;
    }

    public CallInfo setCallee(String callee) {
        this.callee = callee != null ? callee : "";
        return this;
    }

    public String getCalleeApp() {
        return calleeApp;
    }

    public CallInfo setCalleeApp(String calleeApp) {
        this.calleeApp = calleeApp != null ? calleeApp : "";
        return this;
    }

    public String getCallerApp() {
        return callerApp;
    }

    public CallInfo setCallerApp(String callerApp) {
        this.callerApp = callerApp != null ? callerApp : "";
        return this;
    }

    public String getCallerServer() {
        return callerServer;
    }

    public CallInfo setCallerServer(String callerServer) {
        this.callerServer = callerServer != null ? callerServer : "";
        return this;
    }

    public String getCallerService() {
        return callerService;
    }

    public CallInfo setCallerService(String callerService) {
        this.callerService = callerService != null ? callerService : "";
        return this;
    }

    public String getCallerMethod() {
        return callerMethod;
    }

    public CallInfo setCallerMethod(String callerMethod) {
        this.callerMethod = callerMethod != null ? callerMethod : "";
        return this;
    }

    public String getCallerContainerName() {
        return callerContainerName;
    }

    public void setCallerContainerName(String callerContainerName) {
        this.callerContainerName = callerContainerName;
    }

    public String getCallerSetName() {
        return callerSetName;
    }

    public void setCallerSetName(String callerSetName) {
        this.callerSetName = callerSetName;
    }

    public String getCalleeServer() {
        return calleeServer;
    }

    public CallInfo setCalleeServer(String calleeServer) {
        this.calleeServer = calleeServer != null ? calleeServer : "";
        return this;
    }

    public String getCalleeService() {
        return calleeService;
    }

    public CallInfo setCalleeService(String calleeService) {
        this.calleeService = calleeService != null ? calleeService : "";
        return this;
    }

    public String getCalleeMethod() {
        return calleeMethod;
    }

    public CallInfo setCalleeMethod(String calleeMethod) {
        this.calleeMethod = calleeMethod != null ? calleeMethod : "";
        return this;
    }

    public String getCalleeContainerName() {
        return calleeContainerName;
    }

    public void setCalleeContainerName(String calleeContainerName) {
        this.calleeContainerName = calleeContainerName;
    }

    public String getCalleeSetName() {
        return calleeSetName;
    }

    public void setCalleeSetName(String calleeSetName) {
        this.calleeSetName = calleeSetName;
    }

}
