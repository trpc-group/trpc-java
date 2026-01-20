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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * VersionDtoTest
 */
public class VersionDtoTest {

    private VersionDto versionDto;

    @BeforeEach
    public void setUp() {
        this.versionDto = new VersionDto();
    }

    @Test
    public void testGetVersion() {
        Assertions.assertNull(versionDto.getVersion());
    }

    @Test
    public void testSetVersion() {
        versionDto.setVersion("a");
        Assertions.assertEquals(versionDto.getVersion(), "a");
    }

    @Test
    public void testToString() {
        Assertions.assertEquals(versionDto.toString(), "VersionDto{version='null'} CommonDto{errorcode='0', message=''}");

    }
}
