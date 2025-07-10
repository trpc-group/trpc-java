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

package com.tencent.trpc.validation.pgv;

import com.tencent.trpc.core.common.config.PluginConfig;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;

/**
 * Pgv validation related plugin configuration.
 */
public class PgvValidationConfig {

    /**
     * Pgv validator configuration key.
     */
    private static final String VALIDATORS_KEY = "validators";

    /**
     * Configuration key for whether the response body needs data validation.
     */
    private static final String ENABLE_RESPONSE_VALIDATION_KEY = "response_validation";

    /**
     * Collection of pgv validator implementation classes.
     */
    private List<String> validators;

    /**
     * Whether the response body needs data validation, default is false.
     */
    private boolean enableResponseValidation = Boolean.FALSE;

    /**
     * Parse validation plugin configuration.
     *
     * @param pluginConfig validation plugin TRPC framework configuration.
     * @return pgv validation configuration.
     */
    @SuppressWarnings("unchecked")
    public static PgvValidationConfig parse(PluginConfig pluginConfig) {
        PgvValidationConfig config = new PgvValidationConfig();
        Map<String, Object> extMap = pluginConfig.getProperties();
        if (MapUtils.isNotEmpty(extMap)) {
            config.setValidators((List<String>) extMap.get(VALIDATORS_KEY));
        }
        config.setEnableResponseValidation(
                MapUtils.getBooleanValue(extMap, ENABLE_RESPONSE_VALIDATION_KEY, Boolean.FALSE));
        return config;
    }

    public List<String> getValidators() {
        return validators;
    }

    public void setValidators(List<String> validators) {
        this.validators = validators;
    }

    public boolean isEnableResponseValidation() {
        return enableResponseValidation;
    }

    public void setEnableResponseValidation(boolean enableResponseValidation) {
        this.enableResponseValidation = enableResponseValidation;
    }
}
