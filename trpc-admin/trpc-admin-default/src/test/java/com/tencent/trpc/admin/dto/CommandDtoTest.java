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

package com.tencent.trpc.admin.dto;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Command dto test
 *
 */
public class CommandDtoTest {

    private CommandDto commandDto;

    @Before
    public void setUp() {
        this.commandDto = new CommandDto(Lists.newArrayList());
    }

    @Test
    public void testGetCmds() {
        Assert.assertTrue(commandDto.getCmds().isEmpty());
    }

    @Test
    public void testSetCmds() {
        commandDto.setCmds(Lists.newArrayList("1", "2"));
        Assert.assertEquals(2, commandDto.getCmds().size());
        Assert.assertEquals(commandDto.getCmds().get(0), "1");
        Assert.assertEquals(commandDto.getCmds().get(1), "2");
    }

    @Test
    public void testToString() {
        Assert.assertEquals(commandDto.toString(), "CommandDto{cmds=[]} CommonDto{errorcode='0', message=''}");
    }
}