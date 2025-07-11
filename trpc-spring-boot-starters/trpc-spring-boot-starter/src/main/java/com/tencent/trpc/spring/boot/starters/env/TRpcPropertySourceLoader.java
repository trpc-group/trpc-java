/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 Tencent.
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.spring.boot.starters.env;

import com.google.common.collect.Maps;
import com.tencent.trpc.spring.boot.starters.context.configuration.TRpcConfigurationProperties;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * Mainly used for loading TRpc related configurations from the file system
 *
 * @see TRpcConfigurationEnvironmentPostProcessor TRpcConfigurationEnvironmentPostProcessor
 */
public class TRpcPropertySourceLoader {

    private static final String PREFIX = TRpcConfigurationProperties.PREFIX + ".";

    private static final Comparator<PropertySource<?>> DEFAULT_COMPARATOR = (f, s) -> 0;

    private final Log logger;

    private final ResourceLoader resourceLoader;

    private final List<PropertySourceLoader> propertySourceLoaders;

    public TRpcPropertySourceLoader(Log logger, ResourceLoader resourceLoader) {
        this.logger = logger;
        this.resourceLoader = resourceLoader == null ? new DefaultResourceLoader() : resourceLoader;
        this.propertySourceLoaders = SpringFactoriesLoader.loadFactories(PropertySourceLoader.class,
                TRpcPropertySourceLoader.class.getClassLoader());
    }

    /**
     * Load configurations and merge them
     *
     * @param name The name of the merged PropertySource
     * @param locations Configuration file locations
     * @return Merged PropertySource
     * @see OriginTrackedCompositePropertySource
     */
    public CompositePropertySource loadAndCompose(String name, String... locations) {
        List<PropertySource<?>> loaded = load(locations);
        OriginTrackedCompositePropertySource target = new OriginTrackedCompositePropertySource(name, true);
        loaded.forEach(target::addPropertySource);
        return target;
    }

    /**
     * Load configurations from the specified locations and store them in a PropertySource list
     *
     * @param locations Configuration file locations
     * @return PropertySource list
     */
    public List<PropertySource<?>> load(String... locations) {
        if (ArrayUtils.isEmpty(locations)) {
            return Collections.emptyList();
        }
        return Arrays.stream(locations)
                .filter(StringUtils::isNotBlank)
                .map(this::doLoad)
                .flatMap(Collection::stream)
                .map(this::addPrefix)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<PropertySource<?>> doLoad(String location) {
        for (PropertySourceLoader propertySourceLoader : propertySourceLoaders) {
            if (canLoad(location, propertySourceLoader)) {
                return doLoad(location, propertySourceLoader);
            }
        }
        // immutable empty list
        return Collections.emptyList();
    }

    private List<PropertySource<?>> doLoad(String location, PropertySourceLoader propertySourceLoader) {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists() || !resource.isReadable()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Could not load trpc configuration from " + resource.getDescription()
                        + ": not a readable file");
            }
            return Collections.emptyList();
        }

        try {
            return propertySourceLoader.load(location, resource);
        } catch (IOException e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Could not load trpc configuration from " + resource.getDescription() + ": "
                        + e.getMessage());
            }
            return Collections.emptyList();
        }
    }

    private boolean canLoad(String location, PropertySourceLoader propertySourceLoader) {
        return Arrays.stream(propertySourceLoader.getFileExtensions())
                .anyMatch(extension -> StringUtils.endsWithIgnoreCase(location, extension));
    }

    /**
     * Prefixes all keys in the source with {@link #PREFIX} to prevent pollution of variables from other sources
     *
     * @param propertySource Loaded trpc source
     * @return Source with prefix added
     */
    private PropertySource<?> addPrefix(PropertySource<?> propertySource) {
        if (!(propertySource instanceof EnumerablePropertySource)) {
            return null;
        }
        EnumerablePropertySource<?> source = (EnumerablePropertySource<?>) propertySource;
        Map<String, Object> properties = Arrays.stream(source.getPropertyNames())
                .collect(Collectors.toMap(this::applyPrefix,
                        name -> getOriginValue(source, name),
                        (o, n) -> n, // use new value if conflict (should never get here)
                        Maps::newLinkedHashMap));

        if (source instanceof OriginLookup) {
            return new OriginTrackedMapPropertySource(source.getName(), properties,
                    ((OriginTrackedMapPropertySource) source).isImmutable());
        }
        return new MapPropertySource(source.getName(), properties);
    }

    private String applyPrefix(String propertyName) {
        if (StringUtils.startsWith(propertyName, PREFIX)) {
            return propertyName;
        }
        return PREFIX + propertyName;
    }

    private Object getOriginValue(PropertySource<?> propertySource, String propertyName) {
        Origin origin = null;
        Object value = propertySource.getProperty(propertyName);
        if (propertySource instanceof OriginLookup) {
            origin = OriginLookup.getOrigin(propertySource, propertyName);
        }
        return OriginTrackedValue.of(value, origin);
    }

    /**
     * Loads configuration files in the order specified by {@code locations} and adds them to the end of the environment.
     *
     * @param environment Spring environment information
     * @param locations   Configuration file locations
     */
    public void loadInto(ConfigurableEnvironment environment, String... locations) {
        loadInto(environment, DEFAULT_COMPARATOR, locations);
    }

    /**
     * Loads configuration files, sorts them using the provided comparator, and adds them to the end of the environment.
     *
     * @param environment Spring environment information
     * @param comparator  Custom sorting
     * @param locations   Configuration file locations
     */
    public void loadInto(ConfigurableEnvironment environment, Comparator<PropertySource<?>> comparator,
            String... locations) {
        List<PropertySource<?>> propertySources = load(locations);
        propertySources.stream().sorted(comparator).forEach(environment.getPropertySources()::addLast);
    }

}
