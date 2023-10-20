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
import io.envoyproxy.pgv.ValidatorIndex;

/**
 * Pgv ValidatorImpl empty implementation.
 */
@SuppressWarnings({"rawtypes"})
public class PgvEmptyValidatorImpl implements ValidatorImpl {

    @Override
    public void assertValid(Object proto, ValidatorIndex index) {

    }
}
