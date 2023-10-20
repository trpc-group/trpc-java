/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 THL A29 Limited, a Tencent company. 
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.codegen.template;

import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class TemplateEngineTest {

    @Test
    public void testTemplateEngine() {
        FreeMarkerContext context = new FreeMarkerContext();
        context.put("rtxName", "kelgonwu");
        context.put("pyName", "wukegeng");
        context.put("pinyin", false);
        CodeTemplateEngine<String, Map<String, Object>> engine = new FreeMarkerStringTemplateEngine();
        String text = engine.process("name1", "<#if pinyin>\n"
                + "  <#assign name = pyName>\n"
                + "<#else>\n"
                + "  <#assign name = rtxName>\n"
                + "</#if>\n"
                + "hello ${name}!", context);
        Assert.assertEquals("hello kelgonwu!", text);
    }
}
