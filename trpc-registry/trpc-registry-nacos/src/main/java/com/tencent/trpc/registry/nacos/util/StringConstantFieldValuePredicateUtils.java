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

package com.tencent.trpc.registry.nacos.util;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.utils.ClassUtils;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * String constant assertion utility class
 */
public class StringConstantFieldValuePredicateUtils implements Predicate<String> {

    private static final Logger logger = LoggerFactory.getLogger(StringConstantFieldValuePredicateUtils.class);

    /**
     * Collection of constant fields
     */
    private final Set<String> constantFieldValues;

    public StringConstantFieldValuePredicateUtils(Class<?> targetClass) {
        this.constantFieldValues = getConstantFieldValues(targetClass);
    }

    /**
     * Initialize the target class object
     *
     * @param targetClass Target class object
     * @return Current assertion utility class
     */
    public static Predicate<String> of(Class<?> targetClass) {
        return new StringConstantFieldValuePredicateUtils(targetClass);
    }

    /**
     * Get the value of the constant field
     *
     * @param targetClass Target class object
     * @return Set of fields
     */
    private Set<String> getConstantFieldValues(Class<?> targetClass) {
        return ClassUtils.getConstantValues(targetClass).stream()
                // Casts String type
                .map(String.class::cast)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean test(String s) {
        return constantFieldValues.contains(s);
    }

}
