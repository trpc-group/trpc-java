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

package com.tencent.trpc.core.extension;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.TRpcSystemProperties;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.utils.ClassLoaderUtils;
import com.tencent.trpc.core.utils.PreconditionUtils;
import com.tencent.trpc.core.utils.StringUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Plugin loader, the core class for loading SPI plugins.
 *
 * @param <T> plugin type parameter
 * @see Extensible
 */
public class ExtensionLoader<T> {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionLoader.class);
    /**
     * Default definition path for Java SPI interface.
     */
    private static final String SERVICES_DIRECTORY = "META-INF/services/";
    /**
     * Definition path for TRPC SPI interface.
     */
    private static final String TRPC_DIRECTORY = "META-INF/trpc/";
    /**
     * Cache for ExtensionLoader of specified Class.
     */
    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> LOADER_MAPPER = new ConcurrentHashMap<>();
    /**
     * Plugin cache: class -> (name->pluginConfig).
     */
    private static final Map<Class<?>, Map<String, PluginConfig>> PLUGIN_CONFIG_MAP = Maps.newConcurrentMap();
    /**
     * Default SPI plugin name.
     */
    private final String defaultExtName;
    /**
     * Default SPI plugin type.
     */
    private final Class<T> type;
    /**
     * Cache for the implementation classes loaded for the specified type of plugin.
     */
    private final ConcurrentMap<String, ExtensionClass<T>> cachedExtensionClasses;

    private ExtensionLoader(Class<T> type) {
        this.defaultExtName = loadDefaultExtensionName(type);
        this.type = type;
        this.cachedExtensionClasses = loadExtensionClasses();
    }

    /**
     * Check if the plugin loader cache contains the specified class plugin loader.
     *
     * @param clazz plugin class
     * @return whether the cache contains the specified class plugin loader
     */
    public static boolean hasExtensionLoader(Class<?> clazz) {
        return LOADER_MAPPER.containsKey(clazz);
    }

    /**
     * Get the plugin loader instance.
     *
     * <p>Plugins will be loaded from the following two locations:</p>
     * <ul>
     * <li>META-INF/trpc/&ltplugin interface fully qualified name&gt;</li>
     * <li>META-INF/services/&ltplugin interface fully qualified name&gt;</li>
     * </ul>
     *
     * <p>The file content format of the plugin supports the following two formats:</p>
     * <ul>
     * <li>&lt;name&gt;=&lt;plugin class fully qualified name&gt;</li>
     * <li>&lt;plugin class fully qualified name&gt;</li>
     * </ul>
     *
     * @param <T> plugin type parameter
     * @param type plugin interface class
     * @return plugin loader instance
     */
    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        Objects.requireNonNull(type, "param type must not be null");
        return (ExtensionLoader<T>) LOADER_MAPPER.computeIfAbsent(type, ExtensionLoader::new);
    }

    /**
     * Destroy all plugins.
     */
    public static synchronized void destroyAllPlugin() {
        LOADER_MAPPER.values().forEach(v -> v.getAllExtensionClass().forEach(ExtensionClass::destroy));
        LOADER_MAPPER.values().forEach(v -> v.cachedExtensionClasses.clear());
        LOADER_MAPPER.clear();
        PLUGIN_CONFIG_MAP.clear();
    }

    /**
     * Get all plugin configurations.
     */
    public static Map<Class<?>, Map<String, PluginConfig>> getPluginConfigMap() {
        return PLUGIN_CONFIG_MAP;
    }

    /**
     * Get the plugin configuration of the specified plugin type.
     *
     * @param pluginType plugin type
     * @return all configurations of the plugins of the specified pluginType
     */
    public static Map<String, PluginConfig> getPluginConfigMap(Class<?> pluginType) {
        return PLUGIN_CONFIG_MAP.get(pluginType);
    }

    /**
     * Get the configuration of the specified plugin type and plugin name.
     *
     * @param pluginType plugin type
     * @param name plugin name
     * @return the corresponding configuration if the plugin exists, otherwise null
     */
    public static PluginConfig getPluginConfig(Class<?> pluginType, String name) {
        return Optional.ofNullable(PLUGIN_CONFIG_MAP.get(pluginType)).map(map -> map.get(name)).orElse(null);
    }

    /**
     * Register a plugin.
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T, K extends T> void registerPlugin(PluginConfig pluginConfig) {
        ExtensionLoader<?> extensionLoader = ExtensionLoader.getExtensionLoader(pluginConfig.getPluginInterface());
        PreconditionUtils.checkArgument(extensionLoader != null,
                "Register plugin config<%s> exception, extension loader isnull", pluginConfig);
        extensionLoader.addExtension(pluginConfig.getName(), pluginConfig.getPluginClass());// 注册插件业务定义
        registerPluginConfig(pluginConfig.getPluginInterface(), pluginConfig);
    }

    /**
     * Register plugin configuration.
     */
    private static synchronized void registerPluginConfig(Class<?> pluginType,
            PluginConfig pluginConfig) {
        PreconditionUtils.checkArgument(pluginType != null, "pluginService is null");
        PreconditionUtils.checkArgument(pluginConfig != null, "pluginConfig is null");
        Map<String, PluginConfig> map = getPluginConfigMap().computeIfAbsent(pluginType, k -> new HashMap<>());
        if (!map.containsKey(pluginConfig.getName())) {
            logger.info("Register plugin config({})", pluginConfig);
            map.put(pluginConfig.getName(), pluginConfig);
        } else {
            throw new IllegalStateException("Exist plugin config(pluginType=" + pluginType + ", name = "
                    + pluginConfig.getName() + ")");
        }
    }

    /**
     * Manually add a new plugin. If a plugin with the same name already exists, compare the two clazzes, and prompt an
     * exception if they are different.
     *
     * @param name plugin name
     * @param clazz plugin implementation class
     * @throws TRpcExtensionException when there is a duplicate plugin or plugin implementation class exception
     */
    public void addExtension(String name, Class<?> clazz) {
        loadExtensionClass(cachedExtensionClasses, name, clazz);
    }

    /**
     * Manually remove a plugin.
     *
     * @param name plugin name
     * @return the removed plugin
     */
    public ExtensionClass<T> removeExtension(String name) {
        return cachedExtensionClasses.remove(name);
    }

    /**
     * Manually replace a plugin.
     *
     * @param name plugin name
     * @param clazz new plugin class
     */
    @SuppressWarnings("unchecked")
    public void replaceExtension(String name, Class<?> clazz) {
        cachedExtensionClasses.compute(name, (s, oldExtension) -> new ExtensionClass<>(type,
                (Class<? extends T>) clazz, name, clazz.getAnnotation(Activate.class)));
    }

    /**
     * Get the default plugin. The default plugin name is obtained from the {@link Extensible @Extensible} annotation.
     *
     * @return plugin instance, or {@code null} if there is no default plugin or no default plugin name is configured.
     */
    public T getDefaultExtension() {
        return Optional.ofNullable(getExtensionClass(defaultExtName)).map(ExtensionClass::getExtInstance).orElse(null);
    }

    /**
     * Get the plugin by name.
     *
     * @param name plugin name
     * @return plugin instance
     * @throws TRpcExtensionException when the specified plugin with the given name does not exist
     */
    public T getExtension(String name) {
        return Optional.ofNullable(getExtensionClass(name))
                .map(ExtensionClass::getExtInstance)
                .orElseThrow(() -> new TRpcExtensionException(
                        "Cannot get extension of type <" + type.getName() + "> with name <" + name + ">"));

    }

    /**
     * Check if the plugin exists.
     *
     * @param name plugin name
     * @return true if the plugin exists, false otherwise
     */
    public boolean hasExtension(String name) {
        return getExtensionClass(name) != null;
    }

    /**
     * Refresh the plugin configuration.
     *
     * @param name plugin name
     * @param config plugin configuration
     * @throws TRpcExtensionException when the plugin does not support refresh
     */
    public synchronized void refresh(String name, PluginConfig config)
            throws TRpcExtensionException {
        T extension = getExtension(name);
        if (extension != null) {
            if (extension instanceof RefreshableExtension) {
                ((RefreshableExtension) extension).refresh(config);
            } else {
                throw new TRpcExtensionException("Plugin(" + config + ") not support refresh");
            }
        }
    }

    /**
     * Get the plugin wrapper class.
     *
     * @param name plugin name
     * @return plugin wrapper class, or {@code null} if there is no corresponding plugin
     */
    public ExtensionClass<T> getExtensionClass(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        return cachedExtensionClasses.get(name);
    }

    /**
     * Get all plugin wrapper classes.
     *
     * @return plugin wrapper classes, or {@code null} if there is no corresponding plugin
     */
    public Collection<ExtensionClass<T>> getAllExtensionClass() {
        return Collections.unmodifiableCollection(cachedExtensionClasses.values());
    }

    /**
     * Get the default plugin name.
     *
     * @return default plugin name, or {@code null} if the default plugin is not configured
     */
    public String getDefaultExtName() {
        return defaultExtName.isEmpty() ? null : defaultExtName;
    }

    /**
     * Load all active plugins of the specified group.
     *
     * @param group plugin group
     * @return plugin list, or {@code null} if there are no active plugins
     */
    public List<T> getActivateExtensions(String group) {
        Objects.requireNonNull(group, "param group must not be null");
        return getExtensions(extensionClass -> {
            Activate activate = extensionClass.getActivate();
            if (activate != null) {
                String[] groups = activate.group();
                for (String activationGroup : groups) {
                    if (org.apache.commons.lang3.StringUtils.equals(group, activationGroup)) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    /**
     * Filter the active plugin list by the specified judgment condition.
     *
     * @param filter plugin activation judgment condition
     * @return plugin list, or {@code null} if there are no active plugins
     */
    public List<T> getExtensions(Predicate<ExtensionClass<T>> filter) {
        Objects.requireNonNull(filter, "param filter must not be null");
        // get the plugin wrapper class list
        return cachedExtensionClasses.values().stream().filter(filter)
                .sorted(Comparator.comparingInt(ExtensionClass::getOrder))
                .map(ExtensionClass::getExtInstance)
                .collect(Collectors.toList());
    }

    /**
     * Get all plugin instances in a sorted list.
     *
     * @return a list of all plugin instances
     */
    public List<T> getAllExtensions() {
        return getExtensions(ec -> true);
    }

    protected String loadDefaultExtensionName(Class<T> clazz) {
        String defaultExtName = Optional.ofNullable(clazz.getAnnotation(Extensible.class))
                .map(extensible -> extensible.value().trim())
                .orElseThrow(() -> new TRpcExtensionException("extensible " + clazz.getName()
                        + " does not annotated with @" + Extensible.class.getName()));

        if (!StringUtils.isEmpty(defaultExtName) && !StringUtils.isJavaIdentifier(defaultExtName)) {
            throw new TRpcExtensionException("default extension name " + defaultExtName + "of extensible "
                    + clazz.getName() + " is invalid");
        }

        return defaultExtName;
    }

    protected ConcurrentMap<String, ExtensionClass<T>> loadExtensionClasses() {
        ConcurrentMap<String, ExtensionClass<T>> extensionClasses = new ConcurrentHashMap<>();
        ClassLoader classLoader = ClassLoaderUtils.getClassLoader(ExtensionLoader.class);
        loadExtensionsFromFile(extensionClasses, classLoader, TRPC_DIRECTORY + type.getName());
        loadExtensionsFromFile(extensionClasses, classLoader, SERVICES_DIRECTORY + type.getName());
        return extensionClasses;
    }

    protected void loadExtensionsFromFile(Map<String, ExtensionClass<T>> extensionClasses,
            ClassLoader classLoader, String fullFileName) {
        Enumeration<URL> urls;
        try {
            if (classLoader != null) {
                urls = classLoader.getResources(fullFileName);
            } else {
                urls = ClassLoader.getSystemResources(fullFileName);
            }
        } catch (Throwable t) {
            logger.error("failed to load extension of extensible " + type.getName(), t);
            return;
        }

        doLoadExtensionsFromFile(extensionClasses, classLoader, urls);
    }

    private void doLoadExtensionsFromFile(Map<String, ExtensionClass<T>> extensionClasses, ClassLoader classLoader,
            Enumeration<URL> urls) {
        if (urls != null) {
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                parseExtension(extensionClasses, classLoader, url);
            }
        }
    }

    private void parseExtension(Map<String, ExtensionClass<T>> extensionClasses,
            ClassLoader classLoader, URL url) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final int ci = line.indexOf('#');
                if (ci >= 0) {
                    line = line.substring(0, ci);
                }
                line = line.trim();
                // skip comment lines or empty lines
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    loadLine(extensionClasses, classLoader, line);
                } catch (TRpcExtensionException e) {
                    logger.warn("failed to load extension of extensible {} from file {} and line is {}",
                            type.getName(), url, line, e);

                }
            }
        } catch (Throwable t) {
            logger.error("failed to load extension of extensible {} from file {}" + url, type.getName(), url, t);
        }
    }

    protected void loadLine(Map<String, ExtensionClass<T>> extensionClasses,
            ClassLoader classLoader,
            String line) {
        String name = null;
        int i = line.indexOf('=');
        if (i >= 0) {
            name = line.substring(0, i).trim();
            line = line.substring(i + 1).trim();
        }
        String className = line;

        // load plugin implementation class
        Class<?> clazz;
        try {
            clazz = Class.forName(className, false, classLoader);
        } catch (Throwable t) {
            throw new TRpcExtensionException("load extension class " + className + " failed", t);
        }

        loadExtensionClass(extensionClasses, name, clazz);
    }

    @SuppressWarnings("unchecked")
    private void loadExtensionClass(Map<String, ExtensionClass<T>> extensionClasses, String name,
            Class<?> clazz) {
        if (!type.isAssignableFrom(clazz)) {
            throw new TRpcExtensionException("class " + clazz.getName() + " is not subtype of " + type.getName());
        }

        // check if there is an extension annotation
        Extension extension = clazz.getAnnotation(Extension.class);
        name = checkExtensionName(name, extension);

        // check if there is a plugin with the same name
        ExtensionClass<?> old = extensionClasses.computeIfAbsent(name, pluginName -> {
            ExtensionClass<T> extensionClass = new ExtensionClass<>(type, (Class<? extends T>) clazz,
                    pluginName, clazz.getAnnotation(Activate.class));
            if (extension != null) {
                extensionClass.setOrder(extension.order());
            }
            return extensionClass;
        });

        validateDuplicateExtension(name, clazz, old);
    }

    private void validateDuplicateExtension(String name, Class<?> clazz, ExtensionClass<?> old) {
        if (old != null) {
            // prevent misconfiguration of two completely identical plugins
            if (clazz != old.getClazz()) {
                String msg = String.format("duplicate extension name of \"%s\", try to load \"%s\", but already have"
                                + " \"%s\"",
                        name, clazz.getName(), old.getClazz().getName());
                if (TRpcSystemProperties.isIgnoreSamePluginName()) {
                    logger.warn(msg);
                } else {
                    throw new TRpcExtensionException(msg);
                }
            }
        }
    }

    private String checkExtensionName(String name, Extension extension) {
        // only when the plugin name is not configured in the configuration file, get it through the annotation
        if (StringUtils.isEmpty(name) && extension != null) {
            name = extension.value().trim();
        }

        if (!StringUtils.isJavaIdentifier(name)) {
            throw new TRpcExtensionException("do not have a valid name");
        }
        return name;
    }

}
