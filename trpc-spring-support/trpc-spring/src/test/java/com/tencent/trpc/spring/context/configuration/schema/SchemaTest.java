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

package com.tencent.trpc.spring.context.configuration.schema;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.Lists;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.spring.context.configuration.schema.client.ClientSchema;
import com.tencent.trpc.spring.context.configuration.schema.plugin.PluginsSchema;
import com.tencent.trpc.spring.context.configuration.schema.server.ServerSchema;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

public class SchemaTest {

    private static final ObjectMapper MAP_CONVERTER = JsonUtils.copy()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

    private static final TypeReference<Map<String, Object>> MAP_TYPE =
            new TypeReference<Map<String, Object>>() {
            };

    @Test
    public void test() {
        Binder binder = getBinder();

        assertBound(binder, "global", GlobalSchema.class);
        assertBound(binder, "server", ServerSchema.class);
        assertBound(binder, "client", ClientSchema.class);
        assertBound(binder, "plugins", PluginsSchema.class);
    }

    private void assertBound(Binder binder, String prefix, Class<?> clazz) {
        Object bound = bind(binder, prefix, clazz);
        Assert.assertNotNull(bound);

        Map<String, Object> map = MAP_CONVERTER.convertValue(bound, MAP_TYPE);
        Assert.assertTrue(map.size() > 0);
    }

    private Binder getBinder() {
        return Binder.get(loadEnvironment());
    }

    private <T> T bind(Binder binder, String prefix, Class<T> clazz) {
        return binder.bind(prefix, Bindable.of(clazz))
                .orElse(null);
    }

    private Environment loadEnvironment() {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        ResourceLoader resourceLoader = new DefaultResourceLoader();

        List<PropertySource<?>> propertySources;
        try {
            propertySources = loader
                    .load("test-source", resourceLoader.getResource("classpath:schema-test-trpc-config.yml"));
        } catch (IOException e) {
            propertySources = Lists.newArrayList();
        }

        StandardEnvironment environment = new StandardEnvironment();
        propertySources.forEach(environment.getPropertySources()::addLast);

        return environment;
    }
}
