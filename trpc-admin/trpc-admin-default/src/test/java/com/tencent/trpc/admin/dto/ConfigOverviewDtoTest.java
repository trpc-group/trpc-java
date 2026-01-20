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

import com.tencent.trpc.admin.ApplicationConfigOverview;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Configuration view test class
 */
public class ConfigOverviewDtoTest {

    private ConfigOverviewDto configOverviewDto;

    @BeforeEach
    public void setUp() {
        this.configOverviewDto = new ConfigOverviewDto();
    }

    @Test
    public void getContent() {
        Assertions.assertNull(configOverviewDto.getContent());
    }

    @Test
    public void setContent() {
        configOverviewDto.setContent(ApplicationConfigOverview.getInstance());
        Assertions.assertEquals(ApplicationConfigOverview.getInstance(), configOverviewDto.getContent());
    }

    @Test
    public void testToString() {
        Assertions.assertEquals(configOverviewDto.toString(),
                "ConfigOverviewDto{content=null} CommonDto{errorcode='0', message=''}");
    }
}
