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

package com.tencent.trpc.core.sign;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.extension.ExtensionClass;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.sign.spi.Sign;
import com.tencent.trpc.core.utils.StringUtils;
import java.util.Map;

/**
 * Signature utility class.
 */
public class SignSupport {

    /**
     * Sign plugin loader.
     */
    private static final ExtensionLoader<Sign> signLoader = ExtensionLoader.getExtensionLoader(Sign.class);
    /**
     * Sign plugin cache, note that the name should be consistent with the SPI file plugin name.
     */
    private static final Map<String, Sign> nameToSign = Maps.newHashMap();

    static {
        preLoadSign();
    }

    /**
     * Preload Sign plugins.
     */
    public static void preLoadSign() {
        signLoader.getAllExtensionClass().stream()
                .map(ExtensionClass::getExtInstance)
                .forEach(sign -> nameToSign.putIfAbsent(sign.name(), sign));
    }

    /**
     * Get the corresponding plugin from the cache by the sign plugin name.
     *
     * @param signName the plugin name
     * @return the Sign instance if it exists, otherwise return null
     */
    public static Sign ofName(String signName) {
        return nameToSign.get(signName);
    }

    /**
     * Determine whether to verify the signature based on the plugin name and request body.
     * If there is no corresponding plugin for the plugin name, no signature verification is required.
     * If the request body is null, no signature verification is required.
     *
     * @param signName the plugin name
     * @param body the request body
     * @return true if signature verification is required, otherwise return false
     */
    public static boolean isVerify(String signName, byte[] body) {
        return !isNotVerify(signName, body);
    }

    /**
     * Determine whether not to verify the signature based on the plugin name and request body.
     * If there is no corresponding plugin for the plugin name, no signature verification is required.
     * If the request body is null, no signature verification is required.
     *
     * @param signName the plugin name
     * @param body the request body
     * @return true if signature verification is not required, otherwise return false
     */
    public static boolean isNotVerify(String signName, byte[] body) {
        return StringUtils.isEmpty(signName) || null == body;
    }

}
