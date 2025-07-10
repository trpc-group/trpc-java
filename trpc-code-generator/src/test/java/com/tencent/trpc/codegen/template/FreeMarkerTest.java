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
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.junit.Test;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class FreeMarkerTest {
    @Test
    public void test() throws IOException, TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        Map<String, Object> context = new HashMap<>();
        context.put("rtxName", "kelgonwu");
        context.put("pyName", "wukegeng");
        context.put("pinyin", false);
        StringWriter out = new StringWriter();
        Template t = new Template("test", "<#if pinyin>\n"
                + "  <#assign name = pyName>\n"
                + "<#else>\n"
                + "  <#assign name = rtxName>\n"
                + "</#if>\n"
                + "hello ${name}!", cfg);
        t.process(context, out);
        System.out.println(out);
    }
}
