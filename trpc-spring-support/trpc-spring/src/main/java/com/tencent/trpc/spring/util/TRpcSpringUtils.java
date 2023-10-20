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

package com.tencent.trpc.spring.util;

import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

/**
 * TRPC-spring related Utilities Class.
 */
public class TRpcSpringUtils {

    private static final String DEFAULT_APPLICATION_NAME = "application";
    private static final String SPRING_APPLICATION_NAME_KEY = "spring.application.name";

    /**
     * Check if the provided spring context requires aware of tRPC framework
     *
     * @param context {@link ApplicationContext}
     * @return true or false
     */
    public static boolean isAwareContext(ApplicationContext context) {
        String id = context.getId();
        if (null == id) {
            return false;
        }
        String name = context.getEnvironment().getProperty(SPRING_APPLICATION_NAME_KEY);
        return StringUtils.hasText(name) && id.startsWith(name) || id.startsWith(DEFAULT_APPLICATION_NAME);
    }

    /**
     * Set tRPC {@link ProviderConfig}'s reference to relating spring bean
     *
     * @param context spring {@link ApplicationContext}
     * @param providerConfig tRPC {@link ProviderConfig}
     * @throws ClassNotFoundException if refClazz not found
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void setRef(ApplicationContext context, ProviderConfig providerConfig)
            throws ClassNotFoundException {
        Class refClazz = Class.forName(providerConfig.getRefClazz());
        Class refInterface = providerConfig.getServiceInterface();
        if (refInterface == null) {
            refInterface = Arrays.stream(refClazz.getInterfaces())
                    .filter(each -> each.getAnnotation(TRpcService.class) != null)
                    .findFirst()
                    .orElse(null);
        }
        Objects.requireNonNull(refInterface, "Not found spring bean interface with Annotation TRpcService(class = "
                + refClazz);
        Map<String, ?> refBeanMap = BeanFactoryUtils.beansOfTypeIncludingAncestors(context, refInterface);
        Object bean = refBeanMap.values().stream()
                .filter(obj -> refClazz == AopUtils.getTargetClass(obj))
                .findFirst()
                .orElse(null);
        providerConfig.setServiceInterface(refInterface);
        providerConfig.setRef(Objects.requireNonNull(bean, "Not found spring bean (class = " + refClazz));
    }
}
