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

package com.tencent.trpc.validation.pgv;

import io.envoyproxy.pgv.ValidatorImpl;

/**
 * Pgv Validator Registry interface, used for registering Validator implementation classes.
 */
@SuppressWarnings({"rawtypes"})
public interface PgvValidatorRegistry {

    /**
     * Register Validator implementation class.
     *
     * @param pgvValidatorClass
     */
    void register(Class pgvValidatorClass);

    /**
     * Get the validation method for the clazz class.
     *
     * @param needValidatedClass class to be validated
     * @return ValidatorImpl class
     */
    ValidatorImpl getValidatorImpl(Class needValidatedClass);
}
