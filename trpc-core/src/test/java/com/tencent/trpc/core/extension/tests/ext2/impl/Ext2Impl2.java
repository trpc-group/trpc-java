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

package com.tencent.trpc.core.extension.tests.ext2.impl;

import com.tencent.trpc.core.extension.tests.ext2.Ext2;

public class Ext2Impl2 implements Ext2 {

    @Override
    public String echo() {
        return this.getClass().getName() + " say hello";
    }
}