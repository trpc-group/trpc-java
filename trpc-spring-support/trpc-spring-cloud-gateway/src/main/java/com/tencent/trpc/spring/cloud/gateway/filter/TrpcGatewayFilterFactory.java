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

package com.tencent.trpc.spring.cloud.gateway.filter;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.spring.cloud.gateway.rewriter.TrpcRequestRewriter;
import com.tencent.trpc.spring.cloud.gateway.rewriter.TrpcResponseRewriter;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

/**
 * TrpcGatewayFilterFactory implements {@link AbstractGatewayFilterFactory}.
 * <p>As per the convention, the following code in the `routes` configuration in the `yml` file will execute the filter
 * logic in {@link TrpcRoutingFilter}.</p>
 * <pre>
 * filters:
 *   - TRPC=true, {"v1","1"}
 * </pre>
 * <p>Or configure like this:</p>
 * <pre>
 * filters:
 *   - name: TRPC
 *     args:
 *       enabled: true
 *       values: {"v1","1"}
 * </pre>
 * <p></p>
 * The `- name: TRPC` is a convention in SpringGateway and will automatically load the TRPCGatewayFilterFactory.
 */
@Component
public class TrpcGatewayFilterFactory extends AbstractGatewayFilterFactory<TrpcGatewayFilterFactory.Config> {

    private static final Logger logger = LoggerFactory.getLogger(TrpcRoutingFilter.class);
    private static final String[] params = {"enabled", "requestRewriter", "responseRewriter", "metadata"};

    @Autowired
    private TrpcRequestRewriter requestRewriter;
    @Autowired
    private TrpcResponseRewriter responseRewriter;

    public TrpcGatewayFilterFactory() {
        super(Config.class);
        logger.info("Loaded GatewayFilterFactory [TrpcRoutingFilter]");
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList(params);
    }

    @Override
    public GatewayFilter apply(TrpcGatewayFilterFactory.Config config) {
        logger.info("config.toString():" + config.toString());
        loadRequestRewriter(config);
        loadResponseRewriter(config);
        return new TrpcRoutingFilter(requestRewriter, responseRewriter, config);
    }

    private void loadRequestRewriter(Config config) {
        String requestRewriterClassName = config.getRequestRewriter();
        if (!StringUtils.isEmpty(requestRewriterClassName)) {
            try {
                Object o = Class.forName(requestRewriterClassName).newInstance();
                if (o instanceof TrpcRequestRewriter) {
                    requestRewriter = (TrpcRequestRewriter) o;
                }
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                throw new IllegalArgumentException("TrpcRequestRewriter subclass loading error or wrong class name:"
                        + requestRewriterClassName);
            }
        }
    }

    private void loadResponseRewriter(Config config) {
        String responseRewriterClassName = config.getResponseRewriter();
        if (!StringUtils.isEmpty(responseRewriterClassName)) {
            try {
                Object o = Class.forName(responseRewriterClassName).newInstance();
                if (o instanceof TrpcResponseRewriter) {
                    responseRewriter = (TrpcResponseRewriter) o;
                }
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                throw new IllegalArgumentException("TrpcRequestRewriter subclass loading error or wrong class name:"
                        + responseRewriterClassName);
            }
        }
    }

    /**
     * The official recommended usage, the specific configuration method can refer to {@link TrpcGatewayFilterFactory}.
     */
    public static class Config {

        /**
         * Filter is enabled by default.
         */
        private boolean enabled = true;

        private String requestRewriter;

        private String responseRewriter;

        private String metadata;

        public Config() {
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getRequestRewriter() {
            return requestRewriter;
        }

        public void setRequestRewriter(String requestRewriter) {
            this.requestRewriter = requestRewriter;
        }

        public String getResponseRewriter() {
            return responseRewriter;
        }

        public void setResponseRewriter(String responseRewriter) {
            this.responseRewriter = responseRewriter;
        }

        public String getMetadata() {
            return metadata;
        }

        public void setMetadata(String metadata) {
            this.metadata = metadata;
        }

        @Override
        public String toString() {
            return "Config{" + "enabled=" + enabled + ", requestRewriter='" + requestRewriter + '\''
                    + ", responseRewriter='" + responseRewriter + '\'' + ", metadata='" + metadata + '\'' + '}';
        }
    }

}