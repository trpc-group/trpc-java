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

package com.tencent.trpc.admin;

import com.tencent.trpc.admin.dto.CommandDto;
import com.tencent.trpc.admin.dto.CommonDto;
import com.tencent.trpc.admin.impl.CommandAdmin;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

public class CommandAdminTest {

    @Test
    public void test() {
        CommandAdmin commandAdmin = new CommandAdmin();
        CommandDto commandDto = commandAdmin.getCommands();
        commandDto.toString();
        commandDto.setCmds(commandDto.getCmds());
        Assert.assertTrue(CommonDto.SUCCESS.equals(commandDto.getErrorcode()));
        Assert.assertTrue(StringUtils.isEmpty(commandDto.getMessage()));
        Assert.assertTrue(CollectionUtils.isNotEmpty(commandDto.getCmds()));
    }

}
