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

package com.tencent.trpc.spring.boot.starters.context.configuration.bind;

import com.google.common.collect.Maps;
import com.tencent.trpc.spring.boot.starters.context.configuration.bind.handler.BoundPropertiesTrackingBindHandler;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.PropertySourceOrigin;
import org.springframework.boot.origin.SystemEnvironmentOrigin;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * Analyzes the binding results of {@link TRpcConfigurationBinder} and generates a description
 *
 * <ul>
 *     <li>Find all {@link #bound} successfully bound properties and their respective {@link PropertySource}, group by source</li>
 *     <li>For each property, iterate through the values on each source, if the {@link Origin} is different from the property, treat it as an override</li>
 * </ul>
 *
 */
public class BindResultDescription {

     /**
      * Unknown {@link PropertySource}
      */
     private static final String UNKNOWN_SOURCE = "UnknownSource";

     /**
      * Line divider element used to generate line dividers of corresponding length based on the title string: {@link #appendDivideLine(StringBuilder, int)}
      */
     private static final char DIVIDE_LINE_ELEMENT = '-';

     /**
      * To find overridden values, a Spring Boot Binder is used, so a Bindable is needed
      */
     private static final Bindable<Object> BINDABLE = Bindable.of(Object.class);

     /**
      * Environment source of {@link #bound}, containing all {@link PropertySource}
      */
     private final ConfigurableEnvironment environment;

     /**
      * Successfully bound properties, each property containing an {@link Origin}, bound collected by {@link TRpcConfigurationBinder#getBindHandler()}
      */
     private final List<ConfigurationProperty> bound;

     /**
      * To find overridden values with Spring Boot Binder, a Binder needs to be generated for each source: {@link #getBinder(PropertySource)}
      */
     private final Map<PropertySource<?>, Binder> singleSourceBinders = Maps.newConcurrentMap();

     public BindResultDescription(ConfigurableEnvironment environment,
             List<ConfigurationProperty> bound) {
          this.environment = environment;
          this.bound = bound;
     }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendProperties(sb);
        return sb.toString();
    }

    private void appendProperties(StringBuilder sb) {
        // 对bound分组并分析每个组
        groupBySource(bound).forEach((s, p) -> appendGroup(sb, s, p));
    }

    private void appendGroup(StringBuilder sb, String source, List<ConfigurationProperty> configurationProperties) {
        String header = "Source \"" + source + "\":";
        sb.append(header).append('\n');
        appendDivideLine(sb, header.length());
        sb.append('\n');
        String props = configurationProperties.stream()
                .map(this::analyzeProperty)
                .collect(Collectors.joining("\n"));
        sb.append(props);
        sb.append("\n\n");
    }

    private void appendDivideLine(StringBuilder sb, int length) {
        for (int i = 0; i < length; i++) {
            sb.append(DIVIDE_LINE_ELEMENT);
        }
    }

    private Map<String, List<ConfigurationProperty>> groupBySource(List<ConfigurationProperty> properties) {
        return properties.stream()
                .sorted(Comparator.comparing(p -> p.getName().toString()))
                .collect(Collectors.groupingBy(this::getSource, Maps::newLinkedHashMap, Collectors.toList()));
    }

    private String getSource(ConfigurationProperty property) {
        Origin origin = property.getOrigin();
        if (origin instanceof PropertySourceOrigin) {
            return ((PropertySourceOrigin) origin).getPropertySource().getName();
        }
        if (origin instanceof SystemEnvironmentOrigin) {
            return "SystemEnvironment";
        }
        if (origin instanceof TextResourceOrigin) {
            TextResourceOrigin textResourceOrigin = (TextResourceOrigin) origin;
            if (textResourceOrigin.getResource() == null) {
                return UNKNOWN_SOURCE;
            }
            return textResourceOrigin.getResource().getDescription();
        }
        return UNKNOWN_SOURCE;
    }

     /**
      * Analyzes property and generates description using {@link #findOverridden(ConfigurationProperty)} to find duplicate property in {@link #environment}
      *
      * @param property Successfully bound property
      * @return Property analysis description
      */
     private String analyzeProperty(ConfigurationProperty property) {
          StringBuilder sb = new StringBuilder();
          sb.append(property.getName());
          sb.append('=');
          sb.append(getValueAtOrigin(property.getValue(), property.getOrigin()));
          List<String> overridden = findOverridden(property);
          if (CollectionUtils.isNotEmpty(overridden)) {
               sb.append("\n    values overridden:\n");
               String overriddenString = overridden.stream()
                       .map(o -> "        " + o)
                       .collect(Collectors.joining("\n"));
               sb.append(overriddenString);
          }
          return sb.toString();
     }

     /**
      * Finds duplicate property in {@link #environment}
      *
      * Note that the environment may contain composite type sources {@link CompositePropertySource},
      * in order to accurately analyze, it needs to be flattened
      *
      * @param property Successfully bound property
      * @return List of overridden values
      */
     private List<String> findOverridden(ConfigurationProperty property) {
          return environment.getPropertySources().stream()
                  .flatMap(this::tryFlatten)
                  .map(source -> findOverridden(source, property))
                  .filter(Objects::nonNull)
                  .collect(Collectors.toList());
     }

     /**
      * Finds duplicate property from a specific source
      *
      * To be compatible with Spring Boot's Relaxed Binding, use Spring Boot Binder to bind from a single source
      *
      * @param source Source
      * @param property Successfully bound property
      * @return Overridden value
      * @see <a href="https://docs.spring.io/spring-boot/docs/current/reference/html/features.html">
      *         Relaxed Binding</a>
      */
     private String findOverridden(PropertySource<?> source, ConfigurationProperty property) {
          AtomicReference<ConfigurationProperty> holder = new AtomicReference<>(null);
          BindResult<Object> exists = bindProperty(source, property, holder::set);
          if (!exists.isBound()) {
               return null;
          }
          if (Objects.equals(property.getOrigin(), holder.get().getOrigin())) {
               // self
               return null;
          }
          return getValueAtOrigin(exists.get(), holder.get().getOrigin());
     }

     private BindResult<Object> bindProperty(PropertySource<?> source, ConfigurationProperty property,
             Consumer<ConfigurationProperty> consumer) {
          Binder binder = getBinder(source);
          return binder.bind(property.getName(), BINDABLE, new BoundPropertiesTrackingBindHandler(consumer));
     }

     private Binder getBinder(PropertySource<?> source) {
          return singleSourceBinders.computeIfAbsent(source, this::createSingleSourceBinder);
     }

     private Binder createSingleSourceBinder(PropertySource<?> source) {
          StandardEnvironment environment = new StandardEnvironment();
          environment.getPropertySources().addFirst(source);
          return Binder.get(environment);
     }

     private String getValueAtOrigin(Object value, Origin origin) {
          return value + " (\"" + (origin == null ? UNKNOWN_SOURCE : origin) + "\")";
     }

     /**
      * If the source is a composite type, flatten it.
      * Ignore the built-in {@link ConfigurationPropertySource} from Spring Boot, otherwise, it will be analyzed repeatedly
      *
      * @param propertySource Source
      * @return Flattened source
      */
     private Stream<PropertySource<?>> tryFlatten(PropertySource<?> propertySource) {
          if (propertySource instanceof CompositePropertySource) {
               return ((CompositePropertySource) propertySource).getPropertySources().stream();
          }
          if (ConfigurationPropertySources.isAttachedConfigurationPropertySource(propertySource)) {
               // ignore ConfigurationPropertySources
               return Stream.empty();
          }
          return Stream.of(propertySource);
     }
}
