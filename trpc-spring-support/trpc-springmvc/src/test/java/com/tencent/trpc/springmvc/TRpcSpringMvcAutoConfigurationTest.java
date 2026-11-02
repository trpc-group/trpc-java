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

package com.tencent.trpc.springmvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tencent.trpc.springmvc.proto.ProtoJsonHttpMessageConverter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;

public class TRpcSpringMvcAutoConfigurationTest {

    private final TRpcSpringMvcAutoConfiguration configuration = new TRpcSpringMvcAutoConfiguration();

    @Test
    public void testBeans() {
        assertNotNull(configuration.tRpcWebServerFactoryCustomizer());
        assertNotNull(configuration.tRpcServiceHandlerMapping());
        assertNotNull(configuration.tRpcHandlerAdapter());
    }

    @Test
    public void testExtendMessageConverters() {
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        converters.add(new StringHttpMessageConverter());
        configuration.extendMessageConverters(converters);
        assertEquals(2, converters.size());
        assertTrue(converters.get(0) instanceof ProtoJsonHttpMessageConverter);
    }
}
