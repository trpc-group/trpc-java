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

package com.tencent.trpc.spring.boot.starters;

import com.tencent.trpc.core.common.TRPCRunListener;
import com.tencent.trpc.core.extension.Extension;

@Extension("listener1")
public class TRPCRunListenerImpl1 implements TRPCRunListener {

    @Override
    public void starting() {
        System.out.println("test TRPCRunListener");
    }
}
