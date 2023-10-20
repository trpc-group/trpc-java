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

import io.envoyproxy.pgv.StringValidation;
import io.envoyproxy.pgv.ValidationException;
import io.envoyproxy.pgv.ValidatorImpl;
import io.envoyproxy.pgv.ValidatorIndex;

public class TestValidator {

    public static io.envoyproxy.pgv.ValidatorImpl validatorFor(Class clazz) {
        if (clazz.equals(TestRequest.class)) {
            return new TestRequestValidator();
        }
        if (clazz.equals(TestBean.class)) {
            return new TestBeanValidator();
        }
        if (clazz.equals(TestResponse.class)) {
            return new TestResponseValidator();
        }
        return null;
    }

    public static class TestRequestValidator implements ValidatorImpl<TestRequest> {

        @Override
        public void assertValid(TestRequest proto, ValidatorIndex index) throws ValidationException {
            StringValidation.maxLength("TestRequest.var1", proto.getVar1(), 10);
            if (proto.getVar3() != null) {
                index.validatorFor(proto.getVar3()).assertValid(proto.getVar3());
            }
        }
    }

    public static class TestBeanValidator implements ValidatorImpl<TestBean> {

        @Override
        public void assertValid(TestBean proto, ValidatorIndex index) throws ValidationException {
            StringValidation.maxLength("TestBean.var1", proto.getVar1(), 10);
        }
    }

    public static class TestResponseValidator implements ValidatorImpl<TestResponse> {

        @Override
        public void assertValid(TestResponse proto, ValidatorIndex index) throws ValidationException {
            StringValidation.maxLength("TestResponse.var1", proto.getVar1(), 10);
            if (proto.getVar3() != null) {
                index.validatorFor(proto.getVar3()).assertValid(proto.getVar3());
            }
        }
    }

    public static class TestRequest {

        private String var1;
        private Integer var2;
        private TestBean var3;

        public String getVar1() {
            return var1;
        }

        public void setVar1(String var1) {
            this.var1 = var1;
        }

        public Integer getVar2() {
            return var2;
        }

        public void setVar2(Integer var2) {
            this.var2 = var2;
        }

        public TestBean getVar3() {
            return var3;
        }

        public void setVar3(TestBean var3) {
            this.var3 = var3;
        }
    }

    public static class TestBean {

        private String var1;

        public String getVar1() {
            return var1;
        }

        public void setVar1(String var1) {
            this.var1 = var1;
        }
    }

    public static class TestResponse {

        private String var1;
        private Integer var2;
        private TestBean var3;

        public String getVar1() {
            return var1;
        }

        public void setVar1(String var1) {
            this.var1 = var1;
        }

        public Integer getVar2() {
            return var2;
        }

        public void setVar2(Integer var2) {
            this.var2 = var2;
        }

        public TestBean getVar3() {
            return var3;
        }

        public void setVar3(TestBean var3) {
            this.var3 = var3;
        }
    }
    
}
