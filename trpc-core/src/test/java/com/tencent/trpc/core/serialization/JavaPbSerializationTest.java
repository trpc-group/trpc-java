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

package com.tencent.trpc.core.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.tencent.trpc.core.serialization.support.JavaPBSerialization;
import com.tencent.trpc.core.serialization.support.PBSerialization;
import com.tencent.trpc.core.serialization.support.helper.ProtoCodecManager;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JavaPbSerializationTest {

    @Test
    public void test() {
        User user = new User();
        user.setDesc("user desc");
        user.setName("supreme");
        Map<String, String> members = new HashMap<>();
        members.put("m11", "um11");
        members.put("m12", "um12");
        user.setMembers(members);
        JavaPBSerialization javaPBSerialization = new JavaPBSerialization();
        byte[] javaPBBytes = javaPBSerialization.serialize(user);
        System.out.println("javapb bytes:" + Arrays.toString(javaPBBytes));
        User user1 = javaPBSerialization.deserialize(javaPBBytes, User.class);
        Assertions.assertEquals(user.toString(), user1.toString());
        PBSerialization pbSerialization = new PBSerialization();
        try {
            User user2 = pbSerialization.deserialize(javaPBBytes, User.class);
            Assertions.assertEquals(user.toString(), user2.toString());
        } catch (IOException ioException) {
            Assertions.fail();
        }
    }

    @Test
    public void testExtend() {
        UserExtend userExtend = new UserExtend();
        userExtend.setF(1.1f);
        userExtend.setArr(new byte[]{1, 2, 3, 4, 5, 6, 7, 9});
        userExtend.setId(0);
        User user = new User();
        user.setDesc("user desc");
        user.setName("supreme");
        User user1 = new User();
        user1.setDesc("u11");
        user1.setName("supreme12");
        City city = new City();
        city.setName("bj");
        City city2 = new City();
        city2.setName("cd");
        userExtend.setCityList(Lists.newArrayList(city, city2));
        userExtend.setUserList(Lists.newArrayList(user, user1));
        JavaPBSerialization javaPBSerialization = new JavaPBSerialization();
        PBSerialization pbSerialization = new PBSerialization();
        try {
            byte[] b = javaPBSerialization.serialize(userExtend);
            UserExtend userExtend1 = javaPBSerialization.deserialize(b, UserExtend.class);
            Assertions.assertEquals(Arrays.toString(userExtend.getArr()), Arrays.toString(userExtend1.getArr()));
            Assertions.assertEquals(userExtend.getId(), userExtend1.getId());
            Assertions.assertNull(userExtend1.getDesc());
            Assertions.assertNull(userExtend1.getName());
            Assertions.assertTrue(MapUtils.isEmpty(userExtend1.getMembers()));
            UserExtend userExtend2 = pbSerialization.deserialize(b, UserExtend.class);
            Assertions.assertEquals(Arrays.toString(userExtend.getArr()), Arrays.toString(userExtend2.getArr()));
            Assertions.assertEquals(userExtend.getId(), userExtend2.getId());
            Assertions.assertNull(userExtend2.getDesc());
            Assertions.assertNull(userExtend2.getName());
            Assertions.assertTrue(MapUtils.isEmpty(userExtend2.getMembers()));
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testException1() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            JavaPBSerialization javaPBSerialization = new JavaPBSerialization();
            javaPBSerialization.serialize(Object.class);
        });
    }

    @Test
    public void testException2() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            JavaPBSerialization javaPBSerialization = new JavaPBSerialization();
            javaPBSerialization.deserialize(new byte[0], Object.class);
        });
    }

    @Test
    public void testJpb() {
        assertEquals(SerializationType.PB, SerializationSupport.ofName("jpb").type());
        assertSame("jpb", SerializationSupport.ofName("jpb").name());
    }

    @Test
    public void test1() {
        Codec codec = ProtoCodecManager.getCodec(User.class);
        Codec codec1 = ProtoCodecManager.getCodec(User.class);
        assertSame(codec, codec1);
    }
}
