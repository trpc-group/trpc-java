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

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Describes a code template
 *
 * @param <T> Type of the template object
 */
public class CodeTemplate<T> {
    /**
     * optional template name
     */
    private final String name;
    /**
     * template object
     */
    private final T template;
    /**
     * CodeType of this template
     */
    private final CodeType codeType;
    /**
     * CodeScope of this template
     */
    private final CodeScope codeScope;
    /**
     * Name pattern of the code file generated
     */
    private final String codeFilenamePattern;

    public CodeTemplate(String name, T template, CodeType codeType, CodeScope codeScope, String codeFilenamePattern) {
        this.name = name;
        this.template = template;
        this.codeType = codeType;
        this.codeScope = codeScope;
        this.codeFilenamePattern = codeFilenamePattern;
    }

    public String getName() {
        return name;
    }

    public T getTemplate() {
        return template;
    }

    public CodeType getCodeType() {
        return codeType;
    }

    public CodeScope getCodeScope() {
        return codeScope;
    }

    /**
     * Get the name of the generated code file.
     *
     * @param args format arguments (see {@link String#format(String, Object...)})
     * @return relative path of the generated code file
     */
    public Path getCodeFilename(Object... args) {
        return Paths.get(String.format(codeFilenamePattern, args));
    }
}
