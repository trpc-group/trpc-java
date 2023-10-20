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

package com.tencent.trpc.proto.http.common;

import com.google.common.collect.Sets;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.core.utils.ClassUtils;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.http.HttpHeaders;

/**
 * Http's RpcServerContext can be modified to transform the ClientContext logic for transparently passing
 * through the request context, and add the functionality to remove regular HTTP headers to avoid causing
 * HTTP call exceptions or interfering with downstream services.
 */
public class RpcServerContextWithHttp extends RpcServerContext {

    /**
     * The HTTP headers that must be removed can cause HTTP call exceptions.
     */
    private static final Set<String> MUST_REMOVE_HTTP_HEADERS;

    /**
     * The regular HTTP headers that can be removed are removed by default.
     * They can be disabled by using {@link NewClientContextOptionsWithHttp#removeCommonHttpHeaders}.
     */
    private static final Set<String> COMMON_HTTP_HEADERS;

    static {
        MUST_REMOVE_HTTP_HEADERS = Sets.newHashSet(HttpHeaders.CONTENT_LENGTH.toLowerCase(),
                HttpHeaders.TRANSFER_ENCODING.toLowerCase());
        COMMON_HTTP_HEADERS = ClassUtils.getConstantValues(HttpHeaders.class).stream()
                .filter(obj -> obj instanceof String)
                .map(String::valueOf)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Override
    public <T extends RpcClientContext> T newClientContext() {
        return newClientContext(new NewClientContextOptionsWithHttp());
    }

    @Override
    protected void cloneReqAttachMap(NewClientContextOptions option, RpcClientContext context) {
        if (!option.isCloneReqAttachMap()) {
            return;
        }
        if (option instanceof NewClientContextOptionsWithHttp) {
            cloneReqAttachTo(context, (NewClientContextOptionsWithHttp) option);
        } else {
            cloneReqAttachTo(context);
        }
        context.setDyeingKey(this.getDyeingKey());
    }

    /**
     * Copy the attachments of the request to the context.
     *
     * @param context rpc context
     * @param option the options used to convert ServerContext to ClientContext
     */
    private void cloneReqAttachTo(RpcContext context, NewClientContextOptionsWithHttp option) {
        if (this.getReqAttachMap().isEmpty()) {
            return;
        }
        this.getReqAttachMap().forEach((k, v) -> {
            String lowerK = k.toLowerCase();
            if (option.removeCommonHttpHeaders && COMMON_HTTP_HEADERS.contains(lowerK)
                    || MUST_REMOVE_HTTP_HEADERS.contains(lowerK) || option.getRemoveHttpHeaders().contains(lowerK)) {
                return;
            }
            context.getReqAttachMap().put(k, v);
        });
    }

    /**
     * Convert ServerContext to ClientContext configuration options.
     */
    public static class NewClientContextOptionsWithHttp extends NewClientContextOptions {

        /**
         * Whether to remove common HTTP Headers.
         * Passing COMMON_HTTP_HEADERS downstream may cause call exceptions or interfere with downstream operations.
         */
        private boolean removeCommonHttpHeaders = true;

        /**
         * Whether to remove other Http Headers.
         */
        private Set<String> removeHttpHeaders = Collections.emptySet();

        public static NewClientContextOptionsWithHttp newInstance() {
            return new NewClientContextOptionsWithHttp();
        }

        public boolean isRemoveCommonHttpHeaders() {
            return removeCommonHttpHeaders;
        }

        public NewClientContextOptionsWithHttp setRemoveCommonHttpHeaders(boolean removeCommonHttpHeaders) {
            this.removeCommonHttpHeaders = removeCommonHttpHeaders;
            return this;
        }

        public Set<String> getRemoveHttpHeaders() {
            return removeHttpHeaders;
        }

        public NewClientContextOptionsWithHttp setRemoveHttpHeaders(Set<String> removeHttpHeaders) {
            this.removeHttpHeaders = removeHttpHeaders;
            return this;
        }
    }

}
