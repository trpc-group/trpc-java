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

import com.google.common.collect.ImmutableMap;
import com.tencent.trpc.codegen.Protocol;
import com.tencent.trpc.codegen.TRpcCodeGenerateException;
import com.tencent.trpc.codegen.protobuf.ProtoSourceInfo;
import com.tencent.trpc.codegen.protobuf.source.model.ProtoService;
import com.tencent.trpc.core.common.Version;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Templated code generator.
 * Use specified {@link CodeTemplate}s and {@link CodeTemplateEngine} to generate code files.
 *
 * @param <T> Type of the Template Object
 * @param <C> Type of the Template Context
 */
public class CodeFileGenerator<T, C> {
    private static final Logger log = LoggerFactory.getLogger(CodeFileGenerator.class);
    /**
     * CodeTemplateEngine to use
     */
    private final CodeTemplateEngine<T, C> codeTemplateEngine;
    /**
     * CodeTemplates to use
     */
    private final List<CodeTemplate<T>> codeTemplates;
    /**
     * Provider of Template Context
     */
    private final TemplateContextProvider<C> templateContextProvider;
    /**
     * Will not generate pom.xml if set to true
     */
    private final boolean noPom;

    private CodeFileGenerator(CodeTemplateEngine<T, C> codeTemplateEngine,
                              List<CodeTemplate<T>> codeTemplates,
                              TemplateContextProvider<C> templateContextProvider,
                              boolean noPom) {
        this.codeTemplateEngine = codeTemplateEngine;
        this.codeTemplates = codeTemplates;
        this.templateContextProvider = templateContextProvider;
        this.noPom = noPom;
    }

    /**
     * Generate templated code files.
     * <p>Input {@link ProtoSourceInfo} and {@link Protocol} will be used as Template Context.</p>
     * <p>Each {@code ProtoService} in {@code ProtoSourceInfo} will result in a set of code files.</p>
     *
     * @param protocol the specified transfer protocol
     * @param sourceInfo the specified {@code ProtoSourceInfo}
     * @param outputPath parent directory of the generated files
     * @param customContext custom variables to add to Template Context
     */
    public void generateCodeFiles(Protocol protocol,
                                  ProtoSourceInfo sourceInfo,
                                  Path outputPath,
                                  Map<String, Object> customContext) {
        Map<String, Object> baseContext = ImmutableMap.of(
                "version", Version.version(),
                "protocol", protocol.toString(),
                "custom", customContext
        );
        codeTemplates.forEach(template -> {
            if (noPom && template.getCodeType() == CodeType.POM_XML) {
                return;
            }
            if (template.getCodeScope() == CodeScope.GLOBAL) {
                writeCodeTo(outputPath.resolve(template.getCodeFilename()),
                        generateCode(template, baseContext, ImmutableMap.of("sourceInfo", sourceInfo)));
            } else if (template.getCodeScope() == CodeScope.SERVICE) {
                sourceInfo.getServices().forEach(service -> {
                    if (template.getCodeType() == CodeType.STREAM_API && !service.hasStreamingMethod()) {
                        return;
                    }
                    writeCodeTo(getServiceCodeFilePath(service, template, outputPath),
                            generateCode(template, baseContext, ImmutableMap.of("service", service)));
                });
            }
        });
    }

    private String generateCode(CodeTemplate<T> template,
                                Map<String, Object> baseContext,
                                Map<String, Object> otherContext) {
        TemplateContext<C> templateContext = templateContextProvider.createContext();
        baseContext.forEach(templateContext::put);
        otherContext.forEach(templateContext::put);
        return codeTemplateEngine.process(template.getName(), template.getTemplate(), templateContext);
    }

    /**
     * Get the absolute path of the rpc service code file about to generate.
     *
     * @param service {@code ProtoService} related to the code file.
     * @param codeTemplate {@code CodeTemplate} used to generate code.
     * @param outputPath parent directory of the generated code files.
     * @return {@code Path} of the code file.
     */
    private Path getServiceCodeFilePath(ProtoService service, CodeTemplate<T> codeTemplate, Path outputPath) {
        Path filename = codeTemplate.getCodeFilename(service.getInterfaceNamePrefix());
        Path out = outputPath;
        String packageName = service.getJavaPackage();
        if (StringUtils.isEmpty(packageName)) {
            packageName = service.getPackageName();
        }
        for (String s : packageName.split("\\.")) {
            out = out.resolve(s);
        }
        return out.resolve(filename);
    }

    private void writeCodeTo(Path outputPath, String code) {
        log.info("writing code {}", outputPath);
        try {
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, code.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new TRpcCodeGenerateException("failed to write file " + outputPath, e);
        }
    }

    public static <T, C> CodeFileGeneratorBuilder<T, C> builder() {
        return new CodeFileGeneratorBuilder<>();
    }

    /**
     * create a default {@code CodeFileGenerator}, which use FreeMarker as TemplateEngine,
     * and load default CodeTemplates. see {@link DefaultCodeTemplates}
     *
     * @return the default CodeGenerator
     */
    public static CodeFileGenerator<String, Map<String, Object>> createDefault() {
        return CodeFileGenerator.<String, Map<String, Object>>builder()
                .codeTemplateEngine(new FreeMarkerStringTemplateEngine())
                .templateContextProvider(new FreeMarkerContextProvider())
                .codeTemplates(DefaultCodeTemplates.getInstance().getCodeTemplates())
                .build();
    }

    public static final class CodeFileGeneratorBuilder<T, C> {
        private CodeTemplateEngine<T, C> codeTemplateEngine;
        private List<CodeTemplate<T>> codeTemplates = Collections.emptyList();
        private TemplateContextProvider<C> templateContextProvider;
        private boolean noPom;

        private CodeFileGeneratorBuilder() {
        }

        /**
         * Set the CodeTemplateEngine
         */
        public CodeFileGeneratorBuilder<T, C> codeTemplateEngine(CodeTemplateEngine<T, C> codeTemplateEngine) {
            this.codeTemplateEngine = codeTemplateEngine;
            return this;
        }

        /**
         * Set CodeTemplates
         */
        public CodeFileGeneratorBuilder<T, C> codeTemplates(List<CodeTemplate<T>> codeTemplates) {
            this.codeTemplates = codeTemplates;
            return this;
        }

        /**
         * Set TemplateContextProvider
         */
        public CodeFileGeneratorBuilder<T, C> templateContextProvider(
                TemplateContextProvider<C> templateContextProvider) {
            this.templateContextProvider = templateContextProvider;
            return this;
        }

        /**
         * Will not generate pom.xml if set to true
         */
        public CodeFileGeneratorBuilder<T, C> noPom(boolean noPom) {
            this.noPom = noPom;
            return this;
        }

        /**
         * Build {@link CodeFileGenerator}
         */
        public CodeFileGenerator<T, C> build() {
            if (codeTemplateEngine == null) {
                throw new IllegalArgumentException("codeTemplateEngine must not be null");
            }
            if (templateContextProvider == null) {
                throw new IllegalArgumentException("templateContextProvider must not be null");
            }
            return new CodeFileGenerator<>(codeTemplateEngine, codeTemplates, templateContextProvider, noPom);
        }
    }
}
