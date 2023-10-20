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
 * Server-side context.
 */
@SuppressWarnings("unchecked")
public class RpcServerContext extends RpcContext implements Cloneable {

    /**
     * Used to copy the pass-through information (attach_info) received by RpcServerContext, or some business fields in
     * your own protocol that need to be passed through, such as user id, token, etc.
     * If not needed, the business party can manually create a specific clientContext.
     */
    public <T extends RpcClientContext> T newClientContext() {
        return newClientContext(new NewClientContextOptions());
    }

    public <T extends RpcClientContext> T newClientContext(NewClientContextOptions option) {
        RpcClientContext context = new RpcClientContext();
        cloneValueMapTo(context);
        cloneReqAttachMap(option, context);
        cloneCallInfo(option, context);
        // by default, do not copy attachments
        cloneRequestUncodecDataSegment(option, context);
        cloneResponseUncodecDataSegment(option, context);
        return (T) context;
    }

    /**
     * Copy RpcServerContext information, do not copy attachments by default, if you need to copy attachments, please
     * set it by the business party.
     *
     * @return RpcServerContext
     */
    public RpcServerContext clone() {
        RpcServerContext newRpcServerContext = new RpcServerContext();
        cloneTo(newRpcServerContext);
        return newRpcServerContext;
    }

    protected void cloneReqAttachMap(NewClientContextOptions option, RpcClientContext context) {
        if (!option.isCloneReqAttachMap()) {
            return;
        }
        cloneReqAttachTo(context);
        context.setDyeingKey(this.getDyeingKey());
    }

    protected void cloneCallInfo(NewClientContextOptions option, RpcClientContext context) {
        if (!option.isCloneCallInfo()) {
            return;
        }
        // server acts as the callee, when calling downstream in the link scenario, it is the caller information of
        // the client, directly converted.
        context.getCallInfo().setCaller(this.getCallInfo().getCallee());
        context.getCallInfo().setCallerApp(this.getCallInfo().getCalleeApp());
        context.getCallInfo().setCallerServer(this.getCallInfo().getCalleeServer());
        context.getCallInfo().setCallerService(this.getCallInfo().getCalleeService());
        context.getCallInfo().setCallerMethod(this.getCallInfo().getCalleeMethod());
    }

    protected void cloneRequestUncodecDataSegment(NewClientContextOptions options, RpcClientContext context) {
        if (!options.isCloneRequestUncodecDataSegment()) {
            return;
        }
        context.setRequestUncodecDataSegment(getRequestUncodecDataSegment());
    }

    protected void cloneResponseUncodecDataSegment(NewClientContextOptions options, RpcClientContext context) {
        if (!options.isCloneResponseUncodecDataSegment()) {
            return;
        }
        context.setResponseUncodecDataSegment(getResponseUncodecDataSegment());
    }

    public static class NewClientContextOptions {

        /**
         * Whether to clone reqAttachMap (additional parameters received by the server).
         */
        private boolean cloneReqAttachMap = true;
        /**
         * Whether to clone the caller and callee information.
         */
        private boolean cloneCallInfo = true;
        /**
         * Whether to copy requestUncodecDataSegment, not copied by default.
         */
        private boolean cloneRequestUncodecDataSegment = false;
        /**
         * Whether to copy responseUncodecDataSegment, not copied by default.
         */
        private boolean cloneResponseUncodecDataSegment = false;

        public static NewClientContextOptions newInstance() {
            return new NewClientContextOptions();
        }

        public boolean isCloneReqAttachMap() {
            return cloneReqAttachMap;
        }

        public NewClientContextOptions setCloneReqAttachMap(boolean cloneReqAttachMap) {
            this.cloneReqAttachMap = cloneReqAttachMap;
            return this;
        }

        public boolean isCloneCallInfo() {
            return cloneCallInfo;
        }

        public NewClientContextOptions setCloneCallInfo(boolean cloneCallInfo) {
            this.cloneCallInfo = cloneCallInfo;
            return this;
        }

        public boolean isCloneRequestUncodecDataSegment() {
            return cloneRequestUncodecDataSegment;
        }

        public NewClientContextOptions setCloneRequestUncodecDataSegment(boolean cloneRequestUncodecDataSegment) {
            this.cloneRequestUncodecDataSegment = cloneRequestUncodecDataSegment;
            return this;
        }

        public boolean isCloneResponseUncodecDataSegment() {
            return cloneResponseUncodecDataSegment;
        }

        public NewClientContextOptions setCloneResponseUncodecDataSegment(boolean cloneResponseUncodecDataSegment) {
            this.cloneResponseUncodecDataSegment = cloneResponseUncodecDataSegment;
            return this;
        }
    }

}
