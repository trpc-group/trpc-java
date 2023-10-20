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

package com.tencent.trpc.spring.context.configuration;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ClientConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.utils.CollectionUtils;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Implementation of {@link TRpcConfigManagerCustomizer} that allow user to config tRPC filters programmatically.
 * <p>Equivalent to the {@code trpc.server.filters} and {@code trpc.client.filters} configuration in trpc.yaml.
 * <p>Example:
 * <pre>
 *    {@literal @}Configuration
 *     public class BizConfiguration {
 *        {@literal @}Bean
 *         public AddFilterTRpcConfigManagerCustomizer addFilterTRpcConfigManagerCustomizer() {
 *             return new AddFilterTRpcConfigManagerCustomizer()
 *                     .addClientFilters(ADDITIONAL_CLIENT_FILTER)
 *                     .addServerFilters(ADDITIONAL_SERVER_FILTER);
 *         }
 *     }
 * </pre>
 */
public class AddFilterTRpcConfigManagerCustomizer implements TRpcConfigManagerCustomizer {

    private final List<String> clientFilters;

    private final List<String> serverFilters;

    public AddFilterTRpcConfigManagerCustomizer() {
        this(null, null);
    }

    public AddFilterTRpcConfigManagerCustomizer(List<String> clientFilters,
            List<String> serverFilters) {
        this.clientFilters = emptyIfNull(clientFilters);
        this.serverFilters = emptyIfNull(serverFilters);
    }

    private static List<String> emptyIfNull(List<String> list) {
        return Optional.ofNullable(list).orElseGet(Lists::newArrayList);
    }

    public AddFilterTRpcConfigManagerCustomizer addClientFilters(String... filters) {
        Collections.addAll(clientFilters, filters);
        return this;
    }

    public AddFilterTRpcConfigManagerCustomizer addServerFilters(String... filters) {
        Collections.addAll(serverFilters, filters);
        return this;
    }

    @Override
    public void customize(ConfigManager configManager) {
        customize(configManager.getClientConfig());
        customize(configManager.getServerConfig());
    }

    private void customize(ClientConfig clientConfig) {
        if (clientFilters.isEmpty()) {
            return;
        }

        clientConfig.getBackendConfigMap().values().forEach(backendConfig -> customize(clientConfig, backendConfig));
    }

    private void customize(ClientConfig clientConfig, BackendConfig backendConfig) {
        List<String> merged = CollectionUtils.merge(this::merge, clientFilters, clientConfig.getFilters(),
                backendConfig.getFilters());
        backendConfig.setFilters(merged);
    }

    private void customize(ServerConfig serverConfig) {
        if (serverFilters.isEmpty()) {
            return;
        }

        serverConfig.getServiceMap().values().forEach(serviceConfig -> customize(serverConfig, serviceConfig));
    }

    private void customize(ServerConfig serverConfig, ServiceConfig serviceConfig) {
        List<String> merged = CollectionUtils.merge(this::merge, serverFilters, serverConfig.getFilters(),
                serviceConfig.getFilters());
        serviceConfig.setFilters(merged);
    }

    /**
     * Merge adding filters to currently configured filters.
     * Existing filter takes precedence when duplicate filter names detected.
     */
    private List<String> merge(List<String> prior, List<String> origin) {
        Set<String> originSet = Sets.newHashSetWithExpectedSize(CollectionUtils.size(origin));
        LinkedList<String> merged = Lists.newLinkedList();
        emptyIfNull(origin).stream().peek(originSet::add).forEach(merged::addLast);
        emptyIfNull(prior).stream()
                .filter(((Predicate<String>) originSet::contains).negate())
                .forEach(merged::addFirst);
        return merged;
    }
}
