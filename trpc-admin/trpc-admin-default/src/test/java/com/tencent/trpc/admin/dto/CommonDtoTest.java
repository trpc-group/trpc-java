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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * CommonDto test class
 */
public class CommonDtoTest {

    /**
     * Error code
     */
    private static final String ERROR_CODE = "errorCode";
    /**
     * Error message
     */
    private static final String MESSAGE = "commonDto";


    private CommonDto commonDto;

    @Before
    public void setUp() {
        this.commonDto = new CommonDto();
    }

    @Test
    public void testGetErrorcode() {
        Assert.assertEquals(this.commonDto.getErrorcode(), CommonDto.SUCCESS);
    }

    @Test
    public void testSetErrorcode() {
        this.commonDto.setErrorcode(ERROR_CODE);
        Assert.assertEquals(this.commonDto.getErrorcode(), ERROR_CODE);
    }

    @Test
    public void testGetMessage() {
        Assert.assertEquals(this.commonDto.getMessage(), "");

    }

    @Test
    public void testSetMessage() {
        this.commonDto.setMessage(MESSAGE);
        Assert.assertEquals(this.commonDto.getMessage(), MESSAGE);
    }

    @Test
    public void testToString() {
        Assert.assertEquals(this.commonDto.toString(), "CommonDto{errorcode='0', message=''}");
    }
}