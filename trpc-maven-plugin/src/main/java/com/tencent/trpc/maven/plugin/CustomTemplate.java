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

package com.tencent.trpc.maven.plugin;

import com.tencent.trpc.codegen.template.CodeScope;
import com.tencent.trpc.codegen.template.CodeTemplate;
import com.tencent.trpc.codegen.template.CodeType;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Define a custom code template
 */
public class CustomTemplate {
    private String name;
    private CodeScope scope;
    private String file;
    private String namingPattern;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CodeScope getScope() {
        return scope;
    }

    public void setScope(CodeScope scope) {
        this.scope = scope;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getNamingPattern() {
        return namingPattern;
    }

    public void setNamingPattern(String namingPattern) {
        this.namingPattern = namingPattern;
    }

    /**
     * Convert to {@link CodeTemplate}
     *
     * @return CodeTemplate
     * @throws IOException if failed to read template file
     */
    public CodeTemplate<String> toCodeTemplate() throws IOException {
        String template = FileUtils.readFileToString(new File(file), StandardCharsets.UTF_8);
        return new CodeTemplate<>(name, template, CodeType.CUSTOM, scope, namingPattern);
    }

    @Override
    public String toString() {
        return "CustomTemplate{"
                + "name='" + name + '\''
                + ", scope=" + scope
                + ", file='" + file + '\''
                + ", namingPattern='" + namingPattern + '\''
                + '}';
    }
}
