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

package com.tencent.trpc.admin.dto;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * VersionDtoTest
 */
public class VersionDtoTest {

    private VersionDto versionDto;

    @Before
    public void setUp() {
        this.versionDto = new VersionDto();
    }

    @Test
    public void testGetVersion() {
        Assert.assertNull(versionDto.getVersion());
    }

    @Test
    public void testSetVersion() {
        versionDto.setVersion("a");
        Assert.assertEquals(versionDto.getVersion(), "a");
    }

    @Test
    public void testToString() {
        Assert.assertEquals(versionDto.toString(), "VersionDto{version='null'} CommonDto{errorcode='0', message=''}");

    }
}