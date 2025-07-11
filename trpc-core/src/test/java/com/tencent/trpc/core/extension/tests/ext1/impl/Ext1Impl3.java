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

package com.tencent.trpc.core.extension.tests.ext1.impl;

import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.extension.tests.ext1.Ext1;

@Extension("impl3")
public class Ext1Impl3 implements Ext1 {

    @Override
    public String echo() {
        return this.getClass().getName() + " say hello";
    }
}