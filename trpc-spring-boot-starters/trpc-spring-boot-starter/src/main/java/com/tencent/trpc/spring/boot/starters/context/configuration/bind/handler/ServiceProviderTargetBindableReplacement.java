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

package com.tencent.trpc.spring.boot.starters.context.configuration.bind.handler;

import com.tencent.trpc.container.config.YamlUtils;
import com.tencent.trpc.container.config.yaml.ServiceConfigParser;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.spring.boot.starters.context.configuration.TRpcConfigurationProperties;
import com.tencent.trpc.spring.context.configuration.schema.server.ServerServiceSchema;
import com.tencent.trpc.spring.context.configuration.schema.server.ServiceProviderSchema;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;

/**
 * Support for binding trpc.server.service[].impls[] to both {@code String[]} and {@code ServiceProviderSchema[]}
 *
 * @see ServiceConfigParser#parseProviderConfig(YamlUtils, Map)
 * @see #isNestedServerImplsBinding(ConfigurationPropertyName, BindContext)
 * @see ServerServiceSchema
 * @see ServiceProviderSchema
 * @see ProviderConfig
 */
public class ServiceProviderTargetBindableReplacement implements TargetBindableReplacement {

    /**
     * Regex pattern to detect trpc.server.service[*].impls[*]
     */
    private static final Pattern SERVER_IMPLS_PATTERN = Pattern.compile(
            "^" + TRpcConfigurationProperties.PREFIX + "\\.server\\.service\\[[0-9]+]\\.impls\\[[0-9]+]$");

    /**
     * For {@code ServiceProviderSchema[]} type bindings, change the type dynamically to this type
     */
    private static final Bindable<ServiceProviderSchema> BINDABLE_SERVICE_PROVIDER =
            Bindable.of(ServiceProviderSchema.class);

    @SuppressWarnings("unchecked")
    @Override
    public <T> Bindable<T> tryReplace(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
        if (isNestedServerImplsBinding(name, context)) {
            return (Bindable<T>) BINDABLE_SERVICE_PROVIDER;
        }
        return null;
    }

    /**
     * Determines if the binding should be a {@code ServiceProviderSchema[]}. The logic is as follows:
     * <ul>
     *     <li>The current binding is the parent of impls, such as trpc.server.service[0].impls[0]</li>
     *     <li>If any source contains trpc.server.service[0].impls[0] (if it has children, it will not contain it),
     *         then it is considered a {@code String[]}</li>
     * </ul>
     *
     * @param name Configuration name
     * @param context Binding context
     * @return true/false
     */
    private boolean isNestedServerImplsBinding(ConfigurationPropertyName name, BindContext context) {
        return isServerImplsBinding(name) && notContainsProperty(name, context);
    }

    /**
     * Checks if a configuration property is trpc.server.service[].impls[]
     *
     * @param name Configuration name
     * @return true/false
     */
    private boolean isServerImplsBinding(ConfigurationPropertyName name) {
        Matcher matcher = SERVER_IMPLS_PATTERN.matcher(name.toString());
        return matcher.matches();
    }

    private boolean notContainsProperty(ConfigurationPropertyName name, BindContext context) {
        for (ConfigurationPropertySource source : context.getSources()) {
            if (source.getConfigurationProperty(name) != null) {
                return false;
            }
        }
        return true;
    }
}
