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

package com.tencent.trpc.validation.pgv;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.utils.ClassLoaderUtils;
import io.envoyproxy.pgv.ValidatorImpl;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Pgv Validator class, encapsulating the generated Validator implementation.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class PgvValidator implements PgvValidatorRegistry {

    /**
     * Logger instance.
     */
    private static final Logger logger = LoggerFactory.getLogger(PgvValidator.class);

    /**
     * ValidatorImpl instance retrieval method name.
     */
    private static final String VALIDATOR_IMPL_GET_METHOD = "validatorFor";

    /**
     * Pgv validation configuration information.
     */
    private final PgvValidationConfig validationConfig;

    /**
     * Collection of validator implementation classes.
     */
    private final List<Class> validatorClasses = Lists.newLinkedList();

    /**
     * ValidatorImpl instance cache.
     */
    private final Map<String, ValidatorImpl> validatorImplCache = Maps.newConcurrentMap();

    /**
     * Default ValidatorImpl implementation.
     */
    private final ValidatorImpl defaultValidatorImpl = new PgvEmptyValidatorImpl();

    public PgvValidator(PgvValidationConfig validationConfig) {
        this.validationConfig = validationConfig;
        init();
    }

    private void init() throws TRpcExtensionException {
        if (validationConfig == null || CollectionUtils.isEmpty(validationConfig.getValidators())) {
            return;
        }

        ClassLoader classLoader = ClassLoaderUtils.getClassLoader(this.getClass());
        for (String pgvValidator : validationConfig.getValidators()) {
            try {
                register(classLoader.loadClass(pgvValidator));
            } catch (ClassNotFoundException e) {
                logger.error("load validator class parse error, {}", e);
                throw new TRpcExtensionException(e.getMessage());
            }
        }
    }

    @Override
    public void register(Class pgvValidatorClass) {
        if (pgvValidatorClass != null) {
            validatorClasses.add(pgvValidatorClass);
        }
    }

    @Override
    public ValidatorImpl getValidatorImpl(Class needValidatedClass) {
        if (CollectionUtils.isEmpty(validatorClasses)) {
            return defaultValidatorImpl;
        }
        return validatorImplCache.computeIfAbsent(needValidatedClass.getName(),
                (name) -> doGetValidatorImpl(needValidatedClass));
    }

    /**
     * Find ValidatorImpl through validator.
     *
     * @param needValidatedClass class to be validated
     * @return returns ValidatorImpl
     */
    private ValidatorImpl doGetValidatorImpl(Class needValidatedClass) {
        for (Class validatorClass : validatorClasses) {
            try {
                Method validatorForMethod = validatorClass
                        .getDeclaredMethod(VALIDATOR_IMPL_GET_METHOD, Class.class);
                Object validatorImpl = validatorForMethod.invoke(null, needValidatedClass);
                if (validatorImpl instanceof ValidatorImpl) {
                    return (ValidatorImpl) validatorImpl;
                }
            } catch (Exception e) {
                logger.error("get validatorImpl error,{}", e);
            }
        }
        return defaultValidatorImpl;
    }
}
