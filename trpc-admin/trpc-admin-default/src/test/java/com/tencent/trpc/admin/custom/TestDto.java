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

package com.tencent.trpc.admin.custom;

import com.tencent.trpc.admin.dto.CommonDto;

public class TestDto extends CommonDto {

    String testResult;

    public TestDto(String testResult) {
        this.testResult = testResult;
    }

    public TestDto() {
    }

    public String getTestResult() {
        return testResult;
    }

    public void setTestResult(String testResult) {
        this.testResult = testResult;
    }

    @Override
    public String toString() {
        return "TestDto{" + "testResult='" + testResult + '\'' + '}';
    }
}
