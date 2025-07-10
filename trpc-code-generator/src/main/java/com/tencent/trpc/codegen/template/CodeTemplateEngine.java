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

/**
 * Define abilities of the TemplateEngine
 *
 * @param <T> Type of the Template
 * @param <C> Type of the Context
 */
public interface CodeTemplateEngine<T, C> {
    /**
     * Render template with context
     *
     * @param name name of the template, optional
     * @param template template
     * @param context the context to use
     * @return rendered text
     * @throws CodeTemplateException if error occurred while rendering
     */
    String process(String name, T template, TemplateContext<C> context) throws CodeTemplateException;
}
