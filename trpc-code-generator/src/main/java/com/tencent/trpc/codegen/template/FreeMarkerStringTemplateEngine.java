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

import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * Implementation of {@link CodeTemplateEngine} that adapts FreeMarker.
 * Uses plain String as template, and {@code Map<String, Object>} as Context.
 */
public class FreeMarkerStringTemplateEngine implements CodeTemplateEngine<String, Map<String, Object>> {
    /**
     * {@inheritDoc}
     */
    @Override
    public String process(String name, String template, TemplateContext<Map<String, Object>> context)
            throws CodeTemplateException {
        try {
            Template t = new Template(name, template, FreeMarkerConfiguration.getInstance().getConfiguration());
            StringWriter out = new StringWriter();
            t.process(context.getContext(), out);
            return out.toString();
        } catch (IOException | TemplateException e) {
            throw new CodeTemplateException("failed to process template " + name, e);
        }
    }
}
