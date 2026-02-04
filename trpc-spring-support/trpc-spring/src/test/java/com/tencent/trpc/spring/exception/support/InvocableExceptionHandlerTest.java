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

package com.tencent.trpc.spring.exception.support;

import java.lang.reflect.Method;
import java.util.HashSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;

/**
 * Tests for InvocableExceptionHandler
 */
public class InvocableExceptionHandlerTest {

    /**
     * Basic test
     */
    @Test
    public void testInvocableExceptionHandler() throws Exception {
        Method method = MyExceptionHandle.class
                .getDeclaredMethod("handle", MyBean.class, RuntimeException.class, Method.class);
        MyExceptionHandle bean = new MyExceptionHandle();
        InvocableExceptionHandler handler1 = new InvocableExceptionHandler(bean, method);
        InvocableExceptionHandler handler2 = new InvocableExceptionHandler(bean, method);
        HashSet<InvocableExceptionHandler> set = new HashSet<>();
        set.add(handler1);
        Assertions.assertEquals(handler1.hashCode(), handler2.hashCode());
        Assertions.assertEquals(true, handler1.equals(handler1));
        Assertions.assertEquals(true, handler1.equals(handler2));
        Assertions.assertEquals(false, handler1.equals(bean));
        Assertions.assertEquals(true, set.contains(handler2));
        Assertions.assertEquals(MyExceptionHandle.class, handler1.getTargetType());
    }

    /**
     * Test reflection invoking
     */
    @Test
    public void testHandleException() throws Throwable {
        Method targetMethod = MyExceptionHandle.class.getDeclaredMethod("targetMethod", MyBean.class, MyBean.class);
        Method invokeMethod = MyExceptionHandle.class
                .getDeclaredMethod("handle", MyBean.class, RuntimeException.class, Method.class);
        MyExceptionHandle bean = new MyExceptionHandle();
        InvocableExceptionHandler handler = new InvocableExceptionHandler(bean, invokeMethod);
        MethodParameter[] methodParameters = handler.getMethodParameters();
        Object result = handler.handle(new IllegalArgumentException(), targetMethod,
                new Object[]{new MyBean("name1"), new MyBean("name2")});
        Assertions.assertEquals(3, methodParameters.length);
        Assertions.assertEquals(MyBean.class, methodParameters[0].getParameterType());
        Assertions.assertEquals(RuntimeException.class, methodParameters[1].getParameterType());
        Assertions.assertEquals(Method.class, methodParameters[2].getParameterType());
        Assertions.assertEquals(true, String.class.isAssignableFrom(result.getClass()));
        Assertions.assertEquals("name1_IllegalArgumentException_targetMethod", result);
    }

    public static class MyExceptionHandle {

        public void targetMethod(MyBean myBean1, MyBean myBean2) {
        }

        public String handle(MyBean myBean1, RuntimeException e, Method method11111) {
            return myBean1.getName() + "_" + e.getClass().getSimpleName() + "_" + method11111.getName();
        }
    }

    public static class MyBean {

        private String name;

        public MyBean(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
