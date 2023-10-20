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

import io.envoyproxy.pgv.Validator;
import io.envoyproxy.pgv.ValidatorImpl;
import io.envoyproxy.pgv.ValidatorIndex;

/**
 * Default pgv ValidatorIndex implementation.
 */
public class PgvDefaultValidatorIndex implements ValidatorIndex {

    /**
     * Pgv Validator wrapper class, encapsulating the generated Validator implementation.
     */
    private final PgvValidator pgvValidator;

    public PgvDefaultValidatorIndex(PgvValidator pgvValidator) {
        this.pgvValidator = pgvValidator;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Validator<T> validatorFor(Class needValidatedClass) {
        ValidatorImpl validatorImpl = pgvValidator.getValidatorImpl(needValidatedClass);
        return msg -> validatorImpl.assertValid(msg, this);
    }
}
