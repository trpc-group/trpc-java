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

import com.google.common.collect.Lists;
import com.tencent.trpc.core.common.config.annotation.ConfigProperty;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BinderUtilsTest {

    private static final Logger logger = LoggerFactory.getLogger(BinderUtilsTest.class);

    private Map<String, Object> valueMap;
    private BinderObj obj = new BinderObj();

    @Before
    public void setUp() {
        valueMap = new HashMap<>();
        valueMap.put("firstName", "young");
        valueMap.put("age", 12);
        valueMap.put("money", 15D);
    }

    @Test
    public void testUnderscoresToUpperCase() {
        Assert.assertEquals(BinderUtils.underscoresToUpperCase("a_bi_cd"), "aBiCd");
    }

    @Test
    public void testUpperCaseToUnderscores() {
        Assert.assertEquals("a_bi_cd", BinderUtils.upperCaseToUnderscores("aBiCd"));

    }

    @Test
    public void testNewFunction() {
        Assert.assertEquals(BinderUtils.newFunction("a").apply("b"), "a");
    }

    @Test
    public void testBind() {
        BinderUtils.bind(Function.identity(), obj, valueMap, "firstName", Function.identity());
        BinderUtils.bind(Function.identity(), obj, valueMap, "age", Function.identity());
        BinderUtils.bind(Function.identity(), obj, valueMap, "money", Function.identity());
        Assert.assertEquals("young", obj.getFirstName());
        Assert.assertEquals(12, obj.getAge());
        Assert.assertEquals(15, obj.getMoney().intValue());
    }

    @Test
    public void testBind1() {
        valueMap.put("first_name", "hello");
        BinderUtils.bind("age", obj, valueMap, "age", Function.identity());
        BinderUtils.bind("firstName", obj, valueMap, "first_name", Function.identity());
        Assert.assertEquals("hello", obj.getFirstName());
        Assert.assertEquals(12, obj.getAge());

    }

    @Test
    public void testTestBind3() {
        valueMap.put("age2", 14);
        BinderUtils.bind("age", obj, valueMap, "age2");
        Assert.assertEquals(14, obj.getAge());
    }

    @Test
    public void testTestBind4() {
        valueMap.put("age", 16);
        BinderUtils.bind(obj, valueMap);
        Assert.assertEquals(16, obj.getAge());
    }

    @Test
    public void testTestBind6() {
        BinderUtils.bind(obj, "age", 20, Function.identity());
        BinderUtils.bind(obj, "first_name", "hello1", BinderUtils.UNDERSCORES_TO_UPPERCASE);
        Assert.assertEquals(20, obj.getAge());
        Assert.assertEquals("hello1", obj.getFirstName());
    }

    @Test
    public void testBind7() {
        BinderUtils.bind(obj, "age", 20);
        BinderUtils.bind(obj, "first_name", "a");
        Assert.assertEquals(20, obj.getAge());
        Assert.assertEquals("a", obj.getFirstName());
    }

    @Test
    public void testBind8() {
        BinderObj binderObj = new BinderObj();
        BinderUtils.bind(binderObj);
        Assert.assertEquals(20, binderObj.getAge());
        Assert.assertEquals(15, binderObj.getMoney().intValue());
        Assert.assertNull(binderObj.getFirstName());
    }

    @Test
    public void testMergeList() {
        BinderObj targetObj = new BinderObj();
        BinderObj sourceObj = new BinderObj();
        BinderUtils.merge(targetObj, sourceObj);
        Assert.assertNull(targetObj.getFriends());
        Assert.assertNull(sourceObj.getFriends());
        List<String> targetFriends = Lists.newArrayList("obama");
        List<String> sourceFriends = Lists.newArrayList("harry");
        Assert.assertEquals(1, targetFriends.size());
        Assert.assertEquals(1, sourceFriends.size());
        targetObj.setFriends(targetFriends);
        sourceObj.setFriends(null);
        BinderUtils.merge(targetObj, sourceObj);
        Assert.assertEquals(targetObj.getFriends().size(), 1);
        Assert.assertNull(sourceObj.getFriends());
        Assert.assertEquals(targetObj.getFriends().get(0), "obama");
        targetObj.setFriends(null);
        sourceObj.setFriends(sourceFriends);
        BinderUtils.merge(targetObj, sourceObj);
        Assert.assertEquals(targetObj.getFriends().size(), 1);
        Assert.assertEquals(sourceObj.getFriends().size(), 1);
        Assert.assertEquals(targetObj.getFriends().get(0), "harry");
        targetObj.setFriends(targetFriends);
        sourceObj.setFriends(sourceFriends);
        BinderUtils.merge(targetObj, sourceObj);
        Assert.assertEquals(targetObj.getFriends().size(), 2);
        Assert.assertEquals(sourceObj.getFriends().size(), 1);
        Assert.assertEquals(targetObj.getFriends().get(0), "harry");
        Assert.assertEquals(targetObj.getFriends().get(1), "obama");
    }

    @Test
    public void testMergeMap() {
        BinderObj targetObj = new BinderObj();
        BinderObj sourceObj = new BinderObj();
        BinderUtils.merge(targetObj, sourceObj);
        Assert.assertNull(targetObj.getHouses());
        Assert.assertNull(sourceObj.getHouses());
        String tgKey = "tg";
        Object tgValue = new Object();
        Map<String, Object> targetHouses = new HashMap<>();
        targetHouses.put(tgKey, tgValue);
        String srcKey = "src";
        Object srcValue = new Object();
        Map<String, Object> sourceHouses = new HashMap<>();
        sourceHouses.put(srcKey, srcValue);
        targetObj.setHouses(targetHouses);
        sourceObj.setHouses(null);
        BinderUtils.merge(targetObj, sourceObj);
        Assert.assertEquals(targetObj.getHouses().size(), 1);
        Assert.assertNull(sourceObj.getHouses());
        Assert.assertTrue(targetObj.getHouses().containsKey(tgKey));
        Assert.assertEquals(targetObj.getHouses().get(tgKey), tgValue);
        targetObj.setHouses(null);
        sourceObj.setHouses(sourceHouses);
        BinderUtils.merge(targetObj, sourceObj);
        Assert.assertEquals(targetObj.getHouses().size(), 1);
        Assert.assertEquals(sourceObj.getHouses().size(), 1);
        Assert.assertTrue(targetObj.getHouses().containsKey(srcKey));
        Assert.assertEquals(targetObj.getHouses().get(srcKey), srcValue);
        targetObj.setHouses(targetHouses);
        sourceObj.setHouses(sourceHouses);
        BinderUtils.merge(targetObj, sourceObj);
        Assert.assertEquals(targetObj.getHouses().size(), 2);
        Assert.assertEquals(sourceObj.getHouses().size(), 1);
        Assert.assertTrue(targetObj.getHouses().containsKey(tgKey));
        Assert.assertEquals(targetObj.getHouses().get(tgKey), tgValue);
        Assert.assertTrue(targetObj.getHouses().containsKey(srcKey));
        Assert.assertEquals(targetObj.getHouses().get(srcKey), srcValue);
    }

    @Test
    public void testLazyBind() {
        BinderObj binderObj = new BinderObj();
        BinderUtils.bind(binderObj);
        Assert.assertEquals(20, binderObj.getAge());
        Assert.assertEquals(15, binderObj.getMoney().intValue());
        Assert.assertNull(binderObj.getFirstName());
        BinderUtils.lazyBind(binderObj, "first_name", "harry", obj -> ((String) obj).toUpperCase());
        Assert.assertEquals("HARRY", binderObj.getFirstName());
        BinderObj binderObj2 = new BinderObj();
        BinderUtils.bind(binderObj2);
        String nics = null;
        BinderUtils.lazyBind(binderObj2, "first_name", nics, obj -> NetUtils.resolveMultiNicAddr((String) obj));
        Assert.assertEquals(null, binderObj2.getFirstName());
    }

    @Test
    public void testLazyBindSpeed() {
        BinderObj binderObj = new BinderObj();
        BinderUtils.bind(binderObj);
        binderObj.setFirstName("empty ip");
        TimerUtil timerUtil = TimerUtil.newInstance();
        timerUtil.start();
        for (int i = 0; i < 10; i++) {
            BinderUtils.bind(binderObj, "first_name", NetUtils.resolveMultiNicAddr("eth0, lo0, eth1"));
        }
        timerUtil.end();
        final long bindCost = timerUtil.getCost();
        timerUtil.start();
        for (int i = 0; i < 10; i++) {
            BinderUtils.lazyBind(binderObj, "first_name", "eth0, lo0, eth1",
                    obj -> NetUtils.resolveMultiNicAddr((String) obj));
        }
        timerUtil.end();
        final long lazyBindCost = timerUtil.getCost();
        logger.debug("lazyBindCost: {}ms, bindCost: {}ms", lazyBindCost, bindCost);
        Assert.assertTrue(lazyBindCost <= bindCost);
    }

    private static class BinderObj {

        private String firstName;

        @ConfigProperty(value = "20", type = Integer.class)
        private int age;

        @ConfigProperty(value = "15D", type = Double.class)
        private double money;

        @ConfigProperty(needMerged = true)
        private List<String> friends;

        @ConfigProperty(needMerged = true)
        private Map<String, Object> houses;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public Double getMoney() {
            return money;
        }

        public void setMoney(Double money) {
            this.money = money;
        }

        public void setMoney(double money) {
            this.money = money;
        }

        public List<String> getFriends() {
            return friends;
        }

        public void setFriends(List<String> friends) {
            this.friends = friends;
        }

        public Map<String, Object> getHouses() {
            return houses;
        }

        public void setHouses(Map<String, Object> houses) {
            this.houses = houses;
        }
    }
}