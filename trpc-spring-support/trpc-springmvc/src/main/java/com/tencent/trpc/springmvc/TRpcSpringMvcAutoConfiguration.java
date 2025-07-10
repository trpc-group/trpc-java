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

import com.tencent.trpc.spring.context.TRpcConfiguration;
import com.tencent.trpc.springmvc.proto.ProtoJsonHttpMessageConverter;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
public class TRpcSpringMvcAutoConfiguration implements WebMvcConfigurer {

    @Bean
    @ConditionalOnMissingBean
    @DependsOn(TRpcConfiguration.CONFIG_MANAGER_INITIALIZER_BEAN_NAME)
    public TRpcWebServerFactoryCustomizer tRpcWebServerFactoryCustomizer() {
        return new TRpcWebServerFactoryCustomizer();
    }

    @Bean
    @ConditionalOnMissingBean
    public TRpcServiceHandlerMapping tRpcServiceHandlerMapping() {
        return new TRpcServiceHandlerMapping();
    }

    @Bean
    @ConditionalOnMissingBean
    public TRpcHandlerAdapter tRpcHandlerAdapter() {
        return new TRpcHandlerAdapter();
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(0, new ProtoJsonHttpMessageConverter<>());
    }

}
