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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

public class StringConstantFieldValuePredicateUtilsTest {


    @Test
    public void of() {
        Predicate<String> of = StringConstantFieldValuePredicateUtils.of(TestConstant.class);
        Assertions.assertFalse(of.test("group"));
        Assertions.assertTrue(of.test("endpoint"));
        Assertions.assertTrue(of.test("isUseEndpointParsingRule"));
        Assertions.assertTrue(of.test("isUseCloudNamespaceParsing"));
    }

    public static class TestConstant {

        public static final String IS_USE_CLOUD_NAMESPACE_PARSING = "isUseCloudNamespaceParsing";
        public static final String IS_USE_ENDPOINT_PARSING_RULE = "isUseEndpointParsingRule";
        public static final String ENDPOINT = "endpoint";
    }
}
