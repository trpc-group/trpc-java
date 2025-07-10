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

import com.google.common.collect.ImmutableList;
import com.tencent.trpc.codegen.TRpcCodeGenerateException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides default {@link CodeTemplate}s.
 * Call {@link #getInstance()} to get a singleton instance.
 */
public class DefaultCodeTemplates {

    private static final Logger log = LoggerFactory.getLogger(DefaultCodeTemplates.class);
    private static final String templatePrefix = "templates/";
    private static final String templateSuffix = ".tpl";
    private static final DefaultCodeTemplates instance = new DefaultCodeTemplates();
    private final List<CodeTemplate<String>> codeTemplates;

    private DefaultCodeTemplates() {
        ClassLoader classLoader = DefaultCodeTemplates.class.getClassLoader();
        List<CodeTemplateInfo> resources = Arrays.asList(
                new CodeTemplateInfo("API.java", CodeType.API, CodeScope.SERVICE, "%sAPI.java"),
                new CodeTemplateInfo("AsyncAPI.java", CodeType.ASYNC_API, CodeScope.SERVICE, "%sAsyncAPI.java"),
                new CodeTemplateInfo("StreamAPI.java", CodeType.STREAM_API, CodeScope.SERVICE, "%sStreamAPI.java"),
                new CodeTemplateInfo("pom.xml", CodeType.POM_XML, CodeScope.GLOBAL, "pom.xml")
        );
        codeTemplates = resources.stream()
                .map(info -> {
                    String name = info.name;
                    URL url = classLoader.getResource(templatePrefix + name + templateSuffix);
                    Objects.requireNonNull(url, "template resource " + name + " not found");
                    String template;
                    try {
                        template = IOUtils.toString(url, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new TRpcCodeGenerateException("cannot read template " + name);
                    }
                    log.info("loaded code templates: {}({})", name, info.codeType);
                    return new CodeTemplate<>(name, template, info.codeType, info.codeScope, info.namingPattern);
                })
                .collect(ImmutableList.toImmutableList());
    }

    public static DefaultCodeTemplates getInstance() {
        return instance;
    }

    private static class CodeTemplateInfo {

        private final String name;
        private final CodeType codeType;
        private final CodeScope codeScope;

        private final String namingPattern;

        CodeTemplateInfo(String name, CodeType codeType, CodeScope codeScope, String namingPattern) {
            this.name = name;
            this.codeType = codeType;
            this.codeScope = codeScope;
            this.namingPattern = namingPattern;
        }

    }

    public List<CodeTemplate<String>> getCodeTemplates() {
        return codeTemplates;
    }
}
