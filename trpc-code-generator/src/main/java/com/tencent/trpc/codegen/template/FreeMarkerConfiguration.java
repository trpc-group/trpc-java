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

package com.tencent.trpc.codegen.template;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import java.nio.charset.StandardCharsets;

/**
 * Provides FreeMarker global configuration.
 * Call {@link #getInstance()} to get a singleton instance.
 */
public class FreeMarkerConfiguration {
    private static final FreeMarkerConfiguration instance = new FreeMarkerConfiguration();
    private final Configuration configuration;

    private FreeMarkerConfiguration() {
        configuration = new Configuration(Configuration.VERSION_2_3_32);
        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
        configuration.setFallbackOnNullLoopVariable(false);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public static FreeMarkerConfiguration getInstance() {
        return instance;
    }
}
