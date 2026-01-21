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

package com.tencent.trpc.core.utils;

import com.tencent.trpc.core.common.LifecycleBase;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.serialization.spi.Serialization;
import com.tencent.trpc.core.serialization.support.PBSerialization;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClassUtilsTest {

    private static final Logger logger = LoggerFactory.getLogger(ClassUtilsTest.class);

    public String testField;

    public String getTestField() {
        return testField;
    }

    @Test
    public void testIsByteArray() {
        byte[] data = new byte[]{1, 2};
        Assertions.assertTrue(ClassUtils.isByteArray(data));
        Byte[] dataB = new Byte[]{3, 4};
        Assertions.assertTrue(ClassUtils.isByteArray(dataB));
        Assertions.assertTrue(ClassUtils.isByteArray(byte[].class));
        Assertions.assertTrue(ClassUtils.isByteArray(Byte[].class));
        Assertions.assertEquals(1, ClassUtils.cast2ByteArray(new Byte[]{1})[0]);
        Object obj = ClassUtils.newInstance(Object.class);
        Assertions.assertNotNull(obj);
        try {
            ClassUtils.newInstance(LifecycleObj.class);
        } catch (RuntimeException e) {
            logger.error("class util new instance error:", e);
        }
    }

    @Test
    public void testCast2ByteArray() {
        byte[] data = new byte[]{1, 2};
        byte[] newData = ClassUtils.cast2ByteArray(data);
        Assertions.assertNotNull(newData);
    }

    @Test
    public void testGetAllInterfaces() {
        List<Class> classes = ClassUtils.getAllInterfaces(Object.class);
        Assertions.assertEquals(classes.size(), 0);
        List<Class> pbInterfaces = ClassUtils.getAllInterfaces(PBSerialization.class);
        Assertions.assertEquals(pbInterfaces.size(), 1);
        Assertions.assertEquals(pbInterfaces.get(0), Serialization.class);
        List<Class> classNull = ClassUtils.getAllInterfaces(null);
        Assertions.assertTrue(classNull.isEmpty());
    }


    @Test
    public void testGetDeclaredMethod() throws NoSuchMethodException {
        Method method = ClassUtils.getDeclaredMethod(ClassUtilsTest.class, "getTestField");
        Assertions.assertEquals(method, ClassUtilsTest.class.getMethod("getTestField"));
        Method methodNull = ClassUtils.getDeclaredMethod(null, "getTestField");
        Assertions.assertNull(methodNull);
        methodNull = ClassUtils.getDeclaredMethod(ClassUtilsTest.class, "getTestField1");
        Assertions.assertNull(methodNull);
    }

    @Test
    public void testGetValue() throws NoSuchFieldException {
        Assertions.assertEquals("common_string",
                ClassUtils.getValue(new TestInstance(),
                        TestInstance.class.getDeclaredField("commonString")).get());
    }

    @Test
    public void testGetStaticValue() throws NoSuchFieldException {
        Assertions.assertEquals("static_value",
                ClassUtils.getStaticValue(TestInstance.class.getDeclaredField("staticValue")).get());

    }

    @Test
    public void testGetConstantValues() {
        Assertions.assertTrue(ClassUtils.getConstantValues(TestInstance.class).contains("public_constant"));
        Assertions.assertFalse(ClassUtils.getConstantValues(TestInstance.class).contains("private_constant"));
        Assertions.assertFalse(ClassUtils.getConstantValues(TestInstance.class).contains("final_value"));
    }

    private static class TestInstance {

        public static final String PUBLIC_CONSTANT = "public_constant";

        private static final String PRIVATE_CONSTANT = "private_constant";

        private static String staticValue = "static_value";

        private final String finalValue = "final_value";

        private String commonString = "common_string";

    }

    private static class LifecycleObj extends LifecycleBase {

        private LifecycleObj() {

        }
    }
}
