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

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Plugin property encapsulation class.
 *
 * @param <T> Plugin type parameter.
 * @see ExtensionLoader
 */
@SuppressWarnings("rawtypes")
public class ExtensionClass<T> {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionClass.class);

    private final Class<? extends T> clazz;

    private final Class extensionType;

    private final String name;

    private final Activate activate;

    private int order = 0;

    private volatile T instance;

    private AtomicBoolean destroyed = new AtomicBoolean(Boolean.FALSE);

    public ExtensionClass(Class extensionType, Class<? extends T> clazz, String name,
            Activate activate) {
        this.extensionType = extensionType;
        this.clazz = clazz;
        this.name = name;
        this.activate = activate;
    }

    public boolean isInitialized() {
        return instance != null;
    }

    /**
     * In the case of mutual dependence between plugins, there may be deadlocks. Try not to initialize other plugins
     * during the init operation.
     *
     * @return Plugin Instance
     */
    public T getExtInstance() {
        try {
            if (instance == null) {
                synchronized (this) {
                    if (instance == null) {
                        instance = createInstance();
                    }
                }
            }
            return instance;
        } catch (TRpcExtensionException e) {
            throw e;
        } catch (Exception ex) {
            throw new TRpcExtensionException("create " + clazz.getCanonicalName() + " instance error", ex);
        }
    }

    private T createInstance() {
        validateLifecycle();
        Map<String, PluginConfig> map = ExtensionLoader.getPluginConfigMap(extensionType);
        if (map == null) {
            map = ConfigManager.getInstance().getPluginConfigMap().get(extensionType);
        }
        PluginConfig pluginConfig = (map == null ? null : map.get(name));
        try {
            T t = clazz.newInstance();
            injectExtension(t, pluginConfig);
            initExtension(t);
            if (logger.isDebugEnabled()) {
                logger.debug("Create plugin instance (name={}, type={}), config={}) success", name,
                        extensionType, ExtensionLoader.getPluginConfigMap(extensionType));
            }
            return t;
        } catch (Exception e) {
            throw new TRpcExtensionException(
                    "Create plugin instance (name=" + clazz.getCanonicalName() + ") error", e);
        }
    }

    private void injectExtension(T t, PluginConfig pluginConfig) {
        if (t instanceof PluginConfigAware) {
            ((PluginConfigAware) t).setPluginConfig(pluginConfig);
        }
    }

    private void initExtension(T t) {
        if (t instanceof InitializingExtension) {
            ((InitializingExtension) t).init();
        }
    }

    /**
     * Refresh plugin
     *
     * @param config PluginConfig
     */
    public void refresh(PluginConfig config) {
        validateLifecycle();
        synchronized (this) {
            if (instance != null) {
                if (instance instanceof RefreshableExtension) {
                    ((RefreshableExtension) instance).refresh(config);
                }
            }
        }
    }

    /**
     * Plugin destruction, call the specific plugin's destruction method.
     */
    public void destroy() {
        // use CAS to ensure that the plugin is only destroyed once.
        if (destroyed.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
            if (instance != null) {
                if (instance instanceof DisposableExtension) {
                    try {
                        ((DisposableExtension) instance).destroy();
                    } catch (Exception e) {
                        logger.warn("Destroy extension:{} exception", instance, e);
                    }
                }
            }
            instance = null;
        }
    }

    private void validateLifecycle() {
        if (destroyed.get()) {
            throw new TRpcExtensionException(
                    "plugin (name=" + clazz.getCanonicalName() + ") destroyed");
        }
    }

    public Class<? extends T> getClazz() {
        return clazz;
    }

    public String getName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Activate getActivate() {
        return activate;
    }
}