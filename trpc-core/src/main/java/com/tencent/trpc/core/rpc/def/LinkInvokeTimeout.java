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

package com.tencent.trpc.core.rpc.def;

/**
 * Full link timeout.
 */
public class LinkInvokeTimeout {

    /**
     * Original timeout.
     */
    private final long timeout;
    /**
     * Request start time.
     */
    private final long startTime;
    /**
     * Remaining request time.
     */
    private long leftTimeout;
    /**
     * Whether the service enables link timeout.
     */
    private boolean serviceEnableLinkTimeout;

    private LinkInvokeTimeout(long timeout, long startTime, long leftTimeout, boolean serviceEnableLinkTimeout) {
        this.timeout = timeout;
        this.startTime = startTime;
        this.leftTimeout = leftTimeout;
        this.serviceEnableLinkTimeout = serviceEnableLinkTimeout;
    }

    public static LinkInvokeTimeoutBuilder builder() {
        return new LinkInvokeTimeoutBuilder();
    }

    public long getTimeout() {
        return timeout;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getLeftTimeout() {
        return leftTimeout;
    }

    public boolean isServiceEnableLinkTimeout() {
        return serviceEnableLinkTimeout;
    }

    @Override
    public String toString() {
        return "{"
                + "startTime="
                + startTime
                + ", timeout="
                + timeout
                + ", leftTimeout="
                + leftTimeout
                + ", serviceEnableLinkTimeout="
                + serviceEnableLinkTimeout
                + '}';
    }

    public static class LinkInvokeTimeoutBuilder {

        private long timeout;
        /**
         * Request start time.
         */
        private long startTime;
        /**
         * Remaining request time.
         */
        private long leftTimeout;
        /**
         * Whether the service enables link timeout.
         */
        private boolean serviceEnableLinkTimeout;

        private LinkInvokeTimeoutBuilder() {
        }

        public LinkInvokeTimeoutBuilder timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public LinkInvokeTimeoutBuilder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }

        public LinkInvokeTimeoutBuilder leftTimeout(long leftTimeout) {
            this.leftTimeout = leftTimeout;
            return this;
        }

        public LinkInvokeTimeoutBuilder serviceEnableLinkTimeout(boolean serviceEnableLinkTimeout) {
            this.serviceEnableLinkTimeout = serviceEnableLinkTimeout;
            return this;
        }

        public LinkInvokeTimeout build() {
            return new LinkInvokeTimeout(timeout, startTime, leftTimeout, serviceEnableLinkTimeout);
        }
    }

}
