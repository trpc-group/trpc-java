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

import com.tencent.trpc.admin.dto.CommonDto;
import com.tencent.trpc.admin.dto.VersionDto;
import com.tencent.trpc.admin.impl.FrameOverviewAdmin;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

public class FrameOverviewAdminTest {

    @Test
    public void test() {
        FrameOverviewAdmin frameOverviewAdmin = new FrameOverviewAdmin();
        VersionDto versionDto = frameOverviewAdmin.getFrameOverview();
        versionDto.toString();
        versionDto.setVersion(versionDto.getVersion());
        Assert.assertTrue(CommonDto.SUCCESS.equals(versionDto.getErrorcode()));
        Assert.assertTrue(StringUtils.isNotEmpty(versionDto.getVersion()));
    }

}
